# Mosestyle Mindustry Tools

A client-side Mindustry utility suite for Android and desktop by **Mosestyle**. It includes smart Auto Route pathfinding and connected-line upgrades for item and liquid transport, compact movable HUD tools, optional enemy health bars, single-player time control up to x256, and an independent custom music player.

## Install from GitHub

In Mindustry:

1. Open **Mods**.
2. Select **Import Mod**.
3. Select **Import from GitHub**.
4. Paste:

```text
mosestyle/Mosestyle-Mindustry-Tools
```

Repository: https://github.com/mosestyle/Mosestyle-Mindustry-Tools

5. Restart Mindustry when prompted.

## Version 0.9.2 highlights

### Native selection and complete Upgrade Existing Line scanning

The Auto Route panel can be opened without selecting a transport block first. When you do select a Conveyor, Duct, or Conduit, Mindustry's normal yellow build-menu selection border remains visible while Auto Route is open. The mod suppresses ordinary placement only while you touch the map, so waypoint and upgrade taps do not create duplicate vanilla plans.

Upgrade mode accepts the source line and replacement block in either order:

1. Activate Auto Route at any time.
2. Open **Options** and enable **Upgrade line**.
3. Tap any ordinary conveyor, duct, or conduit on the existing line.
4. Select the desired replacement from the normal build menu, such as **Titanium Conveyor**. You may also select the replacement before tapping the line.
5. Review the highlighted replacement preview.
6. Press **Build**.

Selecting an ordinary Conveyor first no longer locks the upgrade target to Conveyor. The tapped tile identifies the source family, while the latest compatible transport selected from the build menu becomes the replacement. You may tap either an older conveyor or a tile that already uses the target tier; mixed-tier lines continue through matching tiles and replace the remaining blocks. Upgrades and downgrades both work.

The scanner now follows the entered lane straight through Junctions and Liquid Junctions without depending on conveyor rotation, and continues through linked bridge pairs, Routers, Sorters, gates, Duct Routers, and compatible one-tile transport connectors. Those special blocks are preserved rather than replaced.

The detector now follows the connected network through linked bridge pairs, straight Junction lanes, routers, sorters, overflow/underflow gates, duct routers, and compatible one-tile transport nodes. These special blocks are preserved rather than replaced. When a router genuinely branches, all attached compatible branches are highlighted so the preview remains transparent before you press **Build**. Factories, drills, cores, and unrelated storage networks are still boundaries.

## Version 0.8.2 highlights

### Custom music player

Open **Settings → Mosestyle Tools** to configure the new custom player:

- **Enable custom music**
- **Import music**
- **Manage music library**
- **Show custom music controls**
- **Shuffle custom music**
- **Repeat custom playlist**
- **Custom music volume**
- **Mute official music automatically**

Supported imported formats are **MP3, OGG Vorbis, WAV, and FLAC**. On Android, **Import music** uses Mindustry's multi-file picker. Selected files are copied into a private Mosestyle library, so they remain available without repeatedly granting document access. On desktop, **Open music folder** opens the private library directory directly.

The movable in-game player contains:

```text
[Move] Current track
[Previous] [Play/Pause] [Next] [Shuffle]
Volume ─────●──── 75%
```

The custom volume is independent from Mindustry's normal soundtrack volume. When **Mute official music automatically** is enabled, the mod temporarily sets the official music volume to zero only while the custom player has an active track, then restores the player's previous official volume when custom playback is disabled or the game exits normally.

The music-library manager can remove individual tracks, refresh after desktop files are copied into the folder, or clear the complete imported library. Undecodable files are skipped automatically instead of stopping the playlist.

## Version 0.7.4 highlights

### Smarter automatic ore and bridge routing

**Ore: automatic fallback** now distinguishes actual ore overlays from broad resource floors. Sand and dark-sand floors no longer make a tiny route wander around the map or construct bridges merely to avoid the floor. Normal conveyors may cross sand directly.

Actual ore overlays such as copper, lead, coal, titanium, thorium, scrap, beryllium, and tungsten remain protected by the ore modes. A protected ore tile also no longer triggers an automatic bridge by itself: bridges are reserved for real structures, multiple transport crossings, or local item-contamination isolation.

### Optional enemy health bars

Enable **Settings → Mosestyle Tools → Enemy health bars** to display compact bars above damaged enemy units currently visible on screen. Shielded enemies receive a second shield bar. The settings page also includes opacity and size controls.

The implementation uses Mindustry's visible-unit group and native drawing APIs.

## Version 0.7.3 highlights

### New project name and GitHub repository

The mod is now publicly named **Mosestyle Mindustry Tools** to reflect its broader routing, HUD, and time-control features. The Auto Route system remains the main routing feature, and the internal mod ID and saved-setting keys remain unchanged so existing preferences continue working.

### Cleaner movable HUD header

The text label **Items / units / time** has been removed from the movable HUD. The top row now contains only the four-way move handle, making the resources, units, and time panel shorter and less visually busy on Android.

### Isolated Item Bridge exits

Mindustry's normal Item Bridge destination can dump items to several adjacent sides. Auto Route now checks those sides before selecting the bridge exit. If an unrelated conveyor, router, storage block, or other item receiver could receive the bridged material, that endpoint is rejected and the router searches for the next isolated endpoint. This keeps parallel lead, copper, and other resource lines separate while still using bridges where appropriate.

### Movable and resizable items/units/time HUD

The combined upper-right HUD now has a four-way move handle. Drag it anywhere on Android or desktop; portrait and landscape positions are remembered separately. **Settings → Mosestyle Tools → Items, units & time HUD width** adjusts the panel from narrow to wide, including the two-column item/unit grid and time-control row. Compact suffixes such as `k` use the orange accent color for better visibility.

### Mosestyle Tools settings page

Mindustry's main **Settings** menu now contains a **Mosestyle Tools** category. It includes:

- **Compact route panel** — enabled by default; disable it for a wider single-column option list.
- **Core items display** — shows the resources stored in your team's core.
- **Unit counter display** — shows friendly unit types and their current counts.
- **Time control** — enables the compact single-player speed control.
- **Items, units & time HUD opacity** — adjusts the upper-right panel transparency.
- **Items, units & time HUD width** — makes that movable panel narrower or wider.
- **Enemy health bars** — shows health and shield bars above damaged enemy units.
- **Enemy health-bar opacity** — adjusts bar transparency.
- **Enemy health-bar size** — adjusts bar dimensions.

These options are kept out of the in-game routing panel, so enabling extra HUD tools does not make the route controls larger.

### Compact upper-right items and units HUD

Core resources and friendly units are displayed in one small upper-right panel:

- two entries per row;
- item or unit icon followed by its compact amount;
- only resources and unit types currently present are shown;
- item and unit sections can be enabled independently;
- the panel hides with the normal HUD and minimap;
- adjustable opacity through **Settings → Mosestyle Tools**.

If Mindustry's normal desktop **Display Core Items** option is also enabled, disable either that option or Mosestyle Mindustry Tools' core-items display to avoid duplicate information.

### Better Vanilla-style time control up to x256

A compact arrow control appears below the items/units panel:

```text
[←]  x1  [→]
```

The right arrow cycles upward through:

```text
x1 → x2 → x4 → x8 → x16 → x32 → x64 → x128 → x256
```

The left arrow steps back down, and tapping the speed label resets to `x1`. It is positioned near the upper-right instead of the bottom-left, so it does not cover the mobile Command interface.

Time scaling is intentionally limited to single-player. Attempting to change it while connected to multiplayer shows a warning instead of risking simulation desynchronization.

### Compact and full route-panel layouts

The improved two-column route panel remains the default. Players who prefer the older list layout can disable **Compact route panel** in **Settings → Mosestyle Tools**. The full version uses a wider, left-aligned single-column list while keeping the collapse and semi-transparent behavior.

### Local contamination bridges

Item routes may continue running close to existing conveyors and buildings. When only a small section could receive unwanted items from a drill, router, sorter, unloader, smelter, press, or other item-output block, Auto Route now isolates only that section:

1. keep normal conveyors on the safe tiles before the danger area;
2. place the shortest valid bridge across the affected tile or tiles;
3. return to normal conveyors immediately after the danger area.

The bridge endpoints must remain on safe tiles. A single clean perpendicular conveyor crossing still uses a **Junction**, not a bridge. Wider detours are now fallback behavior when no valid local bridge is available or automatic bridges are disabled.

### Compact collapsible panel

The resource-cost preview remains removed to keep the panel small on phones.

The routing panel has a **- / +** control in its header:

- tap **-** to collapse the full menu into a compact bar;
- the compact bar keeps the move handle, route status, **Build**, expand, and close controls;
- tap **+** to restore Undo, Clear, Options, Edit route, Bridges, and Forbidden tiles;
- the panel is semi-transparent while idle so the map remains visible underneath;
- touching or hovering the panel temporarily restores full opacity;
- the collapsed state is remembered between game launches.

### Liquid conduit routing

Auto Route now supports Mindustry's normal 1x1 liquid transport lines in addition to item conveyors and ducts:

- Conduit, Pulse Conduit, and Plated Conduit;
- Reinforced Conduit and compatible modded Conduit subclasses;
- automatic Liquid Junction crossings;
- Bridge Conduit or the selected conduit family's official bridge replacement;
- intentional connections to built or planned compatible conduits;
- the same ore, obstacle, forbidden-tile, route-preference, bridge, and build-queue rules used by item routes.

Select the conduit normally, then tap the Auto Route icon and place waypoints exactly as you do with conveyors.

### Route editing

Open **Options → Edit route** after adding one or more waypoints:

1. Tap one of the highlighted waypoints.
2. Tap its new tile.
3. Auto Route recalculates the affected route while preserving all other waypoints.

If the moved point cannot produce a valid route, its previous position and route are restored automatically. Tap the selected waypoint again to cancel the selection, or turn **Edit route** off to resume adding new waypoints.

## Basic use

1. Select a normal 1x1 supported transport block:
   - Conveyor family;
   - Duct family;
   - Conduit family.
2. Tap the Auto Route icon at the top-right of the HUD.
3. Tap Point A and Point B.
4. Add extra waypoints to control the general shape.
5. Review the route preview.
6. Tap **Build** to add the plans to Mindustry's normal construction queue.

Use **Undo** to remove the newest waypoint and **Clear** to reset the route. Hold the four-way handle to move the panel. Portrait and landscape positions are remembered separately.

Tap **-** to collapse the panel into a compact semi-transparent bar. The compact bar still includes **Build**. Tap **+** to expand it again.

## Automatic Junctions and bridges

- A single necessary 90-degree crossing of a compatible item line becomes a **Junction**.
- A single necessary 90-degree crossing of a compatible liquid line becomes a **Liquid Junction**.
- Empty ground and Junction crossings are preferred over bridges.
- Bridges are used for hard obstacles, crowded multi-line crossings, or the shortest local isolation span beside an unintended item output.
- Auto Route uses each selected transport block's official Junction and bridge replacements and normal in-game range.
- Both bridge endpoints are validated and queued together.

Use **Options → Bridges** to enable or disable automatic bridges.

## Ore modes

Tap the ore option to cycle through:

- **Ore: automatic fallback** — first searches for a route that places no normal transport block on ore; retries with a large ore penalty only when necessary.
- **Ore: never cross** — no automatically selected intermediate transport block may be placed on ore.
- **Ore: allow with penalty** — ore may be used, but clean ground is strongly preferred.

Explicit waypoints may still be placed on ore. A bridge may pass above ore because it does not place intermediate blocks on those resource tiles.

## Route preferences

Tap the route option to cycle through:

- **Shortest** — prioritizes total path length.
- **Straightest** — accepts a somewhat longer route to reduce turns.
- **Least interference** — avoids accidental crossings, merges, and planned structures, but may still run parallel beside nearby lines. This is the default.

## Intentional connections

Tap an existing friendly compatible transport block, or one already in your local construction queue, as Point A or Point B to explicitly connect to it.

- A starting transport block must point in the route's outgoing direction.
- Standard conveyors, ducts, and conduits may accept valid rear or side connections.
- Armored/plated transports that reject side input must be approached from the valid rear direction.
- The existing endpoint is kept rather than replaced with a duplicate plan.

Automatically chosen intermediate tiles still avoid accidental merges.

## Forbidden tiles

Open **Options** and enable **Forbidden tiles**:

- **Tap-to-tap lines:** tap Point A, then Point B; every tile between them is marked. Further taps continue the chain.
- **Android freehand:** hold one finger still for about 350 ms, then drag to mark or erase.
- **Android map movement:** quick-drag pans the map while Forbidden mode stays enabled.
- **Desktop freehand:** click and drag immediately.

Use **Draw: mark / Draw: erase**, **New line**, and **Reset** as needed. Forbidden tiles block both ground placement and bridge spans for the current loaded world session.

## Existing build-plan awareness

Auto Route reads the local player's committed construction queue:

- queued structures are treated as occupied;
- compatible planned transports may be intentional endpoints;
- unrelated plans are not overwritten;
- if the queue changes after previewing, Auto Route recalculates and asks you to review the updated route;
- route commits are all-or-nothing, including bridge pairs.

## Item-output safety

Automatically selected ordinary conveyors and ducts are not placed on tiles where an unrelated drill, router, distribution block, unloader, smelter, press, or item-producing factory could feed into the route.

With automatic bridges available, Auto Route treats these tiles as a **local isolation zone** rather than avoiding the entire neighborhood. It prefers the shortest bridge with safe endpoints, then resumes normal ground routing. Nearby parallel conveyors and unrelated safe tiles remain usable.

Explicit waypoints beside these buildings remain allowed for intentional connections. Liquid conduits do not need this item-contamination restriction.

## Performance protection

- Android segments are limited to 500 Manhattan tiles; desktop allows 1000.
- Searches stop at platform-specific node and time limits.
- A bounded second attempt handles rare mobile timing spikes.
- Recent path results are cached.
- Very long routes ask for an intermediate waypoint.

These limits apply per waypoint segment, so very long routes remain possible with several waypoints.

## Automatic GitHub release

The included workflow builds one Android-and-desktop-compatible JAR and publishes it from `main`.

1. Replace your repository files with this project's contents.
2. Commit and push to `main`.
3. Open **Actions** and wait for the build to finish.
4. The workflow creates tag `v0.8.2`, creates the GitHub Release, and attaches `MosestyleMindustryTools.jar`.

For later releases, increase the version in both `mod.hjson` and `build.gradle` before pushing.

## Local build

Requirements:

- JDK 17
- Gradle
- Android SDK with `d8` available on `PATH`

Run:

```bash
gradle deploy
```

Output:

```text
build/libs/MosestyleMindustryTools.jar
```

## Compatibility

- Mindustry v159+
- Android and desktop
- Client-side mod

## Current limitations

- Custom music currently supports MP3, OGG Vorbis, WAV, and FLAC. AAC, M4A, and WMA are not included in the cross-platform player.
- Only ordinary 1x1 Conveyor, Duct, and Conduit families are supported.
- Automatic bridge support depends on the selected block exposing an unlocked official bridge replacement.
- Auto Route does not place sorters, overflow gates, liquid routers, unloaders, or item routers because these alter distribution behavior rather than simply preserving a line.
- Forbidden tiles are currently session-based and clear when the world changes.
- Multiplayer servers may impose block bans or placement limits; final construction remains subject to normal Mindustry validation.

## Credits

The optional HUD concepts were inspired by the open-source **Better Vanilla** core/unit display and time-control UI, and by the original open-source **TimeControl** mod. Mosestyle Mindustry Tools uses its own compact two-column layout and single-player implementation.


## License

MIT License. See `LICENSE`.
