package autoroute;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Time;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

/**
 * Optional compact HUD additions for Auto Route.
 *
 * <p>The overlay shows core items and friendly unit counts in a two-column
 * grid. The complete panel can be dragged with its four-way handle, keeps a
 * separate position for portrait/landscape, and has an adjustable width.</p>
 */
public class AutoRouteHudExtras{
    public static final String coreItemsSetting = "mindustry-auto-route-core-items";
    public static final String unitsSetting = "mindustry-auto-route-unit-counts";
    public static final String timeControlSetting = "mindustry-auto-route-time-control";
    public static final String hudOpacitySetting = "mindustry-auto-route-hud-opacity";
    public static final String hudWidthSetting = "mindustry-auto-route-hud-width";

    private static final String hudPositionPrefix = "mindustry-auto-route-extras-position-";
    private static final float[] timeSpeeds = {1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f};
    private static final float itemCellHeight = 25f;

    private final Interval refreshTimer = new Interval();
    private final Seq<Item> visibleItems = new Seq<>();
    private final Seq<UnitType> visibleUnits = new Seq<>();

    private Table overlayPanel;
    private TextButton speedButton;
    private String lastContentSignature = "";
    private int speedIndex;

    private boolean panelPositionReady;
    private float lastSceneWidth = -1f;
    private float lastSceneHeight = -1f;

    public void init(){
        buildOverlay();

        Events.on(EventType.ResetEvent.class, event -> {
            resetTimeSpeed();
            lastContentSignature = "";
            rebuildNow();
        });
        Events.on(EventType.WorldLoadEvent.class, event -> {
            resetTimeSpeed();
            lastContentSignature = "";
            rebuildNow();
        });
    }

    private void buildOverlay(){
        overlayPanel = new Table(Styles.black6);
        overlayPanel.margin(3f);
        overlayPanel.visible(this::shouldShowOverlay);
        overlayPanel.update(() -> {
            overlayPanel.color.a = Core.settings.getInt(hudOpacitySetting, 82) / 100f;
            if(refreshTimer.get(30f)) refreshIfNeeded();
            updatePanelPlacement();
        });

        Vars.ui.hudGroup.addChild(overlayPanel);
        rebuildNow();
    }

    private boolean shouldShowOverlay(){
        if(Vars.state.isMenu() || Vars.ui == null || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) return false;
        if(Vars.ui.minimapfrag != null && Vars.ui.minimapfrag.shown()) return false;

        return Core.settings.getBool(coreItemsSetting, true) ||
            Core.settings.getBool(unitsSetting, true) ||
            Core.settings.getBool(timeControlSetting, true);
    }

    public void rebuildNow(){
        if(overlayPanel == null) return;
        lastContentSignature = buildContentSignature();
        rebuildContents();
    }

    private void refreshIfNeeded(){
        String signature = buildContentSignature();
        if(!signature.equals(lastContentSignature)){
            lastContentSignature = signature;
            rebuildContents();
        }
    }

    private String buildContentSignature(){
        StringBuilder result = new StringBuilder();
        boolean showItems = Core.settings.getBool(coreItemsSetting, true);
        boolean showUnits = Core.settings.getBool(unitsSetting, true);
        boolean showTime = Core.settings.getBool(timeControlSetting, true);

        result.append(showItems).append('|').append(showUnits).append('|').append(showTime).append('|');
        result.append(currentHudWidth()).append('|');

        if(Vars.player == null) return result.toString();

        if(showItems){
            CoreBuild core = Vars.player.team().core();
            if(core != null){
                for(Item item : Vars.content.items()){
                    if(core.items.get(item) > 0) result.append('i').append(item.id).append(',');
                }
            }
        }

        if(showUnits){
            for(UnitType unit : Vars.content.units()){
                if(Vars.player.team().data().countType(unit) > 0) result.append('u').append(unit.id).append(',');
            }
        }

        return result.toString();
    }

    private void rebuildContents(){
        overlayPanel.clearChildren();
        overlayPanel.margin(3f);
        visibleItems.clear();
        visibleUnits.clear();

        float totalWidth = currentHudWidth();
        float innerWidth = Math.max(120f, totalWidth - 6f);

        buildMoveHeader(innerWidth);

        boolean addedSection = false;

        if(Core.settings.getBool(coreItemsSetting, true)){
            CoreBuild core = Vars.player == null ? null : Vars.player.team().core();
            if(core != null){
                for(Item item : Vars.content.items()){
                    if(core.items.get(item) > 0) visibleItems.add(item);
                }
            }

            if(!visibleItems.isEmpty()){
                buildItemsSection(innerWidth);
                addedSection = true;
            }
        }

        if(Core.settings.getBool(unitsSetting, true) && Vars.player != null){
            for(UnitType unit : Vars.content.units()){
                if(Vars.player.team().data().countType(unit) > 0) visibleUnits.add(unit);
            }

            if(!visibleUnits.isEmpty()){
                if(addedSection) addDivider(innerWidth);
                buildUnitsSection(innerWidth);
                addedSection = true;
            }
        }

        if(Core.settings.getBool(timeControlSetting, true)){
            if(addedSection) addDivider(innerWidth);
            buildTimeControl(innerWidth);
            addedSection = true;
        }

        overlayPanel.background(Styles.black6);
        overlayPanel.pack();
        clampPanelToScreen();
    }

    private void buildMoveHeader(float innerWidth){
        Table header = new Table();
        header.left();

        TextureRegionDrawable moveIcon = new TextureRegionDrawable(
            Core.atlas.find("mindustry-auto-route-move")
        );

        ImageButton moveButton = header.button(moveIcon, Styles.cleari, () -> {})
            .size(28f)
            .get();
        moveButton.resizeImage(18f);
        addPanelDragListener(moveButton);

        // Keep the top row intentionally minimal: the move handle is the only
        // control needed here, and removing the title saves vertical space.
        overlayPanel.add(header).width(innerWidth).height(28f).left();
        overlayPanel.row();
    }

    private void addPanelDragListener(ImageButton moveButton){
        moveButton.addListener(new InputListener(){
            private float grabX;
            private float grabY;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                grabX = x;
                grabY = y;
                overlayPanel.toFront();
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                overlayPanel.moveBy(x - grabX, y - grabY);
                clampPanelToScreen();
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                savePanelPosition();
            }
        });
    }

    private void buildItemsSection(float innerWidth){
        Table grid = new Table();
        grid.defaults().pad(1f);
        float cellWidth = Math.max(56f, (innerWidth - 2f) / 2f);

        int index = 0;
        for(Item item : visibleItems){
            grid.table(cell -> {
                cell.left();
                cell.image(item.uiIcon)
                    .size(18f)
                    .padRight(3f)
                    .tooltip(t -> t.background(Styles.black6).margin(4f)
                        .add(item.localizedName).style(Styles.outlineLabel));
                cell.label(() -> {
                    if(Vars.player == null) return "0";
                    CoreBuild core = Vars.player.team().core();
                    return core == null ? "0" : formatAmountColored(core.items.get(item));
                }).left().growX();
            }).width(cellWidth).height(itemCellHeight).left();

            if(++index % 2 == 0) grid.row();
        }

        overlayPanel.add(grid).width(innerWidth).left();
        overlayPanel.row();
    }

    private void buildUnitsSection(float innerWidth){
        Table grid = new Table();
        grid.defaults().pad(1f);
        float cellWidth = Math.max(56f, (innerWidth - 2f) / 2f);

        int index = 0;
        for(UnitType unit : visibleUnits){
            grid.table(cell -> {
                cell.left();
                cell.image(unit.uiIcon)
                    .size(18f)
                    .padRight(3f)
                    .tooltip(t -> t.background(Styles.black6).margin(4f)
                        .add(unit.localizedName).style(Styles.outlineLabel));
                cell.label(() -> Vars.player == null ? "0" :
                    formatAmountColored(Vars.player.team().data().countType(unit)))
                    .left().growX();
            }).width(cellWidth).height(itemCellHeight).left();

            if(++index % 2 == 0) grid.row();
        }

        overlayPanel.add(grid).width(innerWidth).left();
        overlayPanel.row();
    }

    private void addDivider(float width){
        overlayPanel.table(line -> line.background(Styles.accentDrawable))
            .width(width)
            .height(1f)
            .padTop(2f)
            .padBottom(2f);
        overlayPanel.row();
    }

    private void buildTimeControl(float innerWidth){
        Table controls = new Table();
        controls.defaults().height(32f).pad(1f);

        float arrowWidth = 32f;
        float speedWidth = Math.max(52f, innerWidth - arrowWidth * 2f - 4f);

        controls.button(Icon.left, Styles.cleari, this::slower)
            .size(arrowWidth);

        speedButton = controls.button(speedText(), Styles.cleart, this::resetTimeSpeed)
            .width(speedWidth)
            .height(32f)
            .get();

        controls.button(Icon.right, Styles.cleari, this::faster)
            .size(arrowWidth);

        overlayPanel.add(controls).width(innerWidth).left();
        overlayPanel.row();
    }

    private void faster(){
        if(!canChangeTime()) return;
        if(speedIndex < timeSpeeds.length - 1) speedIndex++;
        applyTimeSpeed();
    }

    private void slower(){
        if(!canChangeTime()) return;
        if(speedIndex > 0) speedIndex--;
        applyTimeSpeed();
    }

    private boolean canChangeTime(){
        if(Vars.net != null && Vars.net.active()){
            Vars.ui.showInfoToast("Mosestyle Tools time control is available in single-player only.", 3f);
            return false;
        }
        return true;
    }

    private void applyTimeSpeed(){
        float multiplier = timeSpeeds[speedIndex];
        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60f * multiplier, 3f * multiplier));
        if(speedButton != null) speedButton.setText(speedText());
    }

    public void resetTimeSpeed(){
        speedIndex = 0;
        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60f, 3f));
        if(speedButton != null) speedButton.setText(speedText());
    }

    public void onTimeControlEnabledChanged(boolean enabled){
        if(!enabled) resetTimeSpeed();
        rebuildNow();
    }

    public void onOpacityChanged(){
        if(overlayPanel != null){
            overlayPanel.color.a = Core.settings.getInt(hudOpacitySetting, 82) / 100f;
        }
    }

    public void onWidthChanged(){
        panelPositionReady = false;
        rebuildNow();
    }

    private int currentHudWidth(){
        return Math.max(130, Math.min(240, Core.settings.getInt(hudWidthSetting, 160)));
    }

    private String formatAmountColored(int value){
        String amount = UI.formatAmount(value);
        int suffixStart = amount.length();
        while(suffixStart > 0 && Character.isLetter(amount.charAt(suffixStart - 1))){
            suffixStart--;
        }

        if(suffixStart < amount.length()){
            return amount.substring(0, suffixStart) + "[accent]" + amount.substring(suffixStart) + "[]";
        }
        return amount;
    }

    private String speedText(){
        return "[accent]x" + (int)timeSpeeds[speedIndex] + "[]";
    }

    private void updatePanelPlacement(){
        if(overlayPanel == null) return;

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
        overlayPanel.pack();

        boolean portrait = sceneHeight > sceneWidth;
        String orientation = portrait ? "portrait-" : "landscape-";
        float topPadding = Vars.mobile ? 154f : 76f;

        float defaultCenterX = (sceneWidth - 4f - overlayPanel.getWidth() / 2f) / sceneWidth;
        float defaultCenterY = (sceneHeight - topPadding - overlayPanel.getHeight() / 2f) / sceneHeight;

        float centerX = Core.settings.getFloat(hudPositionPrefix + orientation + "x", defaultCenterX);
        float centerY = Core.settings.getFloat(hudPositionPrefix + orientation + "y", defaultCenterY);

        overlayPanel.setPosition(
            centerX * sceneWidth - overlayPanel.getWidth() / 2f,
            centerY * sceneHeight - overlayPanel.getHeight() / 2f
        );
        clampPanelToScreen();
    }

    private void savePanelPosition(){
        if(overlayPanel == null || Core.scene.getWidth() <= 0f || Core.scene.getHeight() <= 0f) return;

        boolean portrait = Core.scene.getHeight() > Core.scene.getWidth();
        String orientation = portrait ? "portrait-" : "landscape-";

        float centerX = (overlayPanel.x + overlayPanel.getWidth() / 2f) / Core.scene.getWidth();
        float centerY = (overlayPanel.y + overlayPanel.getHeight() / 2f) / Core.scene.getHeight();

        Core.settings.put(hudPositionPrefix + orientation + "x", centerX);
        Core.settings.put(hudPositionPrefix + orientation + "y", centerY);
    }

    private void clampPanelToScreen(){
        if(overlayPanel == null || Core.scene == null) return;

        float maxX = Math.max(0f, Core.scene.getWidth() - overlayPanel.getWidth());
        float maxY = Math.max(0f, Core.scene.getHeight() - overlayPanel.getHeight());

        overlayPanel.setPosition(
            Math.max(0f, Math.min(overlayPanel.x, maxX)),
            Math.max(0f, Math.min(overlayPanel.y, maxY))
        );
    }
}
