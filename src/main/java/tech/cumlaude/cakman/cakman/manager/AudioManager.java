package tech.cumlaude.cakman.cakman.manager;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AudioManager {
    public static final String MENU_MUSIC = "/tech/cumlaude/cakman/cakman/musics/main-1.mp3";
    public static final String GAME_MUSIC = "/tech/cumlaude/cakman/cakman/musics/game-1.mp3";
    public static final String SFX_COLLECT = "/tech/cumlaude/cakman/cakman/sfx/collect_coin.mp3";
    public static final String SFX_COLLIDE_GHOST = "/tech/cumlaude/cakman/cakman/sfx/collide_ghost.mp3";
    public static final String SFX_EAT_GHOST = "/tech/cumlaude/cakman/cakman/sfx/eat_ghost.mp3";
    public static final String SFX_POWER_PELLET = "/tech/cumlaude/cakman/cakman/sfx/power_pellet.mp3";
    public static final String SFX_GAME_OVER = "/tech/cumlaude/cakman/cakman/sfx/game_over.mp3";
    public static final String SFX_VICTORY = "/tech/cumlaude/cakman/cakman/sfx/victory.mp3";
    public static final String SFX_BUTTON_CLICK = "/tech/cumlaude/cakman/cakman/sfx/button_clicked.mp3";

    private static final AudioManager INSTANCE = new AudioManager();

    private final Map<String, AudioClip> sfxCache = new ConcurrentHashMap<>();

    private MediaPlayer musicPlayer;
    private String currentMusicResource;
    private boolean musicEnabled = true;

    private double musicVolume = 0.55;
    private double soundEffectVolume = 0.85;

    private AudioManager() {
    }

    public static AudioManager getInstance() {
        return INSTANCE;
    }

    public synchronized boolean isMusicEnabled() {
        return musicEnabled;
    }

    public synchronized void setMusicEnabled(boolean enabled) {
        if (musicEnabled == enabled) {
            return;
        }

        musicEnabled = enabled;
        if (!musicEnabled) {
            stopCurrentMusic();
            return;
        }

        if (currentMusicResource != null) {
            playMusic(currentMusicResource);
        }
    }

    public synchronized void toggleMusic() {
        setMusicEnabled(!musicEnabled);
    }

    public void playMenuMusic() {
        playMusic(MENU_MUSIC);
    }

    public void playGameMusic() {
        playMusic(GAME_MUSIC);
    }

    public synchronized void playMusic(String resourcePath) {
        currentMusicResource = resourcePath;

        if (!musicEnabled) {
            stopCurrentMusic();
            return;
        }

        URL url = resolveResource(resourcePath);
        if (url == null) {
            return;
        }

        if (musicPlayer != null) {
            stopCurrentMusic();
        }

        try {
            Media media = new Media(url.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(musicVolume);
            player.setOnError(() -> System.err.println("AudioManager music error: " + player.getError()));
            musicPlayer = player;
            player.play();
        } catch (Exception e) {
            System.err.println("AudioManager failed to play music: " + resourcePath + " -> " + e.getMessage());
        }
    }

    public synchronized void stopCurrentMusic() {
        if (musicPlayer != null) {
            try {
                musicPlayer.stop();
                musicPlayer.dispose();
            } catch (Exception e) {
                System.err.println("AudioManager failed to stop music: " + e.getMessage());
            } finally {
                musicPlayer = null;
            }
        }
    }

    public void playSoundEffect(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return;
        }

        URL url = resolveResource(resourcePath);
        if (url == null) {
            return;
        }

        try {
            AudioClip clip = sfxCache.get(resourcePath);
            if (clip == null) {
                clip = new AudioClip(url.toExternalForm());
                sfxCache.put(resourcePath, clip);
            }
            clip.setVolume(soundEffectVolume);
            clip.play();
        } catch (Exception e) {
            System.err.println("AudioManager failed to play sound effect: " + resourcePath + " -> " + e.getMessage());
        }
    }

    public synchronized void setMusicVolume(double musicVolume) {
        this.musicVolume = clampVolume(musicVolume);
        if (musicPlayer != null) {
            musicPlayer.setVolume(this.musicVolume);
        }
    }

    public synchronized double getMusicVolume() {
        return musicVolume;
    }

    public synchronized void setSoundEffectVolume(double soundEffectVolume) {
        this.soundEffectVolume = clampVolume(soundEffectVolume);
    }

    public synchronized void stopAll() {
        stopCurrentMusic();
        sfxCache.clear();
    }

    private URL resolveResource(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }

        URL url = AudioManager.class.getResource(resourcePath);
        if (url == null) {
            System.err.println("AudioManager resource not found: " + resourcePath);
        }
        return url;
    }

    private double clampVolume(double value) {
        return Math.clamp(value, 0.0, 1.0);
    }
}


