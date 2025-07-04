package ru.dimension.ui.helper;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.log4j.Log4j2;
import org.rtv.Options;

@Log4j2
public class ColorHelper {

  private static int MIN = 0;
  private static int MAX = 255;

  public static Color getColor(String seriesName) {
    Color color = Options.getInstance().getColor(seriesName);
    if (color == null) {
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      color = new Color(threadLocalRandom.nextInt(MIN, MAX),
                        threadLocalRandom.nextInt(MIN, MAX),
                        threadLocalRandom.nextInt(MIN, MAX));
    }

    return color;
  }
}
