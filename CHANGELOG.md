# Changelog

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
