# Changelog

## 0.9.8

- Restored the fast v0.9.6 single-route calculation path for normal Auto Route building. Point B now calculates only the currently selected Short, Straight, or Clean preference instead of running all three pathfinders.
- Removed the experimental alternative-route arrow selector and its repeated alternative refreshes, eliminating the largest new source of long-route pauses on Android.
- Kept every v0.9.7 custom music-player improvement unchanged: Play/Pause and Shuffle on the first row, Previous and Next on the second row, and extra vertical spacing above the volume slider.
- Kept all v0.9.4 multi-line upgrade, consecutive-Junction traversal, safe branch handling, and v0.9.3 smart Junction-crossing improvements.

## 0.9.7

- Added **alternative route previews** for normal Auto Route building. After Point B is selected, the mod calculates the current route preference plus the other Short, Straight, and Clean scoring modes.
- Added compact left/right preview arrows directly below Undo, Clear, and Build. The label shows the active preview, such as **Route 1/3 • Least interference**.
- Cycling the arrows changes only the preview; nothing is queued until **Build** is pressed. The selected preview also updates the normal Route preference setting.
- Deduplicated identical results, so the selector only shows alternatives that genuinely change the route geometry or bridge layout.
- Recalculates alternatives after waypoint moves, forbidden-tile edits, bridge/ore option changes, build-queue changes, and Undo operations.
- Added an **R1/3**-style indicator to the compact/collapsed route status when multiple previews are available.
- Reordered the extra-compact music controls to **Play/Pause + Shuffle** on the first row and **Previous + Next** on the second row.
- Increased the extra-compact controls area slightly and added bottom spacing so the volume slider no longer overlaps the Next button.

## 0.9.6

- Added an **Extra-compact 2x2 music controls** setting, enabled by default, which arranges Previous and Play/Pause above Next and Shuffle.
- Reduced the extra-compact player content width from 160 to 112 UI units so it fits more comfortably along the right or left edge of portrait Android screens.
- Kept the v0.9.5 single-row player as an optional layout; disabling the new setting restores it immediately.
- Rebuilds and clamps the movable player safely when switching layouts while retaining the saved portrait/landscape center position.
- Kept true title ellipsis, the shuffle icon, independent volume, and all existing playback behavior in both layouts.

## 0.9.5

- Made the movable custom music player substantially narrower so it fits more comfortably along the right side of portrait Android screens.
- Replaced the wide **Shuffle** text button with a compact dedicated shuffle icon while preserving its toggle state.
- Added true label ellipsis handling for long song names, keeping all track text clipped inside the player overlay.
- Removed the unnecessary **Vol** text and tightened the previous, play/pause, next, shuffle, slider, and percentage spacing.
- Kept the existing four-way drag handle, independent portrait/landscape positions, playback controls, and custom volume behavior.

## 0.9.4

- Reworked Upgrade Existing Line into an additive multi-line selection mode: tap several conveyor, duct, or conduit lines and press Build once to queue every deduplicated replacement.
- Tapping any transport tile already covered by a selected line removes that selection; Undo removes only the most recently added line, while Clear removes all upgrade selections.
- Added direction-aware Junction and Liquid Junction traversal. Consecutive Junction chains are followed straight through without converting or redesigning the player's special blocks.
- Tracks horizontal and vertical Junction lanes independently, preventing a perpendicular crossing lane from blocking the selected lane during a connected-line scan.
- Continues through linked Bridge Conveyor, Duct Bridge, and Bridge Conduit endpoints before or after consecutive Junctions.
- Added safe connector branching: routers and other transport connectors with one clear continuation are followed automatically, while genuinely ambiguous branches are highlighted and left untouched until the player taps the desired continuation as another selection.
- Deduplicated overlapping replacement plans, preserved-special highlights, and ambiguous-branch markers across all selected lines.
- Added combined status and build messages showing selected line count, total replacements, preserved special blocks, and unresolved branching connectors.

## 0.9.3

- Added Junction-first handling when a later waypoint segment crosses an earlier segment of the same Auto Route preview.
- A single perpendicular self-crossing on an empty tile now creates one Junction/Liquid Junction plan instead of a two-endpoint bridge.
- Deduplicated build plans at self-crossing coordinates so only one valid block is queued on the shared tile.
- Enforced straight-through movement on planned Junction crossings and gave safe self-crossings a small cost so compact Junctions are preferred over wide detours.
- Kept automatic bridges for real structures, local item-isolation zones, and spans crossing multiple transport lines.
- Continued using Mindustry's normal Junction replacement for crossings over already-built conveyor, duct, and conduit lines.

## 0.9.2

- Kept Mindustry's normal selected transport block active while Auto Route is open, restoring the yellow build-menu selection border.
- Added a world-touch placement guard that temporarily suppresses vanilla placement only during map gestures, preventing duplicate blocks while preserving normal build-menu selection.
- Removed the unnecessary "Auto Route is open..." notification when opening the panel without a selected transport block.
- Fixed connected-line upgrade scans through standard Junction and Liquid Junction blocks, including mixed-tier or reverse-flow lanes.
- Junction scans now continue through the opposite side based on the entered lane instead of incorrectly rejecting valid lines because of conveyor rotation.
- Retained and highlighted routers, linked bridge pairs, junctions, sorters, gates, and compatible transport connectors while following the line beyond them.

## 0.9.1

- Auto Route can now be opened without first selecting a conveyor, duct, or conduit.
- Upgrade Existing Line now accepts the source line and replacement block in either order.
- Tapping a tile that already uses the target tier no longer blocks the scan; mixed-tier lines continue through that tile and replace the remaining older or different tiers.
- The active replacement automatically updates when another compatible transport block is selected from the normal build menu while the panel is open.
- Separated the tapped source family from the replacement target, eliminating stale selection behavior that previously made identical attempts work only after closing and reopening the panel.
- Extended connected-line scanning through routers, sorters, overflow/underflow gates, duct routers, compatible one-tile transport nodes, Junction lanes, and linked bridges.
- Preserved special transport blocks while continuing the scan to compatible conveyor, duct, or conduit sections after them.
- Mixed-tier lines can now be scanned as one network: blocks already matching the target are skipped but still connect the remaining replacements.
- Kept mobile and desktop scan safety limits and all-at-once build validation.

## 0.9.0

- Added **Upgrade existing line** mode for conveyor, duct, and conduit families.
- Select the desired replacement block, enable Upgrade line in Auto Route options, then tap any existing compatible transport tile to identify and preview that connected lane.
- Supports upgrades and downgrades, such as Conveyor → Titanium Conveyor or Titanium Conveyor → Conveyor.
- Preserves routers, sorters, factories, cores, Junctions, Liquid Junctions, and bridge endpoints instead of replacing them.
- Continues line detection straight through compatible Junction crossings and linked bridge pairs while preventing the perpendicular crossing lane from being absorbed.
- Treats routers and other branching distribution blocks as boundaries, preventing separate copper and lead networks from being selected together through a shared router.
- Added a mobile safety limit, highlighted preserved special blocks, all-at-once build validation, and automatic preview refresh when the selected line changes.

## 0.8.5

- Reworked the Custom Music Library action buttons after the one-line labels caused icon/text overlap on narrow Android screens.
- Widened the mobile action buttons from 136 to 158 UI units and increased their height slightly.
- Restored intentional two-line labels for **Import music** and **Remove all**, while keeping **Refresh** and **Back** on one line.
- Increased spacing between library action buttons for a cleaner portrait layout.

## 0.8.4

- Fixed the Custom Music Library action labels wrapping vertically on narrow Android screens.
- Gave Import music, Refresh, Remove all, Open folder, and Back fixed one-line button widths.
- Reorganized the library actions into a predictable two-column mobile-friendly layout.

## 0.8.3

- Preserved Android document display names when importing music, so new tracks use their real filenames instead of generic `track-1` names.
- Added a Play button beside every track in the Custom Music Library for directly selecting and starting that song.
- Added a dedicated Android multi-file picker path that reads every selected `ClipData` URI, fixing providers where selecting several songs previously returned no imports.
- Deduplicated Android URI results and retained the existing MP3, OGG, WAV, and FLAC signature checks and private-library copying.

## 0.8.2

- Fixed Android music imports appearing to complete while leaving the custom library empty.
- Android document-picker selections may use opaque content-URI paths with no usable filename extension; imports now detect MP3, OGG, WAV, and FLAC from the file signature when necessary.
- Replaced the generic `copyTo(...)` import path with direct stream copying into the private Mosestyle music folder.
- Added copy verification so empty or failed files are removed instead of silently appearing successful.
- Added clearer import results for imported, unsupported, and failed files.

## 0.8.1

- Fixed the custom music settings build failure against Mindustry v159.7.
- Updated custom section and action preferences to extend `SettingsMenuDialog.SettingsTable.Setting`, the correct nested settings base class.

## 0.8.0

- Added a cross-platform custom music player with its own independent volume.
- Added multi-file import for MP3, OGG, WAV, and FLAC tracks. Imported songs are copied into a private Mosestyle music library under Mindustry's data directory.
- Added a movable in-game player with previous, play/pause, next, shuffle, track name, and volume controls.
- Added shuffle and repeat-playlist settings.
- Added optional automatic muting of Mindustry's official soundtrack while custom music is playing, with restoration of the previous official music volume.
- Added a music-library manager for removing individual tracks or clearing all imported music.
- Added a desktop-only button for opening the private music folder directly, plus a library refresh action after files are copied into it.
- Added automatic skipping when an imported track cannot be decoded.

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
