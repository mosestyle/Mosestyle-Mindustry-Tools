# Mindustry Auto Route

A client-side Mindustry mod for Android and desktop that builds conveyor routes from a few tapped waypoints.

## Version 0.2.0 highlights

- Small icon button instead of the wide **Auto Route** text button.
- Positioned below AutoDrill's top-right toggle so both can be visible.
- Compact routing toolbar that can be dragged anywhere using the four-way arrow.
- Separate remembered toolbar positions for portrait and landscape mode.
- Smart avoidance of tiles directly beside drills, preventing unrelated drills from dumping resources onto the new conveyor line.

## How to use

1. Select a normal 1x1 conveyor-style block, such as a Conveyor, Armored Conveyor, Duct, or Armored Duct.
2. Tap the orange route icon near the top-right of the HUD.
3. Tap Point A.
4. Tap Point B. Auto Route previews a safe path between them.
5. Add more waypoints to control the general shape of the route.
6. Tap **Build** to add the preview to Mindustry's normal build queue.

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

## Build with GitHub Actions

The included workflow builds one Android-and-desktop-compatible JAR.

1. Upload the project contents to your repository.
2. Commit and push the changes.
3. Check the **Actions** tab for the build result.
4. Create a release/tag named `v0.2.0`.
5. The workflow attaches `MindustryAutoRoute.jar` to the release.

Important: keep the version in `mod.hjson` equal to the latest public release version.

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
