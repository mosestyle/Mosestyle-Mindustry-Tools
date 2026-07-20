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
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.mod.Mod;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.DirectionBridge;
import mindustry.world.blocks.distribution.Duct;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.meta.BlockFlag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Client-side waypoint router for Mindustry conveyor-style blocks.
 *
 * Features:
 * - smart A* routing with route preferences;
 * - automatic ore fallback;
 * - junction and bridge crossings;
 * - player-marked forbidden tiles;
 * - awareness of existing local build plans;
 * - intentional connections to existing conveyor/duct endpoints;
 * - mobile-oriented search limits and cached path results.
 */
public class AutoRouteMod extends Mod{
    private static final int[] dirX = {1, 0, -1, 0};
    private static final int[] dirY = {0, 1, 0, -1};

    // Matches AutoDrill's top-right HUD placement; this icon is one row below it.
    private static final int hudIconSize = 30;
    private static final float hudRightMargin = 155f;
    private static final float hudSecondRowTopMargin = 47f;

    private static final float allowedOrePenalty = 12f;
    private static final int desktopMaxExpandedStates = 320_000;
    private static final int mobileMaxExpandedStates = 170_000;
    private static final int desktopMaxSearchCells = 450_000;
    private static final int mobileMaxSearchCells = 180_000;
    private static final int pathCacheLimit = 32;

    private static final String oreModeSetting = "mindustry-auto-route-ore-mode";
    private static final String legacyAvoidOreSetting = "mindustry-auto-route-avoid-ore";
    private static final String preferenceSetting = "mindustry-auto-route-preference";
    private static final String bridgesSetting = "mindustry-auto-route-bridges";
    private static final String panelSettingPrefix = "mindustry-auto-route-panel-";

    private final Seq<Point2> waypoints = new Seq<>();
    private final Seq<PathResult> segments = new Seq<>();
    private final Seq<Point2> routeTiles = new Seq<>();
    private final Seq<BridgeLink> routeBridges = new Seq<>();
    private final Seq<BuildPlan> routePlans = new Seq<>();
    private final IntSet routeKeys = new IntSet();
    private final IntSet waypointKeys = new IntSet();
    private final IntSet connectionWaypointKeys = new IntSet();

    private final Seq<Point2> forbiddenTiles = new Seq<>();
    private final IntSet forbiddenKeys = new IntSet();

    private final HashMap<Integer, BuildPlan> queuedPlansByKey = new HashMap<>();
    private final HashMap<String, PathResult> pathCache = new HashMap<>();

    private boolean active;
    private boolean forbidMode;
    private boolean optionsExpanded;
    private boolean bridgesEnabled = true;
    private boolean searchTimedOut;
    private boolean searchLimitHit;
    private Block routeBlock;
    private OreMode oreMode = OreMode.auto;
    private RoutePreference preference = RoutePreference.clean;

    private int smartCrossings;
    private int smartBridges;
    private int oreFallbackSegments;
    private int connectionEndpoints;
    private int forbiddenRevision;
    private int queuedPlanFingerprint;
    private int previewPlanFingerprint;

    private ImageButton routeButton;
    private TextButton oreButton;
    private TextButton preferenceButton;
    private TextButton forbiddenButton;
    private TextButton bridgeButton;
    private TextButton optionsButton;
    private Table optionsTable;
    private Table routePanel;

    private boolean panelPositionReady;
    private float lastSceneWidth = -1f;
    private float lastSceneHeight = -1f;

    @Override
    public void init(){
        loadSettings();
        buildHud();

        Events.on(EventType.TapEvent.class, event -> {
            if(!active || event.tile == null || event.player != Vars.player || Vars.state.isMenu()) return;

            if(forbidMode){
                toggleForbiddenTile(event.tile);
            }else{
                addWaypoint(event.tile);
            }
        });

        Events.on(EventType.ResetEvent.class, event -> resetForWorldChange());
        Events.on(EventType.WorldLoadEvent.class, event -> resetForWorldChange());
        Events.run(EventType.Trigger.drawOver, this::drawPreview);
    }

    private void loadSettings(){
        if(Core.settings.has(oreModeSetting)){
            oreMode = OreMode.fromOrdinal(Core.settings.getInt(oreModeSetting, 0));
        }else{
            // Preserve the v0.3.x setting when upgrading.
            boolean oldAvoid = Core.settings.getBool(legacyAvoidOreSetting, true);
            oreMode = oldAvoid ? OreMode.auto : OreMode.allow;
        }

        preference = RoutePreference.fromOrdinal(Core.settings.getInt(
            preferenceSetting,
            RoutePreference.clean.ordinal()
        ));
        bridgesEnabled = Core.settings.getBool(bridgesSetting, true);
    }

    private void buildHud(){
        Vars.ui.hudGroup.fill(table -> {
            TextureRegionDrawable icon = new TextureRegionDrawable(
                Core.atlas.find("mindustry-auto-route-icon")
            );

            routeButton = table.button(icon, Styles.emptyTogglei, this::toggleRouting).get();
            routeButton.resizeImage(hudIconSize);
            routeButton.update(() -> routeButton.setChecked(active));
            routeButton.visible(() -> !Vars.state.isMenu());

            table.margin(5f);
            table.marginRight(hudRightMargin);
            table.marginTop(hudSecondRowTopMargin);
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
            .width(Vars.mobile ? 138f : 205f)
            .left()
            .padLeft(5f)
            .padRight(4f);

        routePanel.button("X", Styles.cleart, () -> stopRouting(true))
            .size(42f);

        routePanel.row();

        routePanel.button("Undo", Styles.cleart, this::undoWaypoint)
            .size(82f, 44f);

        routePanel.button("Clear", Styles.cleart, this::clearRoute)
            .size(82f, 44f);

        routePanel.button("Build", Styles.defaultt, this::commitRoute)
            .size(82f, 44f);

        routePanel.row();

        optionsButton = routePanel.button("Options +", Styles.cleart, this::toggleOptions)
            .colspan(3)
            .growX()
            .height(40f)
            .padTop(2f)
            .get();

        routePanel.row();
        optionsTable = new Table();
        routePanel.add(optionsTable).colspan(3).growX();
        rebuildOptionsTable();

        routePanel.pack();
        routePanel.visible(() -> active && !Vars.state.isMenu());
        routePanel.update(this::updatePanelPlacement);
        Vars.ui.hudGroup.addChild(routePanel);
    }

    private void toggleOptions(){
        optionsExpanded = !optionsExpanded;
        rebuildOptionsTable();
    }

    private void rebuildOptionsTable(){
        if(optionsTable == null) return;

        optionsTable.clearChildren();
        optionsButton.setText(optionsExpanded ? "Options -" : optionsSummary());

        if(optionsExpanded){
            oreButton = optionsTable.button("", Styles.cleart, this::cycleOreMode)
                .growX()
                .height(40f)
                .get();
            oreButton.update(() -> oreButton.setText(oreMode.label));

            optionsTable.row();

            preferenceButton = optionsTable.button("", Styles.cleart, this::cyclePreference)
                .growX()
                .height(40f)
                .get();
            preferenceButton.update(() -> preferenceButton.setText(preference.label));

            optionsTable.row();

            bridgeButton = optionsTable.button("", Styles.clearTogglet, this::toggleBridges)
                .growX()
                .height(40f)
                .get();
            bridgeButton.update(() -> {
                bridgeButton.setChecked(bridgesEnabled);
                bridgeButton.setText(bridgesEnabled ? "Bridges: automatic" : "Bridges: off");
            });

            optionsTable.row();

            Table forbiddenRow = new Table();
            forbiddenButton = forbiddenRow.button("", Styles.clearTogglet, this::toggleForbidMode)
                .width(164f)
                .height(40f)
                .get();
            forbiddenButton.update(() -> {
                forbiddenButton.setChecked(forbidMode);
                forbiddenButton.setText(forbidMode ?
                    "Forbidden: marking (" + forbiddenTiles.size + ")" :
                    "Forbidden tiles: " + forbiddenTiles.size
                );
            });

            forbiddenRow.button("Reset", Styles.cleart, this::clearForbiddenTiles)
                .width(82f)
                .height(40f);

            optionsTable.add(forbiddenRow).growX();
        }

        routePanel.pack();
        clampPanelToScreen();
    }

    private String optionsSummary(){
        String bridge = bridgesEnabled ? "B" : "-";
        return "Options: " + oreMode.shortLabel + " / " + preference.shortLabel + " / " + bridge + " +";
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
        if(forbidMode){
            return "[scarlet]Mark forbidden tiles[]\nTap tiles to toggle";
        }
        if(waypoints.isEmpty()) return "[accent]" + routeBlock.localizedName + "[]\nTap Point A";

        StringBuilder details = new StringBuilder();
        if(smartCrossings > 0) details.append(" • ").append(smartCrossings).append("J");
        if(smartBridges > 0) details.append(" • ").append(smartBridges).append("B");
        if(oreFallbackSegments > 0) details.append(" • ore fallback");
        if(connectionEndpoints > 0) details.append(" • ").append(connectionEndpoints).append(connectionEndpoints == 1 ? " link" : " links");

        return "[accent]" + routeBlock.localizedName + "[]\n" +
            waypoints.size + " point" + (waypoints.size == 1 ? "" : "s") +
            " • " + routePlans.size + " plan" + (routePlans.size == 1 ? "" : "s") + details;
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
        forbidMode = false;
        refreshQueuedPlanSnapshot();
        routePanel.toFront();

        Vars.control.input.block = null;
        Vars.ui.showInfoToast("Tap Point A, then Point B. Add more taps for extra waypoints.", 4f);
    }

    private void stopRouting(boolean restoreBlock){
        active = false;
        forbidMode = false;
        clearRoute();

        if(restoreBlock && routeBlock != null && Vars.control != null && Vars.control.input != null){
            Vars.control.input.block = routeBlock;
        }
    }

    private void resetForWorldChange(){
        active = false;
        forbidMode = false;
        clearRoute();
        clearForbiddenTilesInternal(false);
        routeBlock = null;
        queuedPlansByKey.clear();
        pathCache.clear();
    }

    private void cycleOreMode(){
        OreMode previous = oreMode;
        oreMode = oreMode.next();
        Core.settings.put(oreModeSetting, oreMode.ordinal());
        pathCache.clear();

        if(waypoints.size > 1 && !recalculateAllSegments(true)){
            oreMode = previous;
            Core.settings.put(oreModeSetting, oreMode.ordinal());
            recalculateAllSegments(true);
            Vars.ui.showInfoToast("That ore mode leaves no valid route, so it was restored.", 4f);
        }
        rebuildOptionsTable();
    }

    private void cyclePreference(){
        RoutePreference previous = preference;
        preference = preference.next();
        Core.settings.put(preferenceSetting, preference.ordinal());
        pathCache.clear();

        if(waypoints.size > 1 && !recalculateAllSegments(true)){
            preference = previous;
            Core.settings.put(preferenceSetting, preference.ordinal());
            recalculateAllSegments(true);
            Vars.ui.showInfoToast("That route preference could not keep the current path.", 4f);
        }
        rebuildOptionsTable();
    }

    private void toggleBridges(){
        boolean previous = bridgesEnabled;
        bridgesEnabled = !bridgesEnabled;
        Core.settings.put(bridgesSetting, bridgesEnabled);
        pathCache.clear();

        if(waypoints.size > 1 && !recalculateAllSegments(true)){
            bridgesEnabled = previous;
            Core.settings.put(bridgesSetting, bridgesEnabled);
            recalculateAllSegments(true);
            Vars.ui.showInfoToast("The current route needs automatic bridges, so the setting was restored.", 4f);
        }
        rebuildOptionsTable();
    }

    private void toggleForbidMode(){
        forbidMode = !forbidMode;
    }

    private void toggleForbiddenTile(Tile tile){
        int key = tileKey(tile.x, tile.y);
        boolean removed = forbiddenKeys.remove(key);
        Point2 removedPoint = null;

        if(removed){
            for(int i = forbiddenTiles.size - 1; i >= 0; i--){
                Point2 point = forbiddenTiles.get(i);
                if(point.x == tile.x && point.y == tile.y){
                    removedPoint = forbiddenTiles.remove(i);
                    break;
                }
            }
        }else{
            forbiddenKeys.add(key);
            forbiddenTiles.add(new Point2(tile.x, tile.y));
        }

        forbiddenRevision++;
        pathCache.clear();

        if(waypoints.size > 1 && !recalculateAllSegments(true)){
            // Revert the mark if it destroys the current route entirely.
            if(removed){
                forbiddenKeys.add(key);
                forbiddenTiles.add(removedPoint == null ? new Point2(tile.x, tile.y) : removedPoint);
            }else{
                forbiddenKeys.remove(key);
                for(int i = forbiddenTiles.size - 1; i >= 0; i--){
                    Point2 point = forbiddenTiles.get(i);
                    if(point.x == tile.x && point.y == tile.y){
                        forbiddenTiles.remove(i);
                        break;
                    }
                }
            }
            forbiddenRevision++;
            pathCache.clear();
            recalculateAllSegments(true);
            Vars.ui.showInfoToast("That forbidden tile leaves no valid route, so the change was undone.", 4f);
        }
    }

    private void clearForbiddenTiles(){
        if(forbiddenTiles.isEmpty()) return;
        clearForbiddenTilesInternal(true);
    }

    private void clearForbiddenTilesInternal(boolean recalculate){
        forbiddenTiles.clear();
        forbiddenKeys.clear();
        forbiddenRevision++;
        pathCache.clear();
        if(recalculate && waypoints.size > 1){
            recalculateAllSegments(true);
        }
    }

    private void addWaypoint(Tile tile){
        if(routeBlock == null) return;

        refreshQueuedPlanSnapshot();
        Point2 point = new Point2(tile.x, tile.y);

        if(!waypoints.isEmpty()){
            Point2 previous = waypoints.peek();
            if(previous.x == point.x && previous.y == point.y){
                Vars.ui.showInfoToast("That tile is already the latest waypoint.", 2f);
                return;
            }
            if(routeKeys.contains(tileKey(point.x, point.y))){
                Vars.ui.showInfoToast("That tile is already part of the current route.", 3f);
                return;
            }
        }

        if(!isEndpointUsable(point.x, point.y)){
            Vars.ui.showInfoToast("That tile cannot be used as a route point.", 3f);
            return;
        }

        if(waypoints.isEmpty()){
            waypoints.add(point);
            rebuildRouteAndPlans();
            return;
        }

        Point2 start = waypoints.peek();
        int distance = manhattan(start.x, start.y, point.x, point.y);
        if(distance > maxWaypointDistance()){
            Vars.ui.showInfoToast(
                "That segment is very long (" + distance + " tiles). Add an intermediate waypoint to protect mobile performance.",
                5f
            );
            return;
        }

        PathResult path = findPath(start, point);
        if(path == null){
            showPathFailureToast();
            return;
        }

        if(!validateIntentionalConnections(path, start, point)) return;

        segments.add(path);
        waypoints.add(point);
        rebuildRouteAndPlans();
    }

    private void showPathFailureToast(){
        if(searchTimedOut){
            Vars.ui.showInfoToast("Route calculation took too long. Add a closer waypoint or use a less restrictive option.", 5f);
        }else if(searchLimitHit){
            Vars.ui.showInfoToast("Route search reached its safety limit. Add an intermediate waypoint.", 5f);
        }else{
            Vars.ui.showInfoToast(
                "No safe route was found. Try another waypoint, enable bridges, or change the ore/preference options.",
                5f
            );
        }
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
        routeBridges.clear();
        routePlans.clear();
        routeKeys.clear();
        waypointKeys.clear();
        connectionWaypointKeys.clear();
        smartCrossings = 0;
        smartBridges = 0;
        oreFallbackSegments = 0;
        connectionEndpoints = 0;
        previewPlanFingerprint = queuedPlanFingerprint;
        pathCache.clear();
    }

    private boolean recalculateAllSegments(boolean refreshPlans){
        if(refreshPlans) refreshQueuedPlanSnapshot();

        // Recalculation is also used after the world or local build queue has
        // changed. Do not reuse a path cached against old tile occupancy.
        pathCache.clear();

        Seq<PathResult> replacement = new Seq<>();
        routeTiles.clear();
        routeKeys.clear();
        if(!waypoints.isEmpty()) addRouteTile(waypoints.first());

        for(int i = 1; i < waypoints.size; i++){
            Point2 start = waypoints.get(i - 1);
            Point2 goal = waypoints.get(i);
            PathResult path = findPath(start, goal);
            if(path == null || !validateIntentionalConnections(path, start, goal)){
                rebuildRouteAndPlans();
                return false;
            }

            replacement.add(path);
            for(int j = 1; j < path.points.size; j++) addRouteTile(path.points.get(j));
        }

        segments.clear();
        segments.addAll(replacement);
        rebuildRouteAndPlans();
        return true;
    }

    private void rebuildRouteAndPlans(){
        routeTiles.clear();
        routeBridges.clear();
        routePlans.clear();
        routeKeys.clear();
        waypointKeys.clear();
        connectionWaypointKeys.clear();
        smartCrossings = 0;
        smartBridges = 0;
        oreFallbackSegments = 0;
        connectionEndpoints = 0;

        for(Point2 waypoint : waypoints){
            int key = tileKey(waypoint.x, waypoint.y);
            waypointKeys.add(key);
            if(isConnectionEndpoint(waypoint.x, waypoint.y)){
                connectionWaypointKeys.add(key);
                connectionEndpoints++;
            }
        }

        if(waypoints.isEmpty() || routeBlock == null){
            previewPlanFingerprint = queuedPlanFingerprint;
            return;
        }

        addRouteTile(waypoints.first());
        for(PathResult segment : segments){
            if(segment.usedOreFallback) oreFallbackSegments++;
            for(int i = 1; i < segment.points.size; i++) addRouteTile(segment.points.get(i));
            for(BridgeLink bridge : segment.bridges) routeBridges.add(bridge.copy());
        }

        HashMap<Integer, BuildPlan> plansByKey = new HashMap<>();

        for(int i = 0; i < routeTiles.size; i++){
            Point2 current = routeTiles.get(i);
            int key = tileKey(current.x, current.y);

            // An explicitly tapped existing/planned transport tile means
            // "connect here". Do not replace it with another conveyor plan.
            if(waypointKeys.contains(key) && isConnectionEndpoint(current.x, current.y)) continue;

            int rotation;
            if(i < routeTiles.size - 1){
                rotation = direction(current, routeTiles.get(i + 1));
            }else if(i > 0){
                rotation = direction(routeTiles.get(i - 1), current);
            }else{
                rotation = 0;
            }

            BuildPlan plan = new BuildPlan(current.x, current.y, rotation, routeBlock);
            routePlans.add(plan);
            plansByKey.put(key, plan);
        }

        applySmartCrossingReplacements();
        applyBridgeReplacements(plansByKey);
        previewPlanFingerprint = queuedPlanFingerprint;
    }

    /** Uses Mindustry's official getReplacement logic for perpendicular crossings. */
    private void applySmartCrossingReplacements(){
        smartCrossings = 0;
        if(routeBlock == null || routePlans.isEmpty()) return;

        for(BuildPlan plan : routePlans){
            Block replacement = routeBlock.getReplacement(plan, routePlans);
            if(replacement != null && replacement != routeBlock &&
                replacement.unlockedNow() && !replacement.isHidden()){
                plan.block = replacement;
                smartCrossings++;
            }
        }
    }

    private void applyBridgeReplacements(HashMap<Integer, BuildPlan> plansByKey){
        smartBridges = 0;
        BridgeSpec spec = bridgeSpec();
        if(spec == null || !bridgesEnabled) return;

        for(BridgeLink link : routeBridges){
            BuildPlan startPlan = plansByKey.get(tileKey(link.start.x, link.start.y));
            BuildPlan endPlan = plansByKey.get(tileKey(link.end.x, link.end.y));
            if(startPlan == null || endPlan == null) continue;

            int rotation = direction(link.start, link.end);
            startPlan.block = spec.block;
            startPlan.rotation = spec.block.planRotation(rotation);
            endPlan.block = spec.block;
            endPlan.rotation = spec.block.planRotation(endPlan.rotation);

            if(spec.block instanceof ItemBridge){
                startPlan.config = new Point2(link.end.x - link.start.x, link.end.y - link.start.y);
                endPlan.config = null;
            }else if(spec.block instanceof DirectionBridge){
                // Direction bridges auto-link forward. The ending bridge keeps
                // its original route direction so the route may turn afterwards.
                startPlan.rotation = spec.block.planRotation(rotation);
            }

            smartBridges++;
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
        if(routeBlock == null) return;
        if(waypoints.size < 2){
            Vars.ui.showInfoToast("Tap Point B before building the route.", 3f);
            return;
        }
        if(routePlans.isEmpty()){
            Vars.ui.showInfoToast("There are no new blocks to queue for this route.", 3f);
            return;
        }
        if(Vars.player == null || Vars.player.unit() == null){
            Vars.ui.showInfoToast("No player unit is available to build the route.", 3f);
            return;
        }

        refreshQueuedPlanSnapshot();
        if(queuedPlanFingerprint != previewPlanFingerprint){
            if(recalculateAllSegments(false)){
                Vars.ui.showInfoToast("Your build queue changed. The route was updated; review it and tap Build again.", 5f);
            }else{
                Vars.ui.showInfoToast("Your build queue changed and the old route is no longer valid.", 5f);
            }
            return;
        }

        // Commit the route atomically. A partial route is especially unsafe
        // when one of two linked bridge endpoints became invalid after the
        // preview was calculated.
        if(!allPlansUsable()){
            if(recalculateAllSegments(true) && allPlansUsable()){
                Vars.ui.showInfoToast(
                    "The map changed. The route was updated; review it and tap Build again.",
                    5f
                );
            }else{
                Vars.ui.showInfoToast("The route is no longer placeable.", 4f);
            }
            return;
        }

        int queued = routePlans.size;
        for(BuildPlan plan : routePlans){
            Vars.player.unit().addBuild(plan.copy());
        }

        String extras = "";
        if(smartCrossings > 0) extras += " " + smartCrossings + " junction" + (smartCrossings == 1 ? "" : "s") + ".";
        if(smartBridges > 0) extras += " " + smartBridges + " bridge" + (smartBridges == 1 ? "" : "s") + ".";

        Vars.ui.showInfoToast(
            "Queued " + queued + " build plan" + (queued == 1 ? "." : "s.") + extras,
            4f
        );
        clearRoute();
    }

    private boolean isEndpointUsable(int x, int y){
        Tile tile = Vars.world.tile(x, y);
        if(tile == null || forbiddenKeys.contains(tileKey(x, y))) return false;

        if(isConnectionEndpoint(x, y)) return true;

        if(tile.block() == routeBlock && tile.team() == Vars.player.team()) return true;

        // A waypoint must never silently replace an unrelated existing
        // building. Existing compatible conveyors/ducts are handled above as
        // intentional connection endpoints.
        if(tile.build != null) return false;

        for(int rotation = 0; rotation < 4; rotation++){
            if(Build.validPlace(routeBlock, Vars.player.team(), x, y, rotation)) return true;
        }
        return false;
    }

    private boolean isPlanUsable(BuildPlan plan){
        Tile tile = Vars.world.tile(plan.x, plan.y);
        if(tile == null || plan.block == null) return false;

        int key = tileKey(plan.x, plan.y);
        if(forbiddenKeys.contains(key)) return false;

        // Re-check merge/drill safety immediately before building. Explicit
        // waypoints are intentional exceptions, and Junction replacements are
        // expected to occupy an existing conveyor crossing.
        Block junction = junctionReplacement();
        if(!waypointKeys.contains(key) && plan.block != junction &&
            (isBesideDrill(plan.x, plan.y) ||
                (isFedByExistingTransport(plan.x, plan.y) &&
                    !isIntentionalConnectionOutputTile(plan.x, plan.y)))){
            return false;
        }

        return Build.validPlace(plan.block, Vars.player.team(), plan.x, plan.y, plan.rotation) ||
            (tile.block() == plan.block && tile.team() == Vars.player.team());
    }

    private boolean allPlansUsable(){
        if(!connectionsStillValid()) return false;
        for(BuildPlan plan : routePlans){
            if(!isPlanUsable(plan)) return false;
        }
        return allBridgeLinksUsable();
    }

    private boolean connectionsStillValid(){
        for(Point2 waypoint : waypoints){
            int key = tileKey(waypoint.x, waypoint.y);
            if(connectionWaypointKeys.contains(key) &&
                !isConnectionEndpoint(waypoint.x, waypoint.y)){
                return false;
            }
        }

        for(int i = 0; i < segments.size; i++){
            PathResult path = segments.get(i);
            if(path.points.size < 2) return false;

            Point2 start = waypoints.get(i);
            Point2 goal = waypoints.get(i + 1);

            if(connectionWaypointKeys.contains(tileKey(start.x, start.y))){
                int outgoing = direction(path.points.get(0), path.points.get(1));
                if(connectionRotation(start.x, start.y) != outgoing) return false;
            }

            if(connectionWaypointKeys.contains(tileKey(goal.x, goal.y))){
                int arrival = direction(path.points.get(path.points.size - 2), path.points.peek());
                if(!isValidConnectionArrival(goal.x, goal.y, arrival)) return false;
            }
        }
        return true;
    }

    private boolean allBridgeLinksUsable(){
        BridgeSpec spec = bridgeSpec();
        if(routeBridges.isEmpty()) return true;
        if(spec == null || !bridgesEnabled) return false;

        for(BridgeLink link : routeBridges){
            BuildPlan startPlan = findRoutePlan(link.start.x, link.start.y);
            BuildPlan endPlan = findRoutePlan(link.end.x, link.end.y);
            if(startPlan == null || endPlan == null ||
                startPlan.block != spec.block || endPlan.block != spec.block){
                return false;
            }

            int distance = manhattan(link.start.x, link.start.y, link.end.x, link.end.y);
            if(distance < 2 || distance > spec.range ||
                (link.start.x != link.end.x && link.start.y != link.end.y)){
                return false;
            }

            int direction = direction(link.start, link.end);
            for(int step = 1; step < distance; step++){
                int x = link.start.x + dirX[direction] * step;
                int y = link.start.y + dirY[direction] * step;
                int key = tileKey(x, y);
                if(forbiddenKeys.contains(key)) return false;

                if(spec.block instanceof DirectionBridge){
                    Tile tile = Vars.world.tile(x, y);
                    BuildPlan planned = queuedPlansByKey.get(key);
                    if((tile != null && tile.build != null && tile.team() == Vars.player.team() &&
                        tile.block() == spec.block) ||
                        (planned != null && planned.block == spec.block)){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void drawPreview(){
        if(!active || routeBlock == null || Vars.state.isMenu()) return;

        for(Point2 point : forbiddenTiles){
            Tile tile = Vars.world.tile(point.x, point.y);
            if(tile != null){
                Drawf.square(tile.drawx(), tile.drawy(), Vars.tilesize / 2f - 1f, Pal.remove);
            }
        }

        for(BuildPlan plan : routePlans){
            plan.animScale = 1f;
            plan.block.drawPlan(plan, routePlans, isPlanUsable(plan), 1f);
        }

        drawBridgePreviewLinks();
    }

    private void drawBridgePreviewLinks(){
        BridgeSpec spec = bridgeSpec();
        if(spec == null || routeBridges.isEmpty()) return;

        for(BridgeLink link : routeBridges){
            BuildPlan startPlan = findRoutePlan(link.start.x, link.start.y);
            BuildPlan endPlan = findRoutePlan(link.end.x, link.end.y);
            if(startPlan == null || endPlan == null) continue;

            int rotation = direction(link.start, link.end);
            if(spec.block instanceof ItemBridge itemBridge){
                itemBridge.drawBridge(startPlan, endPlan.drawx(), endPlan.drawy(), 0f);
            }else if(spec.block instanceof DirectionBridge directionBridge){
                directionBridge.drawBridge(
                    rotation,
                    startPlan.drawx(),
                    startPlan.drawy(),
                    endPlan.drawx(),
                    endPlan.drawy(),
                    null
                );
            }
        }
    }

    private BuildPlan findRoutePlan(int x, int y){
        for(BuildPlan plan : routePlans){
            if(plan.x == x && plan.y == y) return plan;
        }
        return null;
    }

    private PathResult findPath(Point2 start, Point2 goal){
        searchTimedOut = false;
        searchLimitHit = false;

        PathResult result;
        switch(oreMode){
            case avoid -> result = findPathWithOreRule(start, goal, true);
            case allow -> result = findPathWithOreRule(start, goal, false);
            default -> {
                result = findPathWithOreRule(start, goal, true);
                if(result == null && !searchTimedOut && !searchLimitHit){
                    result = findPathWithOreRule(start, goal, false);
                    if(result != null) result.usedOreFallback = true;
                }
            }
        }
        return result;
    }

    private PathResult findPathWithOreRule(Point2 start, Point2 goal, boolean strictOre){
        String cacheKey = pathCacheKey(start, goal, strictOre);
        PathResult cached = pathCache.get(cacheKey);
        if(cached != null) return cached.copy();

        int worldWidth = Vars.world.width();
        int worldHeight = Vars.world.height();
        int[] margins = {8, 16, 32, 64, 96};
        long deadline = System.nanoTime() + searchBudgetNanos();

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

            PathResult result = searchRegion(start, goal, minX, minY, maxX, maxY, strictOre, deadline);
            if(result != null){
                putPathCache(cacheKey, result);
                return result.copy();
            }
            if(searchTimedOut || searchLimitHit) return null;

            if(minX == 0 && minY == 0 && maxX == worldWidth - 1 && maxY == worldHeight - 1){
                return null;
            }
        }

        PathResult result = searchRegion(start, goal, 0, 0, worldWidth - 1, worldHeight - 1, strictOre, deadline);
        if(result != null){
            putPathCache(cacheKey, result);
            return result.copy();
        }
        return null;
    }

    private PathResult searchRegion(
        Point2 start,
        Point2 goal,
        int minX,
        int minY,
        int maxX,
        int maxY,
        boolean strictOre,
        long deadline
    ){
        int regionWidth = maxX - minX + 1;
        int regionHeight = maxY - minY + 1;
        int cellCount = regionWidth * regionHeight;

        if(cellCount <= 0) return null;
        if(cellCount > maxSearchCells()){
            searchLimitHit = true;
            return null;
        }

        int stateCount = cellCount * 5;
        float[] bestCost = new float[stateCount];
        int[] parent = new int[stateCount];
        boolean[] closed = new boolean[stateCount];
        boolean[] bridgeFromParent = new boolean[stateCount];
        Arrays.fill(bestCost, Float.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);

        int startState = stateIndex(start.x, start.y, 4, minX, minY, regionWidth);
        bestCost[startState] = 0f;

        PriorityQueue<SearchNode> open = new PriorityQueue<>();
        open.add(new SearchNode(startState, 0f, heuristic(start.x, start.y, goal.x, goal.y)));

        int goalState = -1;
        int expanded = 0;
        BridgeSpec bridgeSpec = bridgesEnabled ? bridgeSpec() : null;

        while(!open.isEmpty()){
            if(expanded >= maxExpandedStates()){
                searchLimitHit = true;
                break;
            }
            if((expanded & 255) == 0 && System.nanoTime() > deadline){
                searchTimedOut = true;
                break;
            }

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

            Tile currentTile = Vars.world.tile(x, y);
            int forcedStartDirection = (x == start.x && y == start.y && isConnectionEndpoint(x, y)) ?
                connectionRotation(x, y) : -1;

            for(int nextDirection = 0; nextDirection < 4; nextDirection++){
                if(forcedStartDirection != -1 && nextDirection != forcedStartDirection) continue;

                if(incomingDirection != 4 && isExistingFriendlyConveyor(currentTile) &&
                    nextDirection != incomingDirection){
                    continue;
                }

                int nextX = x + dirX[nextDirection];
                int nextY = y + dirY[nextDirection];

                if(nextX >= minX && nextX <= maxX && nextY >= minY && nextY <= maxY &&
                    isTraversable(nextX, nextY, nextDirection, start, goal, strictOre)){

                    float step = groundStepCost(x, y, nextX, nextY, incomingDirection, nextDirection, goal, strictOre);
                    relaxState(
                        node.state,
                        nextX,
                        nextY,
                        nextDirection,
                        false,
                        step,
                        goal,
                        minX,
                        minY,
                        regionWidth,
                        bestCost,
                        parent,
                        bridgeFromParent,
                        closed,
                        open
                    );
                }

                if(bridgeSpec != null){
                    addBridgeNeighbours(
                        node,
                        x,
                        y,
                        incomingDirection,
                        nextDirection,
                        start,
                        goal,
                        minX,
                        minY,
                        maxX,
                        maxY,
                        regionWidth,
                        strictOre,
                        bridgeSpec,
                        bestCost,
                        parent,
                        bridgeFromParent,
                        closed,
                        open
                    );
                }
            }
        }

        if(goalState == -1) return null;
        return reconstructPath(goalState, startState, parent, bridgeFromParent, minX, minY, regionWidth);
    }

    private void relaxState(
        int parentState,
        int nextX,
        int nextY,
        int nextDirection,
        boolean bridge,
        float step,
        Point2 goal,
        int minX,
        int minY,
        int regionWidth,
        float[] bestCost,
        int[] parent,
        boolean[] bridgeFromParent,
        boolean[] closed,
        PriorityQueue<SearchNode> open
    ){
        int nextState = stateIndex(nextX, nextY, nextDirection, minX, minY, regionWidth);
        if(closed[nextState]) return;

        float newCost = bestCost[parentState] + step;
        if(newCost + 0.0001f < bestCost[nextState]){
            bestCost[nextState] = newCost;
            parent[nextState] = parentState;
            bridgeFromParent[nextState] = bridge;
            float estimate = newCost + heuristic(nextX, nextY, goal.x, goal.y);
            open.add(new SearchNode(nextState, newCost, estimate));
        }
    }

    private void addBridgeNeighbours(
        SearchNode node,
        int x,
        int y,
        int incomingDirection,
        int bridgeDirection,
        Point2 start,
        Point2 goal,
        int minX,
        int minY,
        int maxX,
        int maxY,
        int regionWidth,
        boolean strictOre,
        BridgeSpec spec,
        float[] bestCost,
        int[] parent,
        boolean[] bridgeFromParent,
        boolean[] closed,
        PriorityQueue<SearchNode> open
    ){
        if(!canPlaceBridgeEndpoint(x, y, bridgeDirection, start, goal, spec, strictOre)) return;

        boolean hardObstacleSeen = false;
        int softCrossings = 0;

        for(int distance = 2; distance <= spec.range; distance++){
            int endX = x + dirX[bridgeDirection] * distance;
            int endY = y + dirY[bridgeDirection] * distance;
            if(endX < minX || endX > maxX || endY < minY || endY > maxY) break;

            int middleX = x + dirX[bridgeDirection] * (distance - 1);
            int middleY = y + dirY[bridgeDirection] * (distance - 1);

            // Never jump past the requested endpoint. If the goal is inside
            // this span, a shorter normal/bridge edge must reach it directly.
            if(middleX == goal.x && middleY == goal.y) break;

            int middleKey = tileKey(middleX, middleY);

            // Direction bridges auto-link to the first matching bridge ahead.
            // Do not span across another matching built/planned endpoint or an
            // earlier tile in this route, as that could link to the wrong one.
            if(spec.block instanceof DirectionBridge){
                Tile middleTile = Vars.world.tile(middleX, middleY);
                BuildPlan middlePlan = queuedPlansByKey.get(middleKey);
                if(routeKeys.contains(middleKey) ||
                    (middleTile != null && middleTile.build != null &&
                        middleTile.team() == Vars.player.team() && middleTile.block() == spec.block) ||
                    (middlePlan != null && middlePlan.block == spec.block)){
                    break;
                }
            }

            // A user-marked forbidden tile blocks the route's airspace too;
            // this lets the player reserve an entire corridor, not merely the
            // ground placement layer.
            if(forbiddenKeys.contains(middleKey)) break;

            int obstacleType = bridgeObstacleType(
                middleX,
                middleY,
                bridgeDirection,
                start,
                goal,
                strictOre
            );
            if(obstacleType == 2){
                hardObstacleSeen = true;
            }else if(obstacleType == 1){
                softCrossings++;
            }

            // One perpendicular conveyor is best solved by a junction. A real
            // bridge becomes a candidate for an unplaceable obstacle or for a
            // cluster of two or more conveyor crossings.
            boolean bridgeWorthwhile = hardObstacleSeen || softCrossings >= 2;
            if(!bridgeWorthwhile) continue;
            if(!canPlaceBridgeEndpoint(endX, endY, bridgeDirection, start, goal, spec, strictOre)) continue;

            float step = distance + bridgePenalty();
            if(incomingDirection != 4 && incomingDirection != bridgeDirection){
                step += turnPenalty();
            }

            relaxState(
                node.state,
                endX,
                endY,
                bridgeDirection,
                true,
                step,
                goal,
                minX,
                minY,
                regionWidth,
                bestCost,
                parent,
                bridgeFromParent,
                closed,
                open
            );
        }
    }

    private PathResult reconstructPath(
        int goalState,
        int startState,
        int[] parent,
        boolean[] bridgeFromParent,
        int minX,
        int minY,
        int regionWidth
    ){
        Seq<Integer> reversedStates = new Seq<>();
        int current = goalState;

        while(current != -1){
            reversedStates.add(current);
            if(current == startState) break;
            current = parent[current];
        }

        if(reversedStates.isEmpty() || reversedStates.peek() != startState) return null;

        PathResult result = new PathResult();
        for(int i = reversedStates.size - 1; i >= 0; i--){
            int state = reversedStates.get(i);
            int cell = state / 5;
            int localX = cell % regionWidth;
            int localY = cell / regionWidth;
            Point2 point = new Point2(minX + localX, minY + localY);

            if(!result.points.isEmpty() && bridgeFromParent[state]){
                result.bridges.add(new BridgeLink(result.points.peek(), point));
            }
            result.points.add(point);
        }
        return result;
    }

    private float groundStepCost(
        int x,
        int y,
        int nextX,
        int nextY,
        int incomingDirection,
        int nextDirection,
        Point2 goal,
        boolean strictOre
    ){
        float step = 1f;
        if(incomingDirection != 4 && incomingDirection != nextDirection) step += turnPenalty();

        Tile nextTile = Vars.world.tile(nextX, nextY);
        if(!strictOre && nextTile != null && nextTile.drop() != null &&
            !(nextX == goal.x && nextY == goal.y)){
            step += allowedOrePenalty;
        }

        if(nextTile != null && isExistingFriendlyConveyor(nextTile) &&
            !(nextX == goal.x && nextY == goal.y)){
            step += existingConveyorCrossingPenalty();
        }

        if(preference == RoutePreference.clean){
            step += nearbyInterferencePenalty(nextX, nextY);
        }
        return step;
    }

    private boolean isTraversable(
        int x,
        int y,
        int rotation,
        Point2 start,
        Point2 goal,
        boolean strictOre
    ){
        Tile tile = Vars.world.tile(x, y);
        if(tile == null) return false;

        boolean isStart = x == start.x && y == start.y;
        boolean isGoal = x == goal.x && y == goal.y;
        int key = tileKey(x, y);

        if(forbiddenKeys.contains(key)) return false;
        if(routeKeys.contains(key) && !isStart && !isGoal) return false;
        if(strictOre && !isGoal && tile.drop() != null) return false;
        if(!isStart && !isGoal && isBesideDrill(x, y)) return false;
        if(!isStart && !isGoal && isFedByExistingTransport(x, y) &&
            !isIntentionalConnectionOutputTile(x, y)) return false;

        BuildPlan queued = queuedPlansByKey.get(key);
        if(queued != null && !isStart && !isGoal) return false;

        if(isGoal && isConnectionEndpoint(x, y)){
            return isValidConnectionArrival(x, y, rotation);
        }

        if(!isStart && !isGoal && isExistingFriendlyConveyor(tile)){
            return canCrossExistingConveyor(tile, rotation);
        }

        // Never use normal replacement rules to overwrite an unrelated built
        // transport block, router, bridge, pipe, or factory. Such blocks are
        // obstacles unless a real bridge is selected over them.
        if(tile.build != null) return false;

        return Build.validPlace(routeBlock, Vars.player.team(), x, y, rotation);
    }

    /**
     * Classifies a potential bridge middle tile.
     * 0 = ordinary placeable ground, 1 = a junction-capable conveyor crossing,
     * 2 = a hard obstacle or reserved/planned tile.
     */
    private int bridgeObstacleType(
        int x,
        int y,
        int rotation,
        Point2 start,
        Point2 goal,
        boolean strictOre
    ){
        Tile tile = Vars.world.tile(x, y);
        if(tile == null) return 2;
        int key = tileKey(x, y);

        if(forbiddenKeys.contains(key) || queuedPlansByKey.containsKey(key)) return 2;
        if(strictOre && tile.drop() != null) return 2;
        if(routeKeys.contains(key) && !(x == start.x && y == start.y)) return 2;
        if(isBesideDrill(x, y)) return 2;
        if(isFedByExistingTransport(x, y) && !isIntentionalConnectionOutputTile(x, y)) return 2;

        if(isExistingFriendlyConveyor(tile)){
            return canCrossExistingConveyor(tile, rotation) ? 1 : 2;
        }
        if(tile.build != null) return 2;

        return Build.validPlace(routeBlock, Vars.player.team(), x, y, rotation) ? 0 : 2;
    }

    private boolean canPlaceBridgeEndpoint(
        int x,
        int y,
        int rotation,
        Point2 start,
        Point2 goal,
        BridgeSpec spec,
        boolean strictOre
    ){
        Tile tile = Vars.world.tile(x, y);
        if(tile == null) return false;
        int key = tileKey(x, y);

        if(forbiddenKeys.contains(key) || queuedPlansByKey.containsKey(key)) return false;
        if(routeKeys.contains(key) && !(x == start.x && y == start.y)) return false;
        if(strictOre && tile.drop() != null &&
            !(x == start.x && y == start.y) && !(x == goal.x && y == goal.y)) return false;
        if(isConnectionEndpoint(x, y)) return false;
        if(isBesideDrill(x, y) && !(x == start.x && y == start.y) && !(x == goal.x && y == goal.y)) return false;
        if(isFedByExistingTransport(x, y) && !isIntentionalConnectionOutputTile(x, y) &&
            !(x == start.x && y == start.y) && !(x == goal.x && y == goal.y)) return false;

        // Do not silently replace a built structure to create a bridge endpoint.
        if(tile.build != null && tile.block() != spec.block) return false;

        return Build.validPlace(spec.block, Vars.player.team(), x, y, rotation) ||
            (tile.block() == spec.block && tile.team() == Vars.player.team());
    }

    private boolean isExistingFriendlyConveyor(Tile tile){
        return tile != null && tile.build != null && tile.team() == Vars.player.team() &&
            (tile.block() instanceof Conveyor || tile.block() instanceof Duct);
    }

    /**
     * Returns true when a built or locally planned compatible transport block
     * is pointing directly into this tile. Placing an unrelated automatic
     * route here would merge its items into the new line.
     */
    private boolean isFedByExistingTransport(int x, int y){
        for(int direction = 0; direction < 4; direction++){
            int sourceX = x - dirX[direction];
            int sourceY = y - dirY[direction];
            Tile source = Vars.world.tile(sourceX, sourceY);

            if(source == null) continue;

            if(source.build != null && source.team() == Vars.player.team() &&
                isCompatibleTransport(source.block()) && source.build.rotation == direction){
                return true;
            }

            BuildPlan planned = queuedPlansByKey.get(tileKey(sourceX, sourceY));
            if(planned != null && isCompatibleTransport(planned.block) && planned.rotation == direction){
                return true;
            }
        }
        return false;
    }

    private boolean isIntentionalConnectionOutputTile(int x, int y){
        for(Point2 waypoint : waypoints){
            if(!isConnectionEndpoint(waypoint.x, waypoint.y)) continue;
            int rotation = connectionRotation(waypoint.x, waypoint.y);
            if(rotation >= 0 && waypoint.x + dirX[rotation] == x &&
                waypoint.y + dirY[rotation] == y){
                return true;
            }
        }
        return false;
    }

    private boolean canCrossExistingConveyor(Tile tile, int routeRotation){
        if(tile == null || tile.build == null) return false;

        Block replacement = junctionReplacement();
        if(replacement == null || !replacement.unlockedNow() || replacement.isHidden()) return false;

        boolean compatibleExisting =
            (routeBlock instanceof Conveyor && tile.block() instanceof Conveyor) ||
            (routeBlock instanceof Duct && tile.block() instanceof Duct);
        if(!compatibleExisting) return false;

        boolean perpendicular = Math.floorMod(tile.build.rotation - routeRotation, 2) == 1;
        if(!perpendicular) return false;

        return Build.validPlace(replacement, Vars.player.team(), tile.x, tile.y, 0) ||
            tile.block() == replacement;
    }

    private Block junctionReplacement(){
        if(routeBlock instanceof Conveyor conveyor) return conveyor.junctionReplacement;
        if(routeBlock instanceof Duct duct) return duct.junctionReplacement;
        return null;
    }

    private BridgeSpec bridgeSpec(){
        Block bridge = null;
        if(routeBlock instanceof Conveyor conveyor){
            bridge = conveyor.bridgeReplacement;
        }else if(routeBlock instanceof Duct duct){
            bridge = duct.bridgeReplacement;
        }

        if(bridge == null || !bridge.unlockedNow() || bridge.isHidden()) return null;
        if(bridge instanceof ItemBridge itemBridge) return new BridgeSpec(bridge, itemBridge.range);
        if(bridge instanceof DirectionBridge directionBridge) return new BridgeSpec(bridge, directionBridge.range);
        return null;
    }

    private boolean isBesideDrill(int x, int y){
        for(int direction = 0; direction < 4; direction++){
            Tile nearby = Vars.world.tile(x + dirX[direction], y + dirY[direction]);
            if(nearby == null) continue;

            Block block = nearby.block();
            if(block.flags.contains(BlockFlag.drill)) return true;
        }
        return false;
    }

    private boolean isConnectionEndpoint(int x, int y){
        Tile tile = Vars.world.tile(x, y);
        if(tile != null && tile.build != null && tile.team() == Vars.player.team() &&
            isCompatibleTransport(tile.block())){
            return true;
        }

        BuildPlan planned = queuedPlansByKey.get(tileKey(x, y));
        return planned != null && planned.block != null && isCompatibleTransport(planned.block);
    }

    private boolean isCompatibleTransport(Block block){
        if(block == null) return false;
        if(routeBlock instanceof Conveyor) return block instanceof Conveyor;
        if(routeBlock instanceof Duct) return block instanceof Duct;
        return block == routeBlock;
    }

    private int connectionRotation(int x, int y){
        Tile tile = Vars.world.tile(x, y);
        if(tile != null && tile.build != null && tile.team() == Vars.player.team() &&
            isCompatibleTransport(tile.block())){
            return tile.build.rotation;
        }

        BuildPlan planned = queuedPlansByKey.get(tileKey(x, y));
        return planned == null ? -1 : planned.rotation;
    }

    private Block connectionBlock(int x, int y){
        Tile tile = Vars.world.tile(x, y);
        if(tile != null && tile.build != null && tile.team() == Vars.player.team() &&
            isCompatibleTransport(tile.block())){
            return tile.block();
        }

        BuildPlan planned = queuedPlansByKey.get(tileKey(x, y));
        return planned == null ? null : planned.block;
    }

    private boolean isValidConnectionArrival(int x, int y, int arrivalDirection){
        Block target = connectionBlock(x, y);
        int existingRotation = connectionRotation(x, y);
        if(target == null || existingRotation < 0) return false;

        if(requiresRearInput(target)){
            return arrivalDirection == existingRotation;
        }

        // Standard conveyors and ducts accept rear/side input, but not input
        // from the tile they already output toward.
        return arrivalDirection != Math.floorMod(existingRotation + 2, 4);
    }

    private boolean requiresRearInput(Block target){
        if(target instanceof Duct duct) return duct.armored;
        return target != null && target.noSideBlend;
    }

    private boolean validateIntentionalConnections(PathResult path, Point2 start, Point2 goal){
        if(path == null || path.points.size < 2) return false;

        if(isConnectionEndpoint(start.x, start.y)){
            int required = direction(path.points.get(0), path.points.get(1));
            int existingRotation = connectionRotation(start.x, start.y);
            if(existingRotation != required){
                Vars.ui.showInfoToast(
                    "The starting conveyor points another way. Use it as an end point, rotate it, or choose the tile in front of it.",
                    5f
                );
                return false;
            }
        }

        if(isConnectionEndpoint(goal.x, goal.y)){
            Block target = connectionBlock(goal.x, goal.y);
            int arrivalDirection = direction(path.points.get(path.points.size - 2), path.points.peek());

            if(!isValidConnectionArrival(goal.x, goal.y, arrivalDirection)){
                Vars.ui.showInfoToast(
                    requiresRearInput(target) ?
                        "That armored endpoint only accepts a straight rear connection." :
                        "That endpoint cannot accept a head-on connection. Connect from its rear or side.",
                    5f
                );
                return false;
            }
        }
        return true;
    }

    /** Captures the local player's queued and currently previewed plans. */
    private void refreshQueuedPlanSnapshot(){
        queuedPlansByKey.clear();
        int fingerprint = 1;

        if(Vars.player != null && Vars.player.unit() != null){
            for(BuildPlan plan : Vars.player.unit().plans()){
                fingerprint = addQueuedPlanSnapshot(plan, fingerprint);
            }
        }

        if(Vars.control != null && Vars.control.input != null){
            for(BuildPlan plan : Vars.control.input.linePlans){
                fingerprint = addQueuedPlanSnapshot(plan, fingerprint);
            }
            for(BuildPlan plan : Vars.control.input.selectPlans){
                fingerprint = addQueuedPlanSnapshot(plan, fingerprint);
            }
        }

        queuedPlanFingerprint = fingerprint;
    }

    private int addQueuedPlanSnapshot(BuildPlan plan, int fingerprint){
        if(plan == null || plan.breaking || plan.block == null) return fingerprint;
        int key = tileKey(plan.x, plan.y);
        queuedPlansByKey.put(key, plan);
        return 31 * fingerprint + key * 7 + plan.block.id * 3 + plan.rotation;
    }

    private float turnPenalty(){
        return switch(preference){
            case shortest -> 0.08f;
            case straight -> 2.25f;
            case clean -> 0.45f;
        };
    }

    private float existingConveyorCrossingPenalty(){
        return switch(preference){
            case shortest -> 4f;
            case straight -> 8f;
            case clean -> 20f;
        };
    }

    private float bridgePenalty(){
        return switch(preference){
            case shortest -> 2f;
            case straight -> 2.5f;
            case clean -> 1f;
        };
    }

    private float nearbyInterferencePenalty(int x, int y){
        float penalty = 0f;
        for(int direction = 0; direction < 4; direction++){
            int nx = x + dirX[direction];
            int ny = y + dirY[direction];
            Tile nearby = Vars.world.tile(nx, ny);
            if(nearby != null && nearby.build != null && !isCompatibleTransport(nearby.block())){
                penalty += 0.45f;
            }
            if(nearby != null && queuedPlansByKey.containsKey(tileKey(nx, ny))){
                penalty += 0.55f;
            }
        }
        return penalty;
    }

    private int maxExpandedStates(){
        return Vars.mobile ? mobileMaxExpandedStates : desktopMaxExpandedStates;
    }

    private int maxSearchCells(){
        return Vars.mobile ? mobileMaxSearchCells : desktopMaxSearchCells;
    }

    private int maxWaypointDistance(){
        return Vars.mobile ? 500 : 1000;
    }

    private long searchBudgetNanos(){
        return (Vars.mobile ? 220L : 550L) * 1_000_000L;
    }

    private String pathCacheKey(Point2 start, Point2 goal, boolean strictOre){
        int routeHash = 1;
        for(Point2 point : routeTiles){
            routeHash = 31 * routeHash + tileKey(point.x, point.y);
        }

        return start.x + "," + start.y + ">" + goal.x + "," + goal.y +
            "|o=" + strictOre +
            "|p=" + preference.ordinal() +
            "|b=" + bridgesEnabled +
            "|f=" + forbiddenRevision +
            "|q=" + queuedPlanFingerprint +
            "|r=" + routeHash;
    }

    private void putPathCache(String key, PathResult result){
        if(pathCache.size() >= pathCacheLimit){
            pathCache.clear();
        }
        pathCache.put(key, result.copy());
    }

    private int stateIndex(int x, int y, int direction, int minX, int minY, int regionWidth){
        int localCell = (x - minX) + (y - minY) * regionWidth;
        return localCell * 5 + direction;
    }

    private float heuristic(int x, int y, int goalX, int goalY){
        return manhattan(x, y, goalX, goalY);
    }

    private int manhattan(int x1, int y1, int x2, int y2){
        return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }

    private int tileKey(int x, int y){
        return x + y * Vars.world.width();
    }

    private enum OreMode{
        auto("Ore: automatic fallback", "Auto ore"),
        avoid("Ore: never cross", "No ore"),
        allow("Ore: allow with penalty", "Ore ok");

        final String label;
        final String shortLabel;

        OreMode(String label, String shortLabel){
            this.label = label;
            this.shortLabel = shortLabel;
        }

        OreMode next(){
            return values()[(ordinal() + 1) % values().length];
        }

        static OreMode fromOrdinal(int ordinal){
            return values()[Math.floorMod(ordinal, values().length)];
        }
    }

    private enum RoutePreference{
        shortest("Route: shortest", "Short"),
        straight("Route: straightest", "Straight"),
        clean("Route: least interference", "Clean");

        final String label;
        final String shortLabel;

        RoutePreference(String label, String shortLabel){
            this.label = label;
            this.shortLabel = shortLabel;
        }

        RoutePreference next(){
            return values()[(ordinal() + 1) % values().length];
        }

        static RoutePreference fromOrdinal(int ordinal){
            return values()[Math.floorMod(ordinal, values().length)];
        }
    }

    private static final class BridgeSpec{
        final Block block;
        final int range;

        BridgeSpec(Block block, int range){
            this.block = block;
            this.range = range;
        }
    }

    private static final class BridgeLink{
        final Point2 start;
        final Point2 end;

        BridgeLink(Point2 start, Point2 end){
            this.start = new Point2(start.x, start.y);
            this.end = new Point2(end.x, end.y);
        }

        BridgeLink copy(){
            return new BridgeLink(start, end);
        }
    }

    private static final class PathResult{
        final Seq<Point2> points = new Seq<>();
        final Seq<BridgeLink> bridges = new Seq<>();
        boolean usedOreFallback;

        PathResult copy(){
            PathResult copy = new PathResult();
            for(Point2 point : points){
                copy.points.add(new Point2(point.x, point.y));
            }
            for(BridgeLink bridge : bridges){
                copy.bridges.add(bridge.copy());
            }
            copy.usedOreFallback = usedOreFallback;
            return copy;
        }
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
