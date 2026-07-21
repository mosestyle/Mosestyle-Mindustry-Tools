# Changelog

## 0.5.0

- Improved the routing panel hierarchy with orange accent labels for Options, Ore, Route, Bridges, Forbidden tiles, and Draw mode.
- Simplified the collapsed portrait toolbar to a compact **Options +** label so it no longer clips on narrow phone screens.
- Added Android navigation while Forbidden tiles mode is active: quick-drag pans the map, while holding for 350 ms and then dragging draws or erases forbidden tiles.
- Kept immediate click-and-drag forbidden drawing on desktop.
- Added automatic portrait toast wrapping, including sentence-aware line breaks for instructions and error messages.
- Fixed intermittent first-attempt route failures caused by stale vanilla `linePlans` being mistaken for occupied build plans.
- Stopped treating temporary `linePlans` and `selectPlans` previews as committed construction; the real player build queue remains protected.
- Added one bounded automatic pathfinding retry after a timeout or expansion-limit spike on reasonably short segments.

## 0.4.2

- Fixed automatic bridges replacing simple one-tile perpendicular conveyor crossings.
- Restored Junction-first behavior for ordinary conveyor and duct crossings.
- Existing conveyors are now evaluated as possible Junction crossings before the unrelated-line feed protection is applied.
- Bridges remain available for hard obstacles and multi-crossing spans where a Junction cannot solve the route cleanly.

## 0.4.1

- Added freehand forbidden-tile drawing on Android and desktop: hold and drag to mark every crossed tile without gaps.
- Added waypoint-style forbidden lines: tap Point A, then Point B, C, and more to draw connected straight or diagonal forbidden segments.
- Added **Draw: mark / Draw: erase** so large areas can be corrected with the same drag and line gestures.
- Added **New line** to end the current point chain and begin another shape elsewhere.
- Added a highlighted anchor tile showing the current start of the next forbidden line.
- Batched forbidden edits so route recalculation runs once per drag stroke or completed line instead of once per tile.
- Forbidden drawing now consumes the map gesture, preventing the camera from panning while the player is painting tiles.

## 0.4.0

- Added automatic real bridge placement for Conveyor and Duct families using Mindustry's official bridge replacements and range rules.
- Kept Junction as the preferred solution for one perpendicular crossing; bridges can handle hard obstacles or clusters of multiple conveyor crossings.
- Added a visible bridge-span preview and preserved Item Bridge link configuration in queued plans.
- Added default automatic ore fallback: first search without ore, then retry with a strong ore penalty only when necessary.
- Added three ore modes: automatic fallback, never cross, and allow with penalty.
- Added player-marked forbidden tiles with red map overlays and a reset control. Forbidden tiles also block bridge airspace.
- Added awareness of the player's existing build queue and current placement previews. Planned structures are not overwritten.
- Added queue-change detection before Build; changed queues cause route recalculation and require a second confirmation.
- Added route preferences for shortest, straightest, and least interference.
- Added explicit connections to existing or planned compatible conveyor/duct endpoints.
- Added direction-aware endpoint pathfinding: starts follow their output direction, armored endpoints require rear input, and ordinary conveyors/ducts avoid invalid head-on input while still allowing valid side approaches.
- Added mobile-safe maximum segment distance, node limits, time budgets, and a small path-result cache.
- Made route commits all-or-nothing so bridge pairs and other routes cannot be partially queued after a map change.
- Prevented automatic bridge jumps from skipping across a requested waypoint.
- Hardened obstacle handling so unrelated built transportation blocks, factories, pipes, and structures are never silently replaced.
- Added output-cell avoidance for built and planned conveyor/duct lines to prevent accidental item merging.
- Retained automatic Junction crossings, ore/resource awareness, drill-output avoidance, movable UI, and AutoDrill-aligned activation icon.

## 0.3.0

- Matched AutoDrill's exact top-right HUD table structure, 30px icon size, and 155px right margin, with Auto Route placed on the row directly below it.
- Added smart existing-conveyor crossing for Conveyor-family blocks.
- Automatically replaces perpendicular crossing tiles with the selected conveyor's unlocked junction replacement.
- Prevents routes from running parallel over existing conveyors, avoiding accidental replacement and merging.
- Adds a pathfinding penalty to conveyor crossings so empty ground is preferred and junctions are used only when worthwhile.
- Shows the number of automatic junctions in the route preview and build confirmation.

## 0.2.2

- Adjusted the Auto Route activation button so it sits in a cleaner top-right column directly below AutoDrill-style mod icons.
- Increased the activation button slot size for better visual alignment with neighboring HUD icon mods on mobile.
- Replaced the toolbar drag-handle art with a cleaner four-way move icon that better matches the common Mindustry Tool style.

## 0.2.1

- Fixed the movable toolbar compilation error against Mindustry/Arc v159.7.
- Replaced invalid no-argument `Element.getX()` / `getY()` calls with the supported element position fields.
- Simplified toolbar dragging with `moveBy(...)`.

## 0.2.0

- Replaced the large **Auto Route** text button with the mod icon.
- Moved the activation icon to the top-right HUD and one row below AutoDrill's icon position.
- Added a compact movable routing panel with a four-way drag handle.
- Saved separate panel positions for portrait and landscape orientation.
- Reduced the routing panel footprint on narrow Android screens.
- Added smart drill-output avoidance: automatically selected intermediate tiles no longer run directly beside built-in or compatible modded drills.
- Explicit waypoints beside drills are still allowed for intentional connections.

## 0.1.0

- Initial beta release.
- Waypoint-based A* routing for 1x1 conveyor-style blocks.
- Ore avoidance/allow mode.
- Route preview, undo, clear, cancel, and build controls.
