# Mindustry Auto Route

A client-side Mindustry mod for Android and desktop that builds conveyor routes from a few tapped waypoints.

## Version 0.3.0 highlights

- Auto Route's activation icon now mirrors AutoDrill's exact top-right HUD alignment and appears on the row directly below it.
- Existing conveyor lines are avoided when a reasonable empty route exists.
- When a perpendicular crossing is necessary, Auto Route automatically places a Junction instead of merging into or replacing the existing line.
- Parallel routing over existing conveyors is rejected to prevent accidental interference.
- The movable routing toolbar, ore controls, and drill-output safety remain included.

## How to use

1. Select a normal 1x1 conveyor-style block, such as a Conveyor, Armored Conveyor, Duct, or Armored Duct.
2. Tap the route icon near the top-right of the HUD.
3. Tap Point A.
4. Tap Point B. Auto Route previews a safe path between them.
5. Add more waypoints to control the general shape of the route.
6. Tap **Build** to add the preview to Mindustry's normal build queue.

### Existing conveyor crossings

For Conveyor-family blocks, Auto Route now treats existing friendly conveyors intelligently:

- It prefers routing over empty ground.
- It will not run parallel over an existing conveyor line.
- If the calculated route must cross an existing conveyor at 90 degrees, the crossing tile becomes the selected conveyor's Junction replacement.
- Junction crossing requires that the junction is unlocked and placeable. Otherwise, the existing conveyor is treated as an obstacle.

Automatic junction support currently applies to Conveyor-family blocks. Duct crossing support can be added separately because Mindustry v159.7 does not enable a default duct junction replacement.

### Movable toolbar

Hold and drag the four-way arrow in the toolbar's top-left corner. Its position is remembered independently for portrait and landscape orientation.

### Ore option

- **Ore: avoid**: automatically selected intermediate tiles cannot cross ore.
- **Ore: allow**: crossing ore is possible, but carries a large pathfinding penalty.

Waypoints themselves may still be placed on ore.

### Drill safety

Auto Route avoids automatically placing intermediate conveyors directly beside drills because drills can dump items into adjacent conveyors. This also recognizes compatible modded blocks marked as drills. You may still deliberately place Point A or Point B beside a drill when you actually want to connect it.

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

## Build and release with GitHub Actions

The included workflow builds one Android-and-desktop-compatible JAR and publishes it automatically from the `main` branch.

1. Upload the project contents to your repository.
2. Commit and push the changes to `main`.
3. Check the **Actions** tab for the build result.
4. When the build succeeds, the workflow creates tag `v0.3.0`, creates the GitHub Release, and attaches `MindustryAutoRoute.jar`.

For later versions, increase the version in both `mod.hjson` and `build.gradle` before pushing.

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

## License

MIT License. See `LICENSE`.
