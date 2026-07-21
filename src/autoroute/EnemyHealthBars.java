package autoroute;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;

/**
 * Optional enemy-unit health and shield bars.
 *
 * <p>The renderer is intentionally lightweight: it only checks units inside
 * the current camera rectangle and only draws bars for damaged or shielded
 * enemies.</p>
 */
public class EnemyHealthBars{
    public static final String enabledSetting = "mindustry-auto-route-enemy-health-bars";
    public static final String opacitySetting = "mindustry-auto-route-enemy-health-bars-opacity";
    public static final String scaleSetting = "mindustry-auto-route-enemy-health-bars-scale";

    private TextureRegion barRegion;

    public void init(){
        Events.run(EventType.Trigger.draw, this::draw);
    }

    private void draw(){
        if(!Core.settings.getBool(enabledSetting, false)) return;
        if(!Vars.state.isGame() || Vars.player == null || Vars.renderer == null) return;
        if(Vars.ui == null || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) return;

        if(barRegion == null){
            barRegion = Core.atlas.find("white-ui");
            if(barRegion == null || !barRegion.found()) barRegion = Core.atlas.white();
        }

        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float cw = Core.camera.width;
        float ch = Core.camera.height;

        Draw.z(Layer.shields + 5f);
        Groups.unit.intersect(cx - cw / 2f, cy - ch / 2f, cw, ch, this::drawIfNeeded);
        Draw.reset();
    }

    private void drawIfNeeded(Unit unit){
        if(unit == null || !unit.isValid() || unit.team == Vars.player.team()) return;
        if(unit.health >= unit.maxHealth && unit.shield <= 0f) return;

        float opacity = clamp(Core.settings.getInt(opacitySetting, 85) / 100f, 0.35f, 1f);
        float scale = clamp(Core.settings.getInt(scaleSetting, 100) / 100f, 0.6f, 1.6f);

        float maxHealth = Math.max(unit.maxHealth, 1f);
        float healthFraction = clamp(unit.health / maxHealth, 0f, 1f);
        float x = unit.x;
        float y = unit.y + unit.hitSize * 0.8f + 3f * scale;
        float width = Math.max(12f, unit.hitSize * 2.5f * scale);
        float height = Math.max(1.5f, 2f * scale);
        float left = x - width / 2f;

        Draw.color(Color.black, 0.65f * opacity);
        Draw.rect(barRegion, x, y, width + 2f, height + 2f);

        if(healthFraction > 0f){
            float filledWidth = width * healthFraction;
            Draw.color(unit.team.color, 0.9f * opacity);
            Draw.rect(barRegion, left + filledWidth / 2f, y, filledWidth, height);
        }

        if(unit.shield > 0f){
            float shieldFraction = clamp(unit.shield / maxHealth, 0f, 1f);
            float shieldY = y + height * 2f;

            Draw.color(Color.black, 0.65f * opacity);
            Draw.rect(barRegion, x, shieldY, width + 2f, height + 2f);

            float shieldWidth = width * shieldFraction;
            Draw.color(Pal.shield, 0.9f * opacity);
            Draw.rect(barRegion, left + shieldWidth / 2f, shieldY, shieldWidth, height);
        }

    }

    private float clamp(float value, float min, float max){
        return Math.max(min, Math.min(max, value));
    }
}
