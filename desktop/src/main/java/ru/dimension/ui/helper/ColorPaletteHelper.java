package ru.dimension.ui.helper;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import ru.dimension.ui.model.config.ColorProfile;

public final class ColorPaletteHelper {

  // Constants for bright yet soft color generation
  private static final int MIN_BRIGHT = 120;     // Minimum to ensure visibility
  private static final int MAX_BRIGHT = 240;     // Maximum to avoid eye strain
  private static final int MIN_SATURATION = 100; // Minimum saturation for colorfulness
  private static final int MAX_SATURATION = 220; // Maximum to avoid overly intense colors

  // Predefined bright yet soft colors
  public static final Color BRIGHT_SOFT_RED = new Color(240, 120, 120);
  public static final Color BRIGHT_SOFT_BLUE = new Color(120, 160, 240);
  public static final Color BRIGHT_SOFT_GREEN = new Color(140, 220, 140);
  public static final Color BRIGHT_SOFT_YELLOW = new Color(240, 220, 120);
  public static final Color BRIGHT_SOFT_ORANGE = new Color(240, 180, 100);
  public static final Color BRIGHT_SOFT_PURPLE = new Color(180, 140, 220);
  public static final Color BRIGHT_SOFT_CYAN = new Color(120, 220, 240);
  public static final Color BRIGHT_SOFT_PINK = new Color(240, 160, 200);

  private ColorPaletteHelper() {
    // Utility class - prevent instantiation
  }

  /**
   * Generates a bright yet soft color with good visibility
   * Balances between brightness and comfort
   */
  public static Color generateBrightSoftColor() {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    // Choose a color family to bias towards
    int colorFamily = random.nextInt(8);

    switch (colorFamily) {
      case 0: // Red-biased (bright red with softened green/blue)
        return new Color(
            MIN_SATURATION + 80 + random.nextInt(40),  // 180-220 (bright red)
            MIN_BRIGHT + random.nextInt(60),           // 120-180 (softened)
            MIN_BRIGHT + random.nextInt(60)            // 120-180 (softened)
        );
      case 1: // Green-biased (bright green with softened red/blue)
        return new Color(
            MIN_BRIGHT + random.nextInt(60),           // 120-180 (softened)
            MIN_SATURATION + 80 + random.nextInt(40),  // 180-220 (bright green)
            MIN_BRIGHT + random.nextInt(60)            // 120-180 (softened)
        );
      case 2: // Blue-biased (bright blue with softened red/green)
        return new Color(
            MIN_BRIGHT + random.nextInt(60),           // 120-180 (softened)
            MIN_BRIGHT + random.nextInt(60),           // 120-180 (softened)
            MIN_SATURATION + 80 + random.nextInt(40)   // 180-220 (bright blue)
        );
      case 3: // Yellow-biased (bright yellow with softened blue)
        return new Color(
            MIN_SATURATION + 80 + random.nextInt(40),  // 180-220 (bright red)
            MIN_SATURATION + 80 + random.nextInt(40),  // 180-220 (bright green)
            MIN_BRIGHT + random.nextInt(60)            // 120-180 (softened blue)
        );
      case 4: // Purple-biased (bright purple with softened green)
        return new Color(
            MIN_SATURATION + 60 + random.nextInt(60),  // 180-220 (red)
            MIN_BRIGHT + random.nextInt(60),           // 120-180 (softened green)
            MIN_SATURATION + 80 + random.nextInt(40)   // 180-220 (blue)
        );
      case 5: // Cyan-biased (bright cyan with softened red)
        return new Color(
            MIN_BRIGHT + random.nextInt(60),           // 120-180 (softened red)
            MIN_SATURATION + 80 + random.nextInt(40),  // 180-220 (green)
            MIN_SATURATION + 80 + random.nextInt(40)   // 180-220 (blue)
        );
      case 6: // Orange-biased (bright orange with softened blue)
        return new Color(
            MIN_SATURATION + 80 + random.nextInt(40),  // 180-220 (red)
            MIN_SATURATION + 40 + random.nextInt(60),  // 160-220 (green)
            MIN_BRIGHT + random.nextInt(60)            // 120-180 (softened blue)
        );
      default: // Pink-biased (bright pink with softened green)
        return new Color(
            MIN_SATURATION + 80 + random.nextInt(40),  // 180-220 (red)
            MIN_BRIGHT + random.nextInt(60),           // 120-180 (softened green)
            MIN_SATURATION + 40 + random.nextInt(60)   // 160-220 (blue)
        );
    }
  }

  /**
   * Generates a bright yet soft color with a specific hue bias
   */
  public static Color generateBrightSoftColorWithHue(ColorHue hue) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    switch (hue) {
      case RED:
        return new Color(
            200 + random.nextInt(55),  // 200-255 (bright red)
            120 + random.nextInt(80),  // 120-200 (softened green)
            120 + random.nextInt(80)   // 120-200 (softened blue)
        );
      case BLUE:
        return new Color(
            120 + random.nextInt(80),  // 120-200 (softened red)
            140 + random.nextInt(80),  // 140-220 (medium green)
            200 + random.nextInt(55)   // 200-255 (bright blue)
        );
      case GREEN:
        return new Color(
            120 + random.nextInt(80),  // 120-200 (softened red)
            200 + random.nextInt(55),  // 200-255 (bright green)
            120 + random.nextInt(80)   // 120-200 (softened blue)
        );
      case YELLOW:
        return new Color(
            220 + random.nextInt(35),  // 220-255 (bright red)
            220 + random.nextInt(35),  // 220-255 (bright green)
            120 + random.nextInt(80)   // 120-200 (softened blue)
        );
      case PURPLE:
        return new Color(
            180 + random.nextInt(75),  // 180-255 (bright red)
            120 + random.nextInt(80),  // 120-200 (softened green)
            200 + random.nextInt(55)   // 200-255 (bright blue)
        );
      case ORANGE:
        return new Color(
            220 + random.nextInt(35),  // 220-255 (bright red)
            160 + random.nextInt(60),  // 160-220 (medium green)
            100 + random.nextInt(80)   // 100-180 (softened blue)
        );
      case CYAN:
        return new Color(
            120 + random.nextInt(80),  // 120-200 (softened red)
            200 + random.nextInt(55),  // 200-255 (bright green)
            200 + random.nextInt(55)   // 200-255 (bright blue)
        );
      case PINK:
        return new Color(
            220 + random.nextInt(35),  // 220-255 (bright red)
            140 + random.nextInt(80),  // 140-220 (medium green)
            180 + random.nextInt(75)   // 180-255 (bright blue)
        );
      default: // RANDOM
        return generateBrightSoftColor();
    }
  }

  /**
   * Creates a color using HSB color space for better control over brightness and saturation
   */
  public static Color generateHSBBasedColor() {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    // Generate hue (0.0-1.0), limited saturation (0.5-0.8), high brightness (0.7-0.9)
    float hue = random.nextFloat();
    float saturation = 0.5f + random.nextFloat() * 0.3f;  // 0.5-0.8
    float brightness = 0.7f + random.nextFloat() * 0.2f;  // 0.7-0.9

    return Color.getHSBColor(hue, saturation, brightness);
  }

  public static Color generateHSBBasedColor(ColorProfile profile) {
    final int MAX_ATTEMPTS = 25; // Maximum attempts to find a distinct color
    final float MIN_COLOR_DISTANCE = 0.15f; // Minimum HSB distance to consider colors distinct

    Color candidate;
    int attempts = 0;

    do {
      candidate = generateHSBBasedColor();
      attempts++;

      // Check if color is sufficiently distinct
      if (!isColorTooSimilar(candidate, profile, MIN_COLOR_DISTANCE)) {
        return candidate;
      }
    } while (attempts < MAX_ATTEMPTS);

    return candidate; // Return the last candidate if all attempts fail
  }

  /**
   * Checks if a candidate color is too similar to any existing color in the profile
   */
  private static boolean isColorTooSimilar(Color candidate, ColorProfile profile, float minDistance) {
    if (profile == null || profile.getColors() == null || profile.getColors().isEmpty()) {
      return false; // No existing colors to compare against
    }

    float[] candidateHSB = Color.RGBtoHSB(
        candidate.getRed(), candidate.getGreen(), candidate.getBlue(), null
    );

    for (Map.Entry<String, String> entry : profile.getColors().entrySet()) {
      try {
        Color existingColor = Color.decode(entry.getValue());
        float[] existingHSB = Color.RGBtoHSB(
            existingColor.getRed(), existingColor.getGreen(), existingColor.getBlue(), null
        );

        // Calculate HSB distance (simple Euclidean distance in HSB space)
        float distance = calculateHSBDistance(candidateHSB, existingHSB);

        if (distance < minDistance) {
          return true; // Colors are too similar
        }
      } catch (NumberFormatException e) {
        // Skip invalid color entries
        continue;
      }
    }

    return false; // No similar colors found
  }

  /**
   * Calculates distance between two colors in HSB space
   * HSB values are normalized (0.0-1.0), so we use simple Euclidean distance
   */
  private static float calculateHSBDistance(float[] hsb1, float[] hsb2) {
    // Weight hue more heavily since it's the most perceptually important
    float hueDiff = Math.min(Math.abs(hsb1[0] - hsb2[0]), 1.0f - Math.abs(hsb1[0] - hsb2[0]));
    float saturationDiff = Math.abs(hsb1[1] - hsb2[1]);
    float brightnessDiff = Math.abs(hsb1[2] - hsb2[2]);

    // Euclidean distance with hue weighted 2x more than saturation and brightness
    return (float) Math.sqrt(
        (4.0f * hueDiff * hueDiff) +
            (saturationDiff * saturationDiff) +
            (brightnessDiff * brightnessDiff)
    );
  }

  /**
   * Creates a color with one dominant channel and two supporting channels
   */
  public static Color generateDominantColor() {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    int dominantChannel = random.nextInt(3);
    int dominantValue = 180 + random.nextInt(76);  // 180-255 (bright)
    int supportValue = 120 + random.nextInt(80);   // 120-200 (soft)

    if (dominantChannel == 0) {
      return new Color(dominantValue, supportValue, supportValue); // Red dominant
    } else if (dominantChannel == 1) {
      return new Color(supportValue, dominantValue, supportValue); // Green dominant
    } else {
      return new Color(supportValue, supportValue, dominantValue); // Blue dominant
    }
  }

  /**
   * Enum for color hue preferences
   */
  public enum ColorHue {
    RED, BLUE, GREEN, YELLOW, PURPLE, ORANGE, CYAN, PINK, RANDOM
  }
}