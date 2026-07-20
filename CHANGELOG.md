# Changelog

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
