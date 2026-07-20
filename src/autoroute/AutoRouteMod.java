package autoroute;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.math.geom.Point2;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.IntSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.meta.BlockFlag;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Client-side waypoint router for Mindustry conveyor-style blocks.
 *
 * Workflow:
 * 1. Select a 1x1 conveyor-placement block in the normal build menu.
 * 2. Enable Auto Route from the icon near the top-right HUD controls.
 * 3. Tap two or more waypoint tiles.
 * 4. Confirm to add normal BuildPlan objects to the local player's queue.
 */
public class AutoRouteMod extends Mod{
    private static final int[] dirX = {1, 0, -1, 0};
    private static final int[] dirY = {0, 1, 0, -1};

    private static final float turnPenalty = 0.35f;
    private static final float allowedOrePenalty = 12f;
    private static final int maxExpandedStates = 300_000;
    private static final int maxSearchCells = 450_000;

    private static final String avoidOreSetting = "mindustry-auto-route-avoid-ore";
    private static final String panelSettingPrefix = "mindustry-auto-route-panel-";

    private final Seq<Point2> waypoints = new Seq<>();
    private final Seq<Seq<Point2>> segments = new Seq<>();
    private final Seq<Point2> routeTiles = new Seq<>();
    private final Seq<BuildPlan> routePlans = new Seq<>();
    private final IntSet routeKeys = new IntSet();

    private boolean active;
    private boolean avoidOre = true;
    private Block routeBlock;

    private ImageButton routeButton;
    private TextButton oreButton;
    private Table routePanel;

    private boolean panelPositionReady;
    private float lastSceneWidth = -1f;
    private float lastSceneHeight = -1f;

    @Override
    public void init(){
        avoidOre = Core.settings.getBool(avoidOreSetting, true);

        buildHud();

        Events.on(EventType.TapEvent.class, event -> {
            if(!active || event.tile == null || event.player != Vars.player || Vars.state.isMenu()) return;
            addWaypoint(event.tile);
        });

        Events.on(EventType.ResetEvent.class, event -> resetForWorldChange());
        Events.on(EventType.WorldLoadEvent.class, event -> resetForWorldChange());
        Events.run(EventType.Trigger.drawOver, this::drawPreview);
    }

    private void buildHud(){
        // Match AutoDrill's top-right icon alignment, but place this icon one row
        // lower so both mods remain visible together on narrow portrait screens.
        Vars.ui.hudGroup.fill(table -> {
            TextureRegionDrawable icon = new TextureRegionDrawable(
                Core.atlas.find("mindustry-auto-route-icon")
            );

            routeButton = table.button(icon, Styles.emptyTogglei, this::toggleRouting)
                .size(44f)
                .get();

            routeButton.resizeImage(30f);
            routeButton.update(() -> routeButton.setChecked(active));
            routeButton.visible(() -> !Vars.state.isMenu());

            table.margin(5f);
            table.marginRight(155f);
            table.marginTop(47f);
            table.top().right();
        });

        buildMovableRoutePanel();
    }

    private void buildMovableRoutePanel(){
        routePanel = new Table(Styles.black6);
        routePanel.margin(5f);

        TextureRegionDrawable moveIcon = new TextureRegionDrawable(
            Core.atlas.find("mindustry-auto-route-move")
        );

        ImageButton moveButton = routePanel.button(moveIcon, Styles.defaulti, () -> {})
            .size(42f)
            .get();
        moveButton.resizeImage(23f);
        addPanelDragListener(moveButton);

        routePanel.label(this::statusText)
            .width(Vars.mobile ? 138f : 190f)
            .left()
            .padLeft(5f)
            .padRight(4f);

        routePanel.button("X", Styles.cleart, () -> stopRouting(true))
            .size(42f);

        routePanel.row();

        routePanel.button("Undo", Styles.cleart, this::undoWaypoint)
            .colspan(1)
            .size(82f, 44f);

        routePanel.button("Clear", Styles.cleart, this::clearRoute)
            .colspan(1)
            .size(82f, 44f);

        routePanel.button("Build", Styles.defaultt, this::commitRoute)
            .colspan(1)
            .size(82f, 44f);

        routePanel.row();

        oreButton = routePanel.button("", Styles.clearTogglet, this::toggleOreAvoidance)
            .colspan(3)
            .growX()
            .height(42f)
            .padTop(2f)
            .get();

        oreButton.update(() -> {
            oreButton.setChecked(avoidOre);
            oreButton.setText(avoidOre ? "Ore: avoid" : "Ore: allow");
        });

        routePanel.pack();
        routePanel.visible(() -> active && !Vars.state.isMenu());
        routePanel.update(this::updatePanelPlacement);
        Vars.ui.hudGroup.addChild(routePanel);
    }

    private void addPanelDragListener(ImageButton moveButton){
        moveButton.addListener(new InputListener(){
            private float grabX;
            private float grabY;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                grabX = x;
                grabY = y;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                routePanel.moveBy(x - grabX, y - grabY);
                clampPanelToScreen();
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                savePanelPosition();
            }
        });
    }

    private void updatePanelPlacement(){
        if(routePanel == null) return;

        float sceneWidth = Core.scene.getWidth();
        float sceneHeight = Core.scene.getHeight();
        if(sceneWidth <= 0f || sceneHeight <= 0f) return;

        boolean resized = sceneWidth != lastSceneWidth || sceneHeight != lastSceneHeight;
        if(!panelPositionReady || resized){
            restorePanelPosition(sceneWidth, sceneHeight);
            panelPositionReady = true;
            lastSceneWidth = sceneWidth;
            lastSceneHeight = sceneHeight;
        }else{
            clampPanelToScreen();
        }
    }

    private void restorePanelPosition(float sceneWidth, float sceneHeight){
        routePanel.pack();

        boolean portrait = sceneHeight > sceneWidth;
        float defaultCenterX;
        float defaultCenterY;

        if(portrait){
            // Compact left-side default on phones. It stays clear of the build
            // menu and can immediately be dragged elsewhere with the move icon.
            defaultCenterX = (routePanel.getWidth() / 2f + 8f) / sceneWidth;
            defaultCenterY = 0.56f;
        }else{
            defaultCenterX = 0.5f;
            float bottomSpace = Vars.mobile ? 112f : 18f;
            defaultCenterY = (bottomSpace + routePanel.getHeight() / 2f) / sceneHeight;
        }

        String orientation = portrait ? "portrait-" : "landscape-";
        float centerX = Core.settings.getFloat(panelSettingPrefix + orientation + "x", defaultCenterX);
        float centerY = Core.settings.getFloat(panelSettingPrefix + orientation + "y", defaultCenterY);

        routePanel.setPosition(
            centerX * sceneWidth - routePanel.getWidth() / 2f,
            centerY * sceneHeight - routePanel.getHeight() / 2f
        );
        clampPanelToScreen();
    }

    private void savePanelPosition(){
        if(routePanel == null || Core.scene.getWidth() <= 0f || Core.scene.getHeight() <= 0f) return;

        boolean portrait = Core.scene.getHeight() > Core.scene.getWidth();
        String orientation = portrait ? "portrait-" : "landscape-";

        float centerX = (routePanel.x + routePanel.getWidth() / 2f) / Core.scene.getWidth();
        float centerY = (routePanel.y + routePanel.getHeight() / 2f) / Core.scene.getHeight();

        Core.settings.put(panelSettingPrefix + orientation + "x", centerX);
        Core.settings.put(panelSettingPrefix + orientation + "y", centerY);
    }

    private void clampPanelToScreen(){
        if(routePanel == null) return;

        float maxX = Math.max(0f, Core.scene.getWidth() - routePanel.getWidth());
        float maxY = Math.max(0f, Core.scene.getHeight() - routePanel.getHeight());

        routePanel.setPosition(
            Math.max(0f, Math.min(routePanel.x, maxX)),
            Math.max(0f, Math.min(routePanel.y, maxY))
        );
    }

    private String statusText(){
        if(routeBlock == null) return "[accent]Auto Route[]";
        if(waypoints.isEmpty()) return "[accent]" + routeBlock.localizedName + "[]\nTap Point A";

        return "[accent]" + routeBlock.localizedName + "[]\n" +
            waypoints.size + " point" + (waypoints.size == 1 ? "" : "s") +
            " • " + routePlans.size + " block" + (routePlans.size == 1 ? "" : "s");
    }

    private void toggleRouting(){
        if(active){
            stopRouting(true);
        }else{
            startRouting();
        }
    }

    private void startRouting(){
        if(Vars.state.isMenu() || Vars.control == null || Vars.control.input == null) return;

        Block selected = Vars.control.input.block;
        if(selected == null || !selected.conveyorPlacement || selected.size != 1){
            Vars.ui.showInfoToast("Select a 1x1 conveyor or duct first, then tap the Auto Route icon.", 4f);
            return;
        }

        routeBlock = selected;
        clearRoute();
        active = true;
        routePanel.toFront();

        // Prevent the normal drag-placement tool from also creating plans while
        // waypoint mode is receiving map taps.
        Vars.control.input.block = null;
        Vars.ui.showInfoToast("Tap Point A, then Point B. Add more taps for extra waypoints.", 4f);
    }

    private void stopRouting(boolean restoreBlock){
        active = false;
        clearRoute();

        if(restoreBlock && routeBlock != null && Vars.control != null && Vars.control.input != null){
            Vars.control.input.block = routeBlock;
        }
    }

    private void resetForWorldChange(){
        active = false;
        clearRoute();
        routeBlock = null;
    }

    private void toggleOreAvoidance(){
        boolean previous = avoidOre;
        avoidOre = !avoidOre;

        // Existing segments may use ore when this option changes, so calculate
        // them again from the saved waypoints. If the new rule makes the route
        // impossible, restore the previous setting and route.
        if(waypoints.size > 1 && !recalculateAllSegments()){
            avoidOre = previous;
            recalculateAllSegments();
            Vars.ui.showInfoToast("That ore setting leaves no valid path, so it was restored.", 4f);
        }

        Core.settings.put(avoidOreSetting, avoidOre);
    }

    private void addWaypoint(Tile tile){
        if(routeBlock == null) return;

        Point2 point = new Point2(tile.x, tile.y);

        if(!waypoints.isEmpty()){
            Point2 previous = waypoints.peek();
            if(previous.x == point.x && previous.y == point.y){
                Vars.ui.showInfoToast("That tile is already the latest waypoint.", 2f);
                return;
            }
        }

        if(!isEndpointUsable(point.x, point.y)){
            Vars.ui.showInfoToast("A " + routeBlock.localizedName + " cannot be placed on that tile.", 3f);
            return;
        }

        if(waypoints.isEmpty()){
            waypoints.add(point);
            rebuildRouteAndPlans();
            return;
        }

        Point2 start = waypoints.peek();
        Seq<Point2> path = findPath(start, point);
        if(path == null){
            String suffix = avoidOre ? " Try another waypoint or tap 'Ore: avoid' to allow ore." : " Try another waypoint.";
            Vars.ui.showInfoToast("No safe route was found. The router also avoids tiles beside drills." + suffix, 5f);
            return;
        }

        segments.add(path);
        waypoints.add(point);
        rebuildRouteAndPlans();
    }

    private void undoWaypoint(){
        if(waypoints.isEmpty()) return;

        if(waypoints.size == 1){
            clearRoute();
            return;
        }

        waypoints.remove(waypoints.size - 1);
        segments.remove(segments.size - 1);
        rebuildRouteAndPlans();
    }

    private void clearRoute(){
        waypoints.clear();
        segments.clear();
        routeTiles.clear();
        routePlans.clear();
        routeKeys.clear();
    }

    private boolean recalculateAllSegments(){
        Seq<Seq<Point2>> replacement = new Seq<>();

        // Temporarily rebuild route occupancy one segment at a time so later
        // segments cannot cross earlier preview segments.
        routeTiles.clear();
        routeKeys.clear();
        if(!waypoints.isEmpty()) addRouteTile(waypoints.first());

        for(int i = 1; i < waypoints.size; i++){
            Seq<Point2> path = findPath(waypoints.get(i - 1), waypoints.get(i));
            if(path == null){
                rebuildRouteAndPlans();
                return false;
            }

            replacement.add(path);
            for(int j = 1; j < path.size; j++) addRouteTile(path.get(j));
        }

        segments.clear();
        segments.addAll(replacement);
        rebuildRouteAndPlans();
        return true;
    }

    private void rebuildRouteAndPlans(){
        routeTiles.clear();
        routePlans.clear();
        routeKeys.clear();

        if(waypoints.isEmpty() || routeBlock == null) return;

        addRouteTile(waypoints.first());
        for(Seq<Point2> segment : segments){
            for(int i = 1; i < segment.size; i++) addRouteTile(segment.get(i));
        }

        for(int i = 0; i < routeTiles.size; i++){
            Point2 current = routeTiles.get(i);
            int rotation;

            if(i < routeTiles.size - 1){
                rotation = direction(current, routeTiles.get(i + 1));
            }else if(i > 0){
                rotation = direction(routeTiles.get(i - 1), current);
            }else{
                rotation = 0;
            }

            routePlans.add(new BuildPlan(current.x, current.y, rotation, routeBlock));
        }
    }

    private void addRouteTile(Point2 point){
        Point2 copy = new Point2(point.x, point.y);
        routeTiles.add(copy);
        routeKeys.add(tileKey(copy.x, copy.y));
    }

    private int direction(Point2 from, Point2 to){
        int dx = to.x - from.x;
        int dy = to.y - from.y;

        if(dx > 0) return 0;
        if(dy > 0) return 1;
        if(dx < 0) return 2;
        return 3;
    }

    private void commitRoute(){
        if(routePlans.isEmpty() || routeBlock == null) return;
        if(Vars.player == null || Vars.player.unit() == null){
            Vars.ui.showInfoToast("No player unit is available to build the route.", 3f);
            return;
        }

        int queued = 0;
        for(BuildPlan plan : routePlans){
            if(isPlanUsable(plan)){
                Vars.player.unit().addBuild(plan.copy());
                queued++;
            }
        }

        if(queued == 0){
            Vars.ui.showInfoToast("The route is no longer placeable.", 3f);
            return;
        }

        Vars.ui.showInfoToast("Queued " + queued + " " + routeBlock.localizedName +
            (queued == 1 ? "." : " blocks."), 3f);
        clearRoute();
    }

    private boolean isEndpointUsable(int x, int y){
        Tile tile = Vars.world.tile(x, y);
        if(tile == null) return false;

        if(tile.block() == routeBlock && tile.team() == Vars.player.team()) return true;

        for(int rotation = 0; rotation < 4; rotation++){
            if(Build.validPlace(routeBlock, Vars.player.team(), x, y, rotation)) return true;
        }
        return false;
    }

    private boolean isPlanUsable(BuildPlan plan){
        Tile tile = Vars.world.tile(plan.x, plan.y);
        if(tile == null) return false;

        return Build.validPlace(plan.block, Vars.player.team(), plan.x, plan.y, plan.rotation) ||
            (tile.block() == plan.block && tile.team() == Vars.player.team());
    }

    private void drawPreview(){
        if(!active || routePlans.isEmpty() || routeBlock == null || Vars.state.isMenu()) return;

        for(BuildPlan plan : routePlans){
            plan.animScale = 1f;
            plan.block.drawPlan(plan, routePlans, isPlanUsable(plan), 1f);
        }
    }

    private Seq<Point2> findPath(Point2 start, Point2 goal){
        int worldWidth = Vars.world.width();
        int worldHeight = Vars.world.height();
        int[] margins = {8, 16, 32, 64, 96};

        int previousMinX = -1;
        int previousMinY = -1;
        int previousMaxX = -1;
        int previousMaxY = -1;

        for(int margin : margins){
            int minX = Math.max(0, Math.min(start.x, goal.x) - margin);
            int minY = Math.max(0, Math.min(start.y, goal.y) - margin);
            int maxX = Math.min(worldWidth - 1, Math.max(start.x, goal.x) + margin);
            int maxY = Math.min(worldHeight - 1, Math.max(start.y, goal.y) + margin);

            if(minX == previousMinX && minY == previousMinY && maxX == previousMaxX && maxY == previousMaxY){
                continue;
            }

            previousMinX = minX;
            previousMinY = minY;
            previousMaxX = maxX;
            previousMaxY = maxY;

            Seq<Point2> result = searchRegion(start, goal, minX, minY, maxX, maxY);
            if(result != null) return result;

            if(minX == 0 && minY == 0 && maxX == worldWidth - 1 && maxY == worldHeight - 1){
                return null;
            }
        }

        return searchRegion(start, goal, 0, 0, worldWidth - 1, worldHeight - 1);
    }

    private Seq<Point2> searchRegion(Point2 start, Point2 goal, int minX, int minY, int maxX, int maxY){
        int regionWidth = maxX - minX + 1;
        int regionHeight = maxY - minY + 1;
        int cellCount = regionWidth * regionHeight;

        if(cellCount <= 0 || cellCount > maxSearchCells) return null;

        int stateCount = cellCount * 5;
        float[] bestCost = new float[stateCount];
        int[] parent = new int[stateCount];
        boolean[] closed = new boolean[stateCount];
        Arrays.fill(bestCost, Float.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);

        int startState = stateIndex(start.x, start.y, 4, minX, minY, regionWidth);
        bestCost[startState] = 0f;

        PriorityQueue<SearchNode> open = new PriorityQueue<>();
        open.add(new SearchNode(startState, 0f, heuristic(start.x, start.y, goal.x, goal.y)));

        int goalState = -1;
        int expanded = 0;

        while(!open.isEmpty() && expanded < maxExpandedStates){
            SearchNode node = open.poll();
            if(node.cost > bestCost[node.state] + 0.0001f || closed[node.state]) continue;

            closed[node.state] = true;
            expanded++;

            int cell = node.state / 5;
            int incomingDirection = node.state % 5;
            int localX = cell % regionWidth;
            int localY = cell / regionWidth;
            int x = minX + localX;
            int y = minY + localY;

            if(x == goal.x && y == goal.y){
                goalState = node.state;
                break;
            }

            for(int nextDirection = 0; nextDirection < 4; nextDirection++){
                int nextX = x + dirX[nextDirection];
                int nextY = y + dirY[nextDirection];

                if(nextX < minX || nextX > maxX || nextY < minY || nextY > maxY) continue;
                if(!isTraversable(nextX, nextY, nextDirection, start, goal)) continue;

                int nextState = stateIndex(nextX, nextY, nextDirection, minX, minY, regionWidth);
                if(closed[nextState]) continue;

                float step = 1f;
                if(incomingDirection != 4 && incomingDirection != nextDirection) step += turnPenalty;

                Tile nextTile = Vars.world.tile(nextX, nextY);
                if(!avoidOre && nextTile != null && nextTile.drop() != null &&
                    !(nextX == goal.x && nextY == goal.y)){
                    step += allowedOrePenalty;
                }

                float newCost = bestCost[node.state] + step;
                if(newCost + 0.0001f < bestCost[nextState]){
                    bestCost[nextState] = newCost;
                    parent[nextState] = node.state;
                    float estimate = newCost + heuristic(nextX, nextY, goal.x, goal.y);
                    open.add(new SearchNode(nextState, newCost, estimate));
                }
            }
        }

        if(goalState == -1) return null;

        Seq<Point2> reversed = new Seq<>();
        int current = goalState;

        while(current != -1){
            int cell = current / 5;
            int localX = cell % regionWidth;
            int localY = cell / regionWidth;
            reversed.add(new Point2(minX + localX, minY + localY));

            if(current == startState) break;
            current = parent[current];
        }

        if(reversed.isEmpty()) return null;
        Point2 last = reversed.peek();
        if(last.x != start.x || last.y != start.y) return null;

        Seq<Point2> result = new Seq<>();
        for(int i = reversed.size - 1; i >= 0; i--){
            result.add(reversed.get(i));
        }
        return result;
    }

    private boolean isTraversable(int x, int y, int rotation, Point2 start, Point2 goal){
        Tile tile = Vars.world.tile(x, y);
        if(tile == null) return false;

        boolean isStart = x == start.x && y == start.y;
        boolean isGoal = x == goal.x && y == goal.y;

        // Do not let a newly calculated segment cross an earlier preview
        // segment. Its first tile is the one intentional exception.
        if(routeKeys.contains(tileKey(x, y)) && !isStart && !isGoal) return false;

        // Waypoints are allowed on ore; only automatically chosen intermediate
        // tiles are forbidden when ore avoidance is enabled.
        if(avoidOre && !isGoal && tile.drop() != null) return false;

        // Drills automatically dump mined items into adjacent conveyors. An
        // intermediate route tile beside a drill can therefore contaminate an
        // unrelated line. Explicit start/end waypoints remain allowed so the
        // player can intentionally connect a route to a drill.
        if(!isStart && !isGoal && isBesideDrill(x, y)) return false;

        if(isGoal && tile.block() == routeBlock && tile.team() == Vars.player.team()) return true;

        return Build.validPlace(routeBlock, Vars.player.team(), x, y, rotation);
    }

    private boolean isBesideDrill(int x, int y){
        for(int direction = 0; direction < 4; direction++){
            Tile nearby = Vars.world.tile(x + dirX[direction], y + dirY[direction]);
            if(nearby == null) continue;

            Block block = nearby.block();
            // BlockFlag.drill also catches drills added by other mods, not only
            // Mindustry's built-in Mechanical/Pneumatic/Laser/Blast drills.
            if(block.flags.contains(BlockFlag.drill)) return true;
        }
        return false;
    }

    private int stateIndex(int x, int y, int direction, int minX, int minY, int regionWidth){
        int localCell = (x - minX) + (y - minY) * regionWidth;
        return localCell * 5 + direction;
    }

    private float heuristic(int x, int y, int goalX, int goalY){
        return Math.abs(goalX - x) + Math.abs(goalY - y);
    }

    private int tileKey(int x, int y){
        return x + y * Vars.world.width();
    }

    private static final class SearchNode implements Comparable<SearchNode>{
        final int state;
        final float cost;
        final float estimate;

        SearchNode(int state, float cost, float estimate){
            this.state = state;
            this.cost = cost;
            this.estimate = estimate;
        }

        @Override
        public int compareTo(SearchNode other){
            int estimateOrder = Float.compare(estimate, other.estimate);
            return estimateOrder != 0 ? estimateOrder : Float.compare(cost, other.cost);
        }
    }
}
