# Changelog

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
