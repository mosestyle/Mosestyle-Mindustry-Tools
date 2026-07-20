# Mindustry Auto Route

A client-side Mindustry mod for Android and desktop that builds conveyor routes from a few tapped waypoints.

## Version 0.2.1 highlights

- Fixed the movable toolbar build error for the current Mindustry/Arc API.
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

## Build and release with GitHub Actions

The included workflow builds one Android-and-desktop-compatible JAR and publishes it automatically from the `main` branch.

1. Upload the project contents to your repository.
2. Commit and push the changes to `main`.
3. Check the **Actions** tab for the build result.
4. When the build succeeds, the workflow creates tag `v0.2.1`, creates the GitHub Release, and attaches `MindustryAutoRoute.jar`.

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

## Automatic GitHub tags and releases

The included workflow can create the Git tag and GitHub Release automatically.

For each new public version:

1. Change `version` in `mod.hjson`, for example from `0.2.1` to `0.2.2`.
2. Change `version` near the top of `build.gradle` to the same value.
3. Commit and push the files to the `main` branch.

GitHub Actions then builds the Android/desktop JAR, creates the corresponding tag such as `v0.2.2`, creates the GitHub Release, and attaches `MindustryAutoRoute.jar`.

If that version's release already exists, the workflow still builds the project but does not replace the published release. This keeps every public version reproducible.
