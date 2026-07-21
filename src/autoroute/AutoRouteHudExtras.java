package autoroute;

import arc.Core;
import arc.Events;
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
 * grid near the upper-right corner. A small arrow-based single-player time
 * control is attached below it and supports x1 through x256.</p>
 */
public class AutoRouteHudExtras{
    public static final String coreItemsSetting = "mindustry-auto-route-core-items";
    public static final String unitsSetting = "mindustry-auto-route-unit-counts";
    public static final String timeControlSetting = "mindustry-auto-route-time-control";
    public static final String hudOpacitySetting = "mindustry-auto-route-hud-opacity";

    private static final float[] timeSpeeds = {1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f};
    private static final float itemCellWidth = 76f;
    private static final float itemCellHeight = 25f;

    private final Interval refreshTimer = new Interval();
    private final Seq<Item> visibleItems = new Seq<>();
    private final Seq<UnitType> visibleUnits = new Seq<>();

    private Table overlayPanel;
    private TextButton speedButton;
    private String lastContentSignature = "";
    private int speedIndex;

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
        Vars.ui.hudGroup.fill(root -> {
            root.top().right();

            overlayPanel = new Table(Styles.black6);
            overlayPanel.margin(3f);
            overlayPanel.visible(this::shouldShowOverlay);
            overlayPanel.update(() -> {
                overlayPanel.color.a = Core.settings.getInt(hudOpacitySetting, 82) / 100f;
                if(refreshTimer.get(30f)) refreshIfNeeded();
            });

            // Keep the panel below Mindustry's upper-right player/status card.
            // It remains clear of the bottom-left Command controls.
            float topPadding = Vars.mobile ? 154f : 76f;
            root.add(overlayPanel).padTop(topPadding).padRight(4f);
        });

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

        boolean addedSection = false;

        if(Core.settings.getBool(coreItemsSetting, true)){
            CoreBuild core = Vars.player == null ? null : Vars.player.team().core();
            if(core != null){
                for(Item item : Vars.content.items()){
                    if(core.items.get(item) > 0) visibleItems.add(item);
                }
            }

            if(!visibleItems.isEmpty()){
                buildItemsSection();
                addedSection = true;
            }
        }

        if(Core.settings.getBool(unitsSetting, true) && Vars.player != null){
            for(UnitType unit : Vars.content.units()){
                if(Vars.player.team().data().countType(unit) > 0) visibleUnits.add(unit);
            }

            if(!visibleUnits.isEmpty()){
                if(addedSection) addDivider();
                buildUnitsSection();
                addedSection = true;
            }
        }

        if(Core.settings.getBool(timeControlSetting, true)){
            if(addedSection) addDivider();
            buildTimeControl();
            addedSection = true;
        }

        overlayPanel.background(addedSection ? Styles.black6 : null);
        overlayPanel.pack();
    }

    private void buildItemsSection(){
        Table grid = new Table();
        grid.defaults().pad(1f);

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
                    return core == null ? "0" : UI.formatAmount(core.items.get(item));
                }).left().minWidth(42f);
            }).width(itemCellWidth).height(itemCellHeight).left();

            if(++index % 2 == 0) grid.row();
        }

        overlayPanel.add(grid).left();
        overlayPanel.row();
    }

    private void buildUnitsSection(){
        Table grid = new Table();
        grid.defaults().pad(1f);

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
                    UI.formatAmount(Vars.player.team().data().countType(unit)))
                    .left().minWidth(42f);
            }).width(itemCellWidth).height(itemCellHeight).left();

            if(++index % 2 == 0) grid.row();
        }

        overlayPanel.add(grid).left();
        overlayPanel.row();
    }

    private void addDivider(){
        overlayPanel.table(line -> line.background(Styles.accentDrawable))
            .height(1f)
            .growX()
            .padTop(2f)
            .padBottom(2f);
        overlayPanel.row();
    }

    private void buildTimeControl(){
        Table controls = new Table();
        controls.defaults().height(32f).pad(1f);

        controls.button(Icon.left, Styles.cleari, this::slower)
            .size(34f);

        speedButton = controls.button(speedText(), Styles.cleart, this::resetTimeSpeed)
            .width(76f)
            .height(32f)
            .get();

        controls.button(Icon.right, Styles.cleari, this::faster)
            .size(34f);

        overlayPanel.add(controls).left();
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
            Vars.ui.showInfoToast("Auto Route time control is available in single-player only.", 3f);
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

    private String speedText(){
        return "[accent]x" + (int)timeSpeeds[speedIndex] + "[]";
    }
}
