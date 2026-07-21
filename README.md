# Mindustry Auto Route

A client-side Mindustry mod for Android and desktop. Tap a few waypoints and Auto Route calculates a safe item or liquid transport path, previews it, then adds normal Mindustry build plans.

## Install from GitHub

In Mindustry:

1. Open **Mods**.
2. Select **Import Mod**.
3. Select **Import from GitHub**.
4. Paste:

```text
https://github.com/mosestyle/Mindustry-Auto-Route
```

5. Restart Mindustry when prompted.

## Version 0.6.1 highlights

### Compact collapsible panel

The resource-cost preview has been removed to keep the panel small on phones.

The routing panel now has a **- / +** control in its header:

- tap **-** to collapse the full menu into a compact bar;
- the compact bar keeps the move handle, route status, **Build**, expand, and close controls;
- tap **+** to restore Undo, Clear, Options, Edit route, Bridges, and Forbidden tiles;
- the panel is semi-transparent while idle so the map remains visible underneath;
- touching or hovering the panel temporarily restores full opacity;
- the collapsed state is remembered between game launches.

### Safer item-output avoidance

Item routes now avoid ordinary conveyor tiles beside unintended item-output buildings, including:

- drills;
- routers, sorters, overflow/underflow gates, and unloaders;
- item-producing factories;
- compatible modded distribution or factory blocks.

An explicit Point A or Point B may still be placed beside one of these blocks when the connection is intentional. With automatic bridges enabled, the router may bridge across the unsafe output area or choose a clean detour, while keeping the bridge endpoints outside the contamination zone.

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
- Bridges are reserved for hard obstacles, crowded multi-line crossings, or spans where a Junction cannot solve the route.
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
- **Least interference** — strongly avoids existing lines, planned structures, and crowded building edges. This is the default.

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

Automatically selected item conveyors and ducts are not placed directly beside unintended output buildings such as drills, routers, distribution gates, unloaders, and item-producing factories. This prevents unrelated items from leaking into the new line.

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
4. The workflow creates tag `v0.6.1`, creates the GitHub Release, and attaches `MindustryAutoRoute.jar`.

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
build/libs/MindustryAutoRoute.jar
```

## Compatibility

- Mindustry v159+
- Android and desktop
- Client-side mod

## Current limitations

- Only ordinary 1x1 Conveyor, Duct, and Conduit families are supported.
- Automatic bridge support depends on the selected block exposing an unlocked official bridge replacement.
- Auto Route does not place sorters, overflow gates, liquid routers, unloaders, or item routers because these alter distribution behavior rather than simply preserving a line.
- Forbidden tiles are currently session-based and clear when the world changes.
- Multiplayer servers may impose block bans or placement limits; final construction remains subject to normal Mindustry validation.

## License

MIT License. See `LICENSE`.
