package autoroute;

import arc.ApplicationListener;
import arc.Core;
import arc.audio.Music;
import arc.files.Fi;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.FileChooser;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable;

import java.util.Locale;

/**
 * Cross-platform custom music player for Mosestyle Mindustry Tools.
 *
 * <p>Imported tracks are copied into the Mindustry data directory so Android
 * document permissions do not have to be retained. Playback uses Arc's normal
 * streamed {@link Music} API and has a volume setting independent of
 * Mindustry's official soundtrack volume.</p>
 */
public class CustomMusicPlayer{
    public static final String enabledSetting = "mindustry-auto-route-custom-music-enabled";
    public static final String volumeSetting = "mindustry-auto-route-custom-music-volume";
    public static final String shuffleSetting = "mindustry-auto-route-custom-music-shuffle";
    public static final String repeatSetting = "mindustry-auto-route-custom-music-repeat";
    public static final String showControlsSetting = "mindustry-auto-route-custom-music-controls";
    public static final String muteOfficialSetting = "mindustry-auto-route-custom-music-mute-official";

    private static final String currentTrackSetting = "mindustry-auto-route-custom-music-current-track";
    private static final String previousOfficialVolumeSetting = "mindustry-auto-route-custom-music-previous-official-volume";
    private static final String officialMutedMarkerSetting = "mindustry-auto-route-custom-music-official-muted";
    private static final String panelPositionPrefix = "mindustry-auto-route-music-panel-";

    private static final String[] supportedExtensions = {"mp3", "ogg", "wav", "flac"};
    private static final long playbackStartGraceNanos = 900_000_000L;
    private static final long playbackFailureGraceNanos = 2_500_000_000L;

    private final Seq<Fi> tracks = new Seq<>();

    private Fi musicDirectory;
    private Music currentMusic;
    private int currentIndex;
    private boolean paused;
    private boolean playRequested;
    private boolean playbackStarted;
    private long playbackStartNanos;

    private Table playerPanel;
    private ImageButton playPauseButton;
    private TextButton shuffleButton;
    private Slider volumeSlider;
    private Label volumeLabel;

    private boolean panelPositionReady;
    private float lastSceneWidth = -1f;
    private float lastSceneHeight = -1f;

    public void init(){
        musicDirectory = Core.settings.getDataDirectory()
            .child("mosestyle-mindustry-tools")
            .child("music");
        musicDirectory.mkdirs();

        scanLibrary();
        buildPlayerPanel();
        recoverOfficialMusicVolumeIfNeeded();

        Core.app.addListener(new ApplicationListener(){
            @Override
            public void update(){
                updatePlayback();
            }

            @Override
            public void dispose(){
                shutdown(true);
            }
        });

        Core.app.post(() -> {
            if(isEnabled() && !tracks.isEmpty()){
                playCurrentTrack(false);
            }else{
                applyOfficialMusicMuteState();
            }
        });
    }

    public void addSettings(SettingsTable table){
        table.pref(new SectionSetting("mindustry-auto-route-custom-music-section"));
        table.checkPref(enabledSetting, false, this::onEnabledChanged);
        table.checkPref(showControlsSetting, true, value -> refreshPanelVisibility());
        table.checkPref(muteOfficialSetting, true, value -> applyOfficialMusicMuteState());
        table.checkPref(shuffleSetting, false, value -> refreshControlState());
        table.checkPref(repeatSetting, true);
        table.sliderPref(volumeSetting, 75, 0, 100, 5,
            value -> value + "%",
            value -> onVolumeChanged()
        );

        table.pref(new ActionSetting(
            "mindustry-auto-route-custom-music-import",
            Icon.download,
            this::importMusic
        ));
        table.pref(new ActionSetting(
            "mindustry-auto-route-custom-music-library",
            Icon.settings,
            this::showLibraryDialog
        ));

        if(Core.app.isDesktop()){
            table.pref(new ActionSetting(
                "mindustry-auto-route-custom-music-open-folder",
                Icon.folder,
                this::openMusicFolder
            ));
        }
    }

    private void buildPlayerPanel(){
        playerPanel = new Table(Styles.black6);
        playerPanel.margin(4f);
        playerPanel.visible(this::shouldShowControls);
        playerPanel.update(() -> {
            playerPanel.color.a = 0.90f;
            updatePanelPlacement();
            refreshControlState();
        });

        Table header = new Table();
        header.left();

        TextureRegionDrawable moveIcon = new TextureRegionDrawable(
            Core.atlas.find("mindustry-auto-route-move")
        );
        ImageButton moveButton = header.button(moveIcon, Styles.cleari, () -> {})
            .size(30f)
            .get();
        moveButton.resizeImage(19f);
        addPanelDragListener(moveButton);

        header.label(this::displayTrackName)
            .width(186f)
            .left()
            .padLeft(3f);

        playerPanel.add(header).width(220f).height(30f).left();
        playerPanel.row();

        Table controls = new Table();
        controls.defaults().height(36f).pad(1f);

        controls.button(Icon.left, Styles.cleari, this::previousTrack)
            .size(36f);

        playPauseButton = controls.button(Icon.play, Styles.cleari, this::togglePlayPause)
            .size(36f)
            .get();

        controls.button(Icon.right, Styles.cleari, () -> nextTrack(false))
            .size(36f);

        shuffleButton = controls.button("Shuffle", Styles.clearTogglet, this::toggleShuffle)
            .width(76f)
            .height(36f)
            .get();

        playerPanel.add(controls).width(220f).left();
        playerPanel.row();

        Table volume = new Table();
        volume.left();
        volume.add("Vol").width(30f).left();

        volumeSlider = new Slider(0f, 100f, 5f, false);
        volumeSlider.setValue(Core.settings.getInt(volumeSetting, 75));
        volumeSlider.changed(() -> {
            int value = Math.round(volumeSlider.getValue());
            Core.settings.put(volumeSetting, value);
            applyCustomVolume();
        });
        volume.add(volumeSlider).width(142f).height(30f);

        volumeLabel = new Label(volumeText(), Styles.outlineLabel);
        volume.add(volumeLabel).width(44f).right();

        playerPanel.add(volume).width(220f).height(34f).left();
        playerPanel.row();

        Vars.ui.hudGroup.addChild(playerPanel);
        playerPanel.pack();
    }

    private boolean shouldShowControls(){
        if(!isEnabled() || !Core.settings.getBool(showControlsSetting, true)) return false;
        if(Vars.state.isMenu() || Vars.ui == null || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) return false;
        return Vars.ui.minimapfrag == null || !Vars.ui.minimapfrag.shown();
    }

    private void addPanelDragListener(ImageButton moveButton){
        moveButton.addListener(new InputListener(){
            private float grabX;
            private float grabY;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                grabX = x;
                grabY = y;
                playerPanel.toFront();
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                playerPanel.moveBy(x - grabX, y - grabY);
                clampPanelToScreen();
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                savePanelPosition();
            }
        });
    }

    private void updatePanelPlacement(){
        if(playerPanel == null) return;

        float sceneWidth = Core.scene.getWidth();
        float sceneHeight = Core.scene.getHeight();
        if(sceneWidth <= 0f || sceneHeight <= 0f) return;

        boolean resized = sceneWidth != lastSceneWidth || sceneHeight != lastSceneHeight;
        if(!panelPositionReady || resized){
            restorePanelPosition(sceneWidth, sceneHeight);
            panelPositionReady = true;
            lastSceneWidth = sceneWidth;
            lastSceneHeight = sceneHeight;
        }else{
            clampPanelToScreen();
        }
    }

    private void restorePanelPosition(float sceneWidth, float sceneHeight){
        playerPanel.pack();

        boolean portrait = sceneHeight > sceneWidth;
        String orientation = portrait ? "portrait-" : "landscape-";

        float defaultCenterX = portrait
            ? 0.50f
            : Math.max(0.15f, 1f - (playerPanel.getWidth() / 2f + 18f) / sceneWidth);
        float defaultCenterY = portrait ? 0.62f : 0.42f;

        float centerX = Core.settings.getFloat(panelPositionPrefix + orientation + "x", defaultCenterX);
        float centerY = Core.settings.getFloat(panelPositionPrefix + orientation + "y", defaultCenterY);

        playerPanel.setPosition(
            centerX * sceneWidth - playerPanel.getWidth() / 2f,
            centerY * sceneHeight - playerPanel.getHeight() / 2f
        );
        clampPanelToScreen();
    }

    private void savePanelPosition(){
        if(playerPanel == null || Core.scene.getWidth() <= 0f || Core.scene.getHeight() <= 0f) return;

        boolean portrait = Core.scene.getHeight() > Core.scene.getWidth();
        String orientation = portrait ? "portrait-" : "landscape-";

        float centerX = (playerPanel.x + playerPanel.getWidth() / 2f) / Core.scene.getWidth();
        float centerY = (playerPanel.y + playerPanel.getHeight() / 2f) / Core.scene.getHeight();

        Core.settings.put(panelPositionPrefix + orientation + "x", centerX);
        Core.settings.put(panelPositionPrefix + orientation + "y", centerY);
    }

    private void clampPanelToScreen(){
        if(playerPanel == null || Core.scene == null) return;

        float maxX = Math.max(0f, Core.scene.getWidth() - playerPanel.getWidth());
        float maxY = Math.max(0f, Core.scene.getHeight() - playerPanel.getHeight());

        playerPanel.setPosition(
            Math.max(0f, Math.min(playerPanel.x, maxX)),
            Math.max(0f, Math.min(playerPanel.y, maxY))
        );
    }

    private void importMusic(){
        FileChooser.open(supportedExtensions)
            .title("Import music")
            .submitMulti(files -> Vars.ui.loadAnd("Importing music", () -> importSelectedFiles((Fi[])files)));
    }

    private void importSelectedFiles(Fi[] files){
        int imported = 0;
        int skipped = 0;

        try{
            musicDirectory.mkdirs();

            for(Fi source : files){
                if(source == null || source.isDirectory() || !isSupported(source)){
                    skipped++;
                    continue;
                }

                String extension = source.extension().toLowerCase(Locale.ROOT);
                String base = Strings.sanitizeFilename(source.nameWithoutExtension());
                if(base == null || base.trim().isEmpty()) base = "track";

                Fi destination = uniqueDestination(base, extension);
                source.copyTo(destination);
                imported++;
            }

            scanLibrary();

            if(isEnabled() && currentMusic == null && !tracks.isEmpty()){
                playCurrentTrack(false);
            }
            applyOfficialMusicMuteState();
            refreshPanelVisibility();

            String message = "Imported " + imported + " track" + (imported == 1 ? "." : "s.");
            if(skipped > 0) message += " Skipped " + skipped + " unsupported file" + (skipped == 1 ? "." : "s.");
            Vars.ui.showInfoToast(message, 4f);
        }catch(Throwable error){
            Log.err(error);
            Vars.ui.showException("Could not import one or more music files.", error);
        }
    }

    private Fi uniqueDestination(String base, String extension){
        Fi destination = musicDirectory.child(base + "." + extension);
        int suffix = 2;
        while(destination.exists()){
            destination = musicDirectory.child(base + "-" + suffix + "." + extension);
            suffix++;
        }
        return destination;
    }

    private void scanLibrary(){
        String currentName = currentTrackFile() == null
            ? Core.settings.getString(currentTrackSetting, "")
            : currentTrackFile().name();

        tracks.clear();
        musicDirectory.mkdirs();
        for(Fi file : musicDirectory.list()){
            if(!file.isDirectory() && isSupported(file)) tracks.add(file);
        }
        tracks.sort((first, second) -> first.name().compareToIgnoreCase(second.name()));

        currentIndex = 0;
        if(!currentName.isEmpty()){
            for(int i = 0; i < tracks.size; i++){
                if(tracks.get(i).name().equals(currentName)){
                    currentIndex = i;
                    break;
                }
            }
        }
        clampCurrentIndex();
        refreshControlState();
    }

    private void showLibraryDialog(){
        scanLibrary();

        BaseDialog dialog = new BaseDialog("Custom Music Library");
        Table trackList = new Table();
        trackList.top().left();

        Runnable rebuild = () -> rebuildLibraryRows(trackList, dialog);
        rebuild.run();

        dialog.cont.pane(trackList).grow().maxWidth(620f);
        dialog.buttons.defaults().height(54f).pad(2f);
        dialog.buttons.button("Import music", Icon.download, () -> {
            dialog.hide();
            importMusic();
        });

        if(Core.app.isDesktop()){
            dialog.buttons.button("Open folder", Icon.folder, this::openMusicFolder);
        }

        dialog.buttons.button("Refresh", () -> {
            scanLibrary();
            rebuild.run();
        });
        dialog.buttons.row();

        dialog.buttons.button("Remove all", Icon.trash, () -> {
            if(tracks.isEmpty()) return;
            Vars.ui.showConfirm("Remove imported music", "Remove every imported track?", () -> {
                stopAndDisposeCurrent();
                for(Fi track : tracks.copy()) track.delete();
                scanLibrary();
                applyOfficialMusicMuteState();
                rebuild.run();
            });
        });
        dialog.addCloseButton();
        dialog.show();
    }

    private void rebuildLibraryRows(Table table, BaseDialog dialog){
        table.clearChildren();
        table.margin(6f);

        if(tracks.isEmpty()){
            table.add("No music has been imported yet.\nSupported formats: MP3, OGG, WAV and FLAC.")
                .width(420f)
                .wrap()
                .center()
                .pad(20f);
            return;
        }

        for(Fi track : tracks.copy()){
            table.table(Styles.grayPanel, row -> {
                row.margin(8f);
                row.add(shorten(track.nameWithoutExtension(), 46))
                    .left()
                    .growX();
                row.button(Icon.trash, Styles.cleari, () -> {
                    Vars.ui.showConfirm("Remove track", "Remove \"" + track.name() + "\"?", () -> {
                        removeTrack(track);
                        rebuildLibraryRows(table, dialog);
                    });
                }).size(42f);
            }).growX().height(50f).padBottom(3f);
            table.row();
        }
    }

    private void removeTrack(Fi track){
        boolean removingCurrent = currentTrackFile() != null && currentTrackFile().equals(track);
        if(removingCurrent) stopAndDisposeCurrent();

        track.delete();
        scanLibrary();

        if(removingCurrent && isEnabled() && !tracks.isEmpty()){
            playCurrentTrack(false);
        }
        applyOfficialMusicMuteState();
    }

    private void openMusicFolder(){
        musicDirectory.mkdirs();
        if(!Core.app.openFolder(musicDirectory.absolutePath())){
            Vars.ui.showInfoToast("The music folder could not be opened on this device.", 3f);
        }
    }

    private boolean isSupported(Fi file){
        String extension = file.extension().toLowerCase(Locale.ROOT);
        for(String supported : supportedExtensions){
            if(supported.equals(extension)) return true;
        }
        return false;
    }

    private void onEnabledChanged(boolean enabled){
        if(enabled){
            scanLibrary();
            if(tracks.isEmpty()){
                Vars.ui.showInfoToast("Custom music is enabled, but no tracks are imported yet.", 4f);
            }else{
                playCurrentTrack(false);
            }
        }else{
            stopAndDisposeCurrent();
        }
        applyOfficialMusicMuteState();
        refreshPanelVisibility();
    }

    private void togglePlayPause(){
        if(tracks.isEmpty()){
            Vars.ui.showInfoToast("Import music from Settings → Mosestyle Tools first.", 4f);
            return;
        }

        if(currentMusic == null){
            playCurrentTrack(true);
            return;
        }

        if(paused || !currentMusic.isPlaying()){
            paused = false;
            playRequested = true;
            currentMusic.play();
        }else{
            paused = true;
            currentMusic.pause(true);
        }
        refreshControlState();
    }

    private void toggleShuffle(){
        boolean enabled = !Core.settings.getBool(shuffleSetting, false);
        Core.settings.put(shuffleSetting, enabled);
        refreshControlState();
    }

    private void previousTrack(){
        if(tracks.isEmpty()) return;

        if(currentMusic != null && currentMusic.getPosition() > 4f){
            currentMusic.setPosition(0f);
            return;
        }

        currentIndex--;
        if(currentIndex < 0) currentIndex = tracks.size - 1;
        playCurrentTrack(true);
    }

    private void nextTrack(boolean automatic){
        if(tracks.isEmpty()) return;

        if(Core.settings.getBool(shuffleSetting, false) && tracks.size > 1){
            int oldIndex = currentIndex;
            do{
                currentIndex = (int)(Math.random() * tracks.size);
            }while(currentIndex == oldIndex);
        }else{
            currentIndex++;
            if(currentIndex >= tracks.size){
                if(automatic && !Core.settings.getBool(repeatSetting, true)){
                    currentIndex = Math.max(0, tracks.size - 1);
                    stopAndDisposeCurrent();
                    refreshControlState();
                    return;
                }
                currentIndex = 0;
            }
        }

        playCurrentTrack(!automatic);
    }

    private void playCurrentTrack(boolean userRequested){
        if(!isEnabled()){
            if(userRequested){
                Core.settings.put(enabledSetting, true);
            }else{
                return;
            }
        }

        if(tracks.isEmpty()){
            if(userRequested) Vars.ui.showInfoToast("No custom music has been imported.", 3f);
            return;
        }

        clampCurrentIndex();
        stopAndDisposeCurrent();

        int attempts = tracks.size;
        while(attempts-- > 0){
            Fi track = tracks.get(currentIndex);
            Music candidate = Core.audio.newMusic(track);

            if(candidate.valid()){
                currentMusic = candidate;
                currentMusic.setLooping(false);
                applyCustomVolume();
                currentMusic.play();

                paused = false;
                playRequested = true;
                playbackStarted = false;
                playbackStartNanos = System.nanoTime();
                Core.settings.put(currentTrackSetting, track.name());
                applyOfficialMusicMuteState();
                refreshControlState();
                return;
            }

            candidate.dispose();
            currentIndex = (currentIndex + 1) % tracks.size;
        }

        playRequested = false;
        paused = false;
        Vars.ui.showInfoToast("None of the imported tracks could be decoded.", 5f);
        applyOfficialMusicMuteState();
        refreshControlState();
    }

    private void updatePlayback(){
        applyOfficialMusicMuteState();

        if(currentMusic == null || !playRequested || paused) return;

        if(currentMusic.isPlaying()){
            playbackStarted = true;
            return;
        }

        long elapsed = System.nanoTime() - playbackStartNanos;
        if((playbackStarted && elapsed >= playbackStartGraceNanos) || elapsed >= playbackFailureGraceNanos){
            nextTrack(true);
        }
    }

    private void stopAndDisposeCurrent(){
        if(currentMusic != null){
            try{
                currentMusic.stop();
                currentMusic.dispose();
            }catch(Throwable error){
                Log.err(error);
            }
        }

        currentMusic = null;
        paused = false;
        playRequested = false;
        playbackStarted = false;
    }

    private void applyCustomVolume(){
        if(currentMusic != null){
            currentMusic.setVolume(Core.settings.getInt(volumeSetting, 75) / 100f);
        }
        if(volumeLabel != null) volumeLabel.setText(volumeText());
    }

    public void onVolumeChanged(){
        int settingVolume = Core.settings.getInt(volumeSetting, 75);
        if(volumeSlider != null && Math.round(volumeSlider.getValue()) != settingVolume){
            volumeSlider.setValue(settingVolume);
        }
        applyCustomVolume();
    }

    private void refreshControlState(){
        if(playPauseButton != null){
            playPauseButton.getImage().setDrawable(paused || currentMusic == null || !playRequested ? Icon.play : Icon.pause);
        }
        if(shuffleButton != null){
            shuffleButton.setChecked(Core.settings.getBool(shuffleSetting, false));
        }
        if(volumeLabel != null) volumeLabel.setText(volumeText());
    }

    private void refreshPanelVisibility(){
        if(playerPanel != null){
            playerPanel.visible(this::shouldShowControls);
        }
    }

    private String displayTrackName(){
        Fi track = currentTrackFile();
        if(track == null){
            return tracks.isEmpty() ? "No music imported" : "Ready";
        }
        return shorten(track.nameWithoutExtension(), 29);
    }

    private String volumeText(){
        return Core.settings.getInt(volumeSetting, 75) + "%";
    }

    private String shorten(String text, int maximum){
        if(text == null) return "";
        if(text.length() <= maximum) return text;
        return text.substring(0, Math.max(1, maximum - 1)) + "…";
    }

    private Fi currentTrackFile(){
        return tracks.isEmpty() || currentIndex < 0 || currentIndex >= tracks.size
            ? null
            : tracks.get(currentIndex);
    }

    private void clampCurrentIndex(){
        if(tracks.isEmpty()){
            currentIndex = 0;
        }else{
            currentIndex = Math.max(0, Math.min(currentIndex, tracks.size - 1));
        }
    }

    private boolean isEnabled(){
        return Core.settings.getBool(enabledSetting, false);
    }

    private void recoverOfficialMusicVolumeIfNeeded(){
        boolean markedMuted = Core.settings.getBool(officialMutedMarkerSetting, false);
        boolean shouldRemainMuted = isEnabled()
            && Core.settings.getBool(muteOfficialSetting, true)
            && currentMusic != null;

        if(markedMuted && !shouldRemainMuted){
            restoreOfficialMusicVolume();
        }else if(shouldRemainMuted){
            applyOfficialMusicMuteState();
        }
    }

    private void applyOfficialMusicMuteState(){
        boolean shouldMute = isEnabled()
            && Core.settings.getBool(muteOfficialSetting, true)
            && currentMusic != null;

        boolean markedMuted = Core.settings.getBool(officialMutedMarkerSetting, false);

        if(shouldMute){
            if(!markedMuted){
                Core.settings.put(previousOfficialVolumeSetting, Core.settings.getInt("musicvol", 100));
                Core.settings.put(officialMutedMarkerSetting, true);
            }
            if(Core.settings.getInt("musicvol", 100) != 0){
                Core.settings.put("musicvol", 0);
            }
        }else if(markedMuted){
            restoreOfficialMusicVolume();
        }
    }

    private void restoreOfficialMusicVolume(){
        int previous = Core.settings.getInt(previousOfficialVolumeSetting, 100);
        Core.settings.put("musicvol", Math.max(0, Math.min(previous, 100)));
        Core.settings.put(officialMutedMarkerSetting, false);
    }

    private void shutdown(boolean restoreOfficial){
        stopAndDisposeCurrent();
        if(restoreOfficial && Core.settings.getBool(officialMutedMarkerSetting, false)){
            restoreOfficialMusicVolume();
        }
    }


    private static class SectionSetting extends SettingsMenuDialog.Setting{
        SectionSetting(String name){
            super(name);
        }

        @Override
        public void add(SettingsTable table){
            table.add("[accent]" + title + "[]")
                .left()
                .growX()
                .padTop(18f)
                .padBottom(4f);
            table.row();
        }
    }

    /** Button row that survives SettingsTable rebuilds like a normal preference. */
    private static class ActionSetting extends SettingsMenuDialog.Setting{
        private final Drawable icon;
        private final Runnable action;

        ActionSetting(String name, Drawable icon, Runnable action){
            super(name);
            this.icon = icon;
            this.action = action;
        }

        @Override
        public void add(SettingsTable table){
            Button button = new Button(Styles.grayt);
            button.background(Styles.grayPanel);
            button.margin(10f);
            button.image(icon).size(30f).padRight(8f).padLeft(-4f);
            button.add(title).left().growX();
            button.clicked(action);
            button.left();

            Element element = table.add(button)
                .minWidth(Math.min(500f, Core.graphics.getWidth() / 1.2f))
                .fillX()
                .height(48f)
                .left()
                .padTop(7f)
                .get();
            addDesc(element);
            table.row();
        }
    }
}
