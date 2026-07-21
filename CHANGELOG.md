# Changelog

## 0.7.5

- Removed the unnecessary third-party notices file and external health-bar attribution references.
- Kept the enemy health-bar renderer as Mosestyle Mindustry Tools' independent implementation.

## 0.7.4

- Fixed **Ore: automatic fallback** treating large floor resources such as sand like protected ore overlays. Sand floors no longer force huge detours or unnecessary bridges.
- Automatic bridges are no longer triggered solely to avoid an ore tile; bridges remain reserved for real obstacles, multi-line crossings, and local item-isolation zones.
- Ore avoidance continues to protect actual ore overlays such as copper, lead, coal, titanium, thorium, scrap, beryllium, and tungsten.
- Added optional damaged-enemy health and shield bars.
- Added health-bar enable, opacity, and size controls under **Settings → Mosestyle Tools**.

## 0.7.3

- Renamed the visible mod from **Auto Route** to **Mosestyle Mindustry Tools**.
- Updated the Mindustry Settings category to **Mosestyle Tools**.
- Updated GitHub installation instructions to `mosestyle/Mosestyle-Mindustry-Tools`.
- Updated the repository URL to `https://github.com/mosestyle/Mosestyle-Mindustry-Tools`.
- Renamed Gradle, workflow, release, and downloadable JAR branding to `MosestyleMindustryTools`.
- Preserved the internal mod ID, Java package, texture names, and saved-setting keys for upgrade compatibility.

## 0.7.2

- Removed the **Items / units / time** title from the movable HUD.
- Kept only the four-way move handle in the top row.
- Removed the now-unnecessary divider beneath the title row, making the panel shorter and cleaner on Android.

## 0.7.1

- Prevented Item Bridge exits from dumping one resource into a neighbouring parallel item line. Unsafe destination endpoints are skipped so the router extends, shifts, or reroutes the bridge to an isolated exit.
- Added a four-way move handle to the combined core-items, unit-count, and time-control HUD.
- Saved separate HUD positions for portrait and landscape layouts.
- Added an **Items, units & time HUD width** slider in **Settings → Auto Route**.
- Colored compact amount suffixes such as `k` with Mindustry's accent color for improved contrast.

## 0.7.0

- Added an **Auto Route** category to Mindustry's main Settings menu.
- Added independent settings for the compact route panel, core-items HUD, unit-count HUD, and time control.
- Added a HUD opacity slider for the combined items, units, and time panel.
- Added a compact upper-right core-items display with two entries per row.
- Added a compact friendly-unit counter using the same two-column layout.
- Added Better Vanilla-style left/right time controls with x1, x2, x4, x8, x16, x32, x64, x128, and x256 speeds.
- Kept time control single-player only to avoid multiplayer desynchronization.
- Added an optional full single-column route-panel layout while retaining compact mode as the default.
- Added setting names and descriptions through the mod bundle.

## 0.6.4

- Widened the compact Android panel so action and option labels no longer wrap unnecessarily.
- Increased compact row heights and spacing for clearer touch targets and improved text readability.
- Gave Undo, Clear, and Build fixed equal widths so `Clear` remains on one line.
- Added extra padding around toggle buttons so the yellow selected border no longer crowds the Bridge text.
- Kept the two-column compact layout; a future setting can offer compact and full single-column modes.

## 0.6.3

- Redesigned the expanded routing panel into a compact two-column mobile layout.
- Reduced portrait width, margins, button heights, and header size.
- Moved Build into the main action row in expanded mode to make the header narrower.
- Shortened option labels while preserving orange category accents.
- Added compact route status text for points, tiles, Junctions, bridges, ore fallback, and intentional links.
- Added automatic panel relayout when rotating between portrait and landscape.
- Preserved the collapsible bar and semi-transparent idle behavior.

## 0.6.2

- Reworked item-output protection into localized isolation bridges.
- Routes may still run close to existing conveyors and structures when the nearby tiles are otherwise safe.
- Tiles that could receive unintended items from drills, routers, sorters, unloaders, smelters, presses, factories, or existing transport outputs are classified separately from hard obstacles.
- Automatic bridge search now chooses the first safe endpoint after the contamination zone and adds a small span-length cost so the shortest valid bridge wins.
- Single perpendicular conveyor crossings continue to prefer Junctions.
- Clean detours are now fallback behavior when a local bridge is unavailable or automatic bridges are disabled.

## 0.6.1

- Removed the live resource-cost preview to reduce the mobile panel footprint.
- Added a collapsible panel with a compact bar that keeps route status and Build available.
- Added semi-transparent idle panel rendering; touching or hovering restores full opacity.
- Saved the collapsed/expanded panel state between launches.
- Generalized drill safety into item-output safety for routers, distribution gates, unloaders, factories, and compatible modded blocks.
- Automatic routes now detour or use a bridge across unsafe output-adjacent tiles, while explicit endpoint connections remain possible.

## 0.6.0

- Added a live build and resource-cost preview before committing a route.
- The preview separates normal transport blocks, Junction/Liquid Junction replacements, bridge spans, and bridge endpoints.
- Added total item requirements from every generated build plan.
- Added liquid Conduit routing for Serpulo and Erekir conduit families and compatible modded Conduit subclasses.
- Added automatic Liquid Junction crossings and official conduit bridge replacement support.
- Added intentional connections to built or planned compatible liquid conduits.
- Added route-edit mode: select a highlighted waypoint and tap a new tile to move it without rebuilding the route manually.
- Failed waypoint moves restore the previous point and route safely.
- Updated README installation instructions to use the public Mosestyle repository URL near the top.

## 0.5.0

- Improved mobile GUI styling and portrait notification wrapping.
- Added quick-drag map movement and hold-to-draw behavior in Forbidden mode.
- Added a bounded path-search retry to reduce intermittent first-attempt failures.

## 0.4.2

- Restored Junction-first behavior for single perpendicular conveyor crossings.
- Bridges are reserved for hard obstacles or crowded multi-line spans.

## 0.4.1

- Added tap-to-tap and freehand forbidden-tile drawing and erasing.

## 0.4.0

- Added automatic bridges, ore fallback, forbidden tiles, route preferences, build-queue awareness, intentional connections, and mobile performance protection.

## 0.3.0

- Added automatic Junction crossings and aligned the HUD icon below AutoDrill.

## 0.2.x

- Added icon-based activation, movable mobile controls, and drill-output avoidance.

## 0.1.0

- Initial waypoint-based A* conveyor routing beta.
