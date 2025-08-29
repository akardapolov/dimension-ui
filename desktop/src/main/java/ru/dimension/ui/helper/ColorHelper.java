package ru.dimension.ui.helper;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.model.config.ColorProfile;

@Log4j2
@Singleton
public class ColorHelper {
  private static final long SYNC_DELAY = 1; // minutes
  private static final String DEFAULT_PROFILE_NAME = "default";

  private final FilesHelper filesHelper;
  private final ConfigurationManager configurationManager;
  private final ScheduledExecutorService scheduler;
  private final Map<String, ColorProfile> colorProfiles = new ConcurrentHashMap<>();
  private final Map<String, Boolean> dirtyProfiles = new ConcurrentHashMap<>();
  private final Map<String, Object> profileLocks = new ConcurrentHashMap<>();
  private ColorProfile defaultProfile;

  @Inject
  public ColorHelper(FilesHelper filesHelper,
                     @Named("configurationManager") ConfigurationManager configurationManager) {
    this.filesHelper = filesHelper;
    this.configurationManager = configurationManager;
    this.scheduler = Executors.newSingleThreadScheduledExecutor();

    loadDefaultProfile();
    startSynchronization();

    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  private void startSynchronization() {
    scheduler.scheduleAtFixedRate(this::syncProfilesToDisk, SYNC_DELAY, SYNC_DELAY, TimeUnit.MINUTES);
  }

  private void shutdown() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    syncProfilesToDisk(); // Final sync on shutdown
  }

  private void syncProfilesToDisk() {
    for (String profileName : dirtyProfiles.keySet()) {
      if (dirtyProfiles.remove(profileName) != null) {
        ColorProfile profile = colorProfiles.get(profileName);
        if (profile != null) {
          try {
            configurationManager.updateConfig(profile, ColorProfile.class);
            log.debug("Synced color profile '{}' to disk", profileName);
          } catch (Exception e) {
            log.error("Failed to sync color profile '{}' to disk", profileName, e);
            dirtyProfiles.put(profileName, true); // Mark as dirty again on failure
          }
        }
      }
    }
  }

  private void loadDefaultProfile() {
    String folderPath = filesHelper.getColorsDir();
    try {
      filesHelper.loadFileToFolder("default.json", "json/colors", folderPath);

      // Load default profile into memory
      try {
        defaultProfile = configurationManager.getConfig(ColorProfile.class, DEFAULT_PROFILE_NAME);
        colorProfiles.put(DEFAULT_PROFILE_NAME, defaultProfile);
        log.info("Default color profile loaded successfully");
      } catch (NotFoundException e) {
        log.warn("Default color profile not found, creating empty one");
        defaultProfile = new ColorProfile();
        defaultProfile.setId(DEFAULT_PROFILE_NAME.hashCode());
        defaultProfile.setName(DEFAULT_PROFILE_NAME);
        defaultProfile.setColors(new HashMap<>());
        configurationManager.addConfig(defaultProfile, ColorProfile.class);
        colorProfiles.put(DEFAULT_PROFILE_NAME, defaultProfile);
      }
    } catch (IOException e) {
      log.error("Failed to load default color profile", e);
      // Create empty default profile if loading fails
      defaultProfile = new ColorProfile();
      defaultProfile.setId(DEFAULT_PROFILE_NAME.hashCode());
      defaultProfile.setName(DEFAULT_PROFILE_NAME);
      defaultProfile.setColors(new HashMap<>());
    }
  }

  private ColorProfile loadOrCreateProfile(String colorProfileName) {
    ColorProfile profile;
    try {
      profile = configurationManager.getConfig(ColorProfile.class, colorProfileName);
    } catch (NotFoundException e) {
      profile = new ColorProfile();
      profile.setId(colorProfileName.hashCode());
      profile.setName(colorProfileName);
      profile.setColors(new HashMap<>());
      configurationManager.addConfig(profile, ColorProfile.class);
      log.info("Created new color profile: {}", colorProfileName);
    }

    colorProfiles.put(colorProfileName, profile);
    return profile;
  }

  public Map<String, Color> getColorMap(String colorProfileName) {
    ColorProfile profile = colorProfiles.computeIfAbsent(colorProfileName, this::loadOrCreateProfile);
    Map<String, Color> colorMap = new HashMap<>();

    if (profile.getColors() != null) {
      for (Map.Entry<String, String> entry : profile.getColors().entrySet()) {
        try {
          colorMap.put(entry.getKey(), Color.decode(entry.getValue()));
        } catch (NumberFormatException e) {
          log.warn("Invalid color format for key {}: {}", entry.getKey(), entry.getValue());
        }
      }
    }
    return colorMap;
  }

  public Color getColor(String colorProfileName, String seriesName) {
    ColorProfile profile = colorProfiles.get(colorProfileName);

    if (profile == null) {
      Object lock = profileLocks.computeIfAbsent(colorProfileName, k -> new Object());
      synchronized (lock) {
        profile = colorProfiles.get(colorProfileName);
        if (profile == null) {
          profile = loadOrCreateProfile(colorProfileName);
        }
      }
    }

    // First check if the color exists in the requested profile
    if (profile.getColors().containsKey(seriesName)) {
      try {
        return Color.decode(profile.getColors().get(seriesName));
      } catch (NumberFormatException e) {
        log.warn("Invalid color format for key {}: {}", seriesName, profile.getColors().get(seriesName));
      }
    }

    // If not found in requested profile, check default profile
    Color colorFromDefault = getColorFromDefaultProfile(seriesName);
    if (colorFromDefault != null) {
      saveColorToProfile(profile, seriesName, colorFromDefault, colorProfileName);
      return colorFromDefault;
    }

    // If not found in default profile either, generate a new color
    Color newColor = ColorPaletteHelper.generateHSBBasedColor(profile);

    saveColorToProfile(profile, seriesName, newColor, colorProfileName);
    return newColor;
  }

  private Color getColorFromDefaultProfile(String seriesName) {
    if (defaultProfile != null && defaultProfile.getColors() != null &&
        defaultProfile.getColors().containsKey(seriesName)) {
      try {
        return Color.decode(defaultProfile.getColors().get(seriesName));
      } catch (NumberFormatException e) {
        log.warn("Invalid color format in default profile for key {}: {}",
                 seriesName, defaultProfile.getColors().get(seriesName));
      }
    }
    return null;
  }

  private void saveColorToProfile(ColorProfile profile, String seriesName, Color color, String profileName) {
    String hexColor = String.format("#%02x%02x%02x",
                                    color.getRed(), color.getGreen(), color.getBlue());

    // Update the profile and mark as dirty
    synchronized (profile) {
      profile.getColors().put(seriesName, hexColor);
    }
    dirtyProfiles.put(profileName, true);

    log.debug("Added color for '{}' in profile '{}': {}", seriesName, profileName, hexColor);
  }

  public ColorProfile getDefaultProfile() {
    return defaultProfile;
  }
}