# Mindustry Auto Route

A client-side Mindustry mod for Android and desktop. Tap a few waypoints and Auto Route calculates a safe conveyor or duct path, previews it, then adds normal Mindustry build plans.

## Version 0.4.0 highlights

Version 0.4.0 implements the seven major routing upgrades requested after v0.3.0:

1. **Automatic bridges** over real obstacles and crowded conveyor crossings.
2. **Automatic ore fallback** when no ore-free route exists.
3. **Forbidden tiles** that the router must not use or bridge across.
4. **Build-queue awareness** for already planned, unbuilt structures.
5. **Route preferences** for shortest, straightest, or least-interference paths.
6. **Intentional connections** to existing or planned conveyor/duct endpoints.
7. **Mobile performance protection** with distance, time, node, and cache limits.

The v0.3 features remain included: automatic junction crossings, drill-output avoidance, obstacle avoidance, movable controls, separate portrait/landscape panel positions, and the AutoDrill-aligned HUD icon.

## Basic use

1. Select a normal 1x1 conveyor-style block, such as Conveyor, Armored Conveyor, Duct, or Armored Duct.
2. Tap the Auto Route icon at the top-right of the HUD.
3. Tap Point A and Point B.
4. Add extra waypoints when you want to control the general shape.
5. Review the complete preview.
6. Tap **Build** to add the plans to Mindustry's normal construction queue.

The route remains editable with **Undo** and **Clear**. Hold the four-way handle to move the panel. Portrait and landscape positions are remembered separately.

## Automatic bridges

Yes, the bridges are real Mindustry bridge blocks rather than a visual trick.

Auto Route uses the selected conveyor or duct's official bridge replacement and its actual range. It places two valid bridge endpoints and preserves the link/configuration in the build queue.

A bridge can be chosen when:

- a wall, pipe, bridge endpoint, factory, small structure, invalid tile, or other hard obstacle blocks a straight section;
- multiple conveyor lines would otherwise require several junction crossings;
- an ore patch can be crossed without placing normal conveyors directly on its resource tiles.

A single perpendicular conveyor crossing still prefers a **Junction**. For a cluster of two or more crossings, a bridge becomes a candidate. The chosen bridge must be researched/unlocked, both endpoints must be placeable, and the span must fit within its normal in-game range.

Use **Options → Bridges** to turn automatic bridges on or off. Bridge spans are drawn in the preview before you build.

## Ore modes

Tap the ore option to cycle through:

- **Ore: automatic fallback** — default. First searches for a route that places no normal conveyor on ore. Only if that fails does it retry with a large ore penalty.
- **Ore: never cross** — no automatic intermediate conveyor may be placed on ore.
- **Ore: allow with penalty** — ore may be used, but clean ground is strongly preferred.

Explicit waypoints may still be placed on ore. A bridge may pass above ore because it does not place intermediate conveyors on those tiles.

## Route preferences

Tap the route option to cycle through:

- **Shortest** — prioritizes total path length while still applying a small corner penalty.
- **Straightest** — accepts a somewhat longer route to greatly reduce turns.
- **Least interference** — strongly avoids existing lines, planned structures, and crowded building edges. This is the default.

## Junctions and existing conveyor lines

- Empty ground is preferred.
- A single necessary 90-degree crossing of a compatible existing conveyor becomes a Junction.
- Parallel placement over an existing conveyor is rejected.
- Automatic intermediate tiles also avoid the output cell of built or planned conveyor/duct lines, preventing an unrelated line from feeding into the new route. Explicit waypoints can still be used for intentional merges.
- Unrelated routers, bridges, factories, pipes, and other built blocks are never silently replaced.
- Multiple crowded crossings may be passed with a real bridge when one is available.

## Intentional connections

Tap an existing friendly conveyor/duct, or a compatible conveyor/duct already in your build queue, as Point A or Point B to explicitly connect to it.

- A starting conveyor must point in the route's outgoing direction.
- A normal conveyor endpoint may be entered from its rear or side, but never head-on from the tile it is already outputting toward.
- Armored conveyor/duct endpoints must be approached from the valid rear direction; ordinary conveyors and ducts may also accept a side connection.
- The existing endpoint is kept; Auto Route does not replace it with a duplicate plan.

Ordinary automatically selected tiles still avoid accidental merges.

## Forbidden tiles

Open **Options**, enable **Forbidden tiles**, then tap map tiles to toggle them.

Forbidden tiles are shown with a red outline. Auto Route will neither place a route on them nor bridge through their airspace. This is useful for reserving room for future factories, defenses, power, liquids, or access paths.

Tap **Reset** to clear all forbidden tiles. Marks are kept for the current loaded world session and clear when the world changes.

## Existing build-plan awareness

Auto Route reads the local player's current construction queue and the live Mindustry placement previews.

- Planned structures are treated as occupied.
- The router may bridge over them when appropriate, but does not overwrite them.
- A compatible planned conveyor/duct may be tapped intentionally as an endpoint.
- If the build queue changes after the route was previewed, **Build** recalculates it and asks you to review the updated preview before committing.
- Build commits are all-or-nothing, preventing half of a bridge pair or another partial route from being queued when the map changes.

## Drill safety

Automatically selected intermediate conveyors are not placed directly beside drills, preventing unrelated drills from dumping items into the new route. Explicit waypoints beside drills remain allowed for intentional connections. Compatible modded blocks carrying Mindustry's drill flag are also recognized.

## Performance protection

To prevent long route calculations from freezing a phone:

- Android segments are limited to 500 Manhattan tiles; desktop allows 1000.
- Searches stop at a platform-specific node limit.
- Android searches use a shorter time budget than desktop.
- Recent path results are cached.
- Very long routes ask you to add an intermediate waypoint.

These limits apply to each waypoint-to-waypoint segment, so extremely long routes are still possible by adding a few waypoints.

## Install from GitHub

In Mindustry:

1. Open **Mods**.
2. Select **Import Mod**.
3. Select **Import from GitHub**.
4. Enter:

```text
YOUR-GITHUB-USERNAME/Mindustry-Auto-Route
```

Restart Mindustry when prompted.

## Automatic GitHub release

The included workflow builds one Android-and-desktop-compatible JAR and publishes it from `main`.

1. Replace your repository files with this project's contents.
2. Commit and push to `main`.
3. Open the **Actions** tab and wait for the build to finish.
4. The workflow creates tag `v0.4.0`, creates the GitHub Release, and attaches `MindustryAutoRoute.jar`.

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

- The router only works with ordinary 1x1 conveyor-placement blocks.
- Automatic bridge support depends on the selected block exposing an unlocked official bridge replacement.
- It does not automatically place sorters, overflow gates, unloaders, or routers because those change item-flow behavior rather than merely preserving a route.
- Multiplayer servers may apply their own block bans or placement limits; final placement remains subject to normal Mindustry validation.

## License

MIT License. See `LICENSE`.
