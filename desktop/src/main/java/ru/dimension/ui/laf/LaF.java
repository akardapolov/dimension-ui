package ru.dimension.ui.laf;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.ChartPanel;
import ru.dimension.ui.component.chart.StackedChart;

@UtilityClass
@Log4j2
public class LaF {

  @Getter
  @Setter
  private static LaFType lafType = LaFType.DEFAULT;

  public void setLookAndFeel(LaFType lafType) {
    setLafType(lafType);

    LookAndFeelInfo laf = new LookAndFeelInfo("Metal", MetalLookAndFeel.class.getName());

    if (LaFType.DEFAULT.equals(lafType)) {
      laf = new LookAndFeelInfo("Metal", MetalLookAndFeel.class.getName());
    } else if (LaFType.LIGHT.equals(lafType)) {
      laf = new LookAndFeelInfo("Light", FlatLightLaf.class.getName());
    } else if (LaFType.DARK.equals(lafType)) {
      laf = new LookAndFeelInfo("Darcula", FlatDarculaLaf.class.getName());
    }

    try {
      UIManager.setLookAndFeel(laf.getClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
             UnsupportedLookAndFeelException e) {
      log.catching(e);
    }
  }

  public static void setBackgroundColor(LafColorGroup lafColorGroup,
                                        Component... components) {
    Color backgroundColor = getBackgroundColor(lafColorGroup, getLafType());

    for (Component component : components) {
      if (backgroundColor != null) {
        component.setBackground(backgroundColor);
      }
    }
  }

  public static void setBackgroundAndTextColorForStackedChartPanel(LafColorGroup lafColorGroup,
                                                                   StackedChart chart) {
    Color backgroundColor = getBackgroundColor(lafColorGroup, getLafType());
    Color legendBackgroundColor = getBackgroundColor(LafColorGroup.LEGEND_CHART_PANEL, getLafType());
    if (backgroundColor != null) {
      chart.setBackgroundAndTextColor(backgroundColor, legendBackgroundColor);
    }
  }

  public static void setBackgroundAndTextColorForChartPanel(LafColorGroup lafColorGroup,
                                                            ChartPanel chart) {
    Color backgroundColor = getBackgroundColor(lafColorGroup, getLafType());
    Color legendBackgroundColor = getBackgroundColor(LafColorGroup.LEGEND_CHART_PANEL, getLafType());
    if (backgroundColor != null) {
      chart.setBackgroundAndTextColor(backgroundColor, legendBackgroundColor);
    }
  }

  public static Color getBackgroundColor(LafColorGroup lafColorGroup,
                                         LaFType lafType) {
    switch (lafColorGroup) {
      case CONFIG -> {
        return switch (lafType) {
          case DEFAULT -> null;
          case LIGHT -> Color.gray;
          case DARK -> Color.darkGray;
        };
      }
      case CHART -> {
        return switch (lafType) {
          case DEFAULT -> null;
          case LIGHT -> Color.white;
          case DARK -> Color.black;
        };
      }
      case REPORT -> {
        return switch (lafType) {
          case DEFAULT -> null;
          case LIGHT -> Color.white;
          case DARK -> new Color(70, 73, 75);
        };
      }
      case LEGEND_CHART_PANEL, TEXTAREA -> {
        return switch (lafType) {
          case DEFAULT -> new Color(219, 219, 224);
          case LIGHT -> Color.white;
          case DARK -> Color.lightGray;
        };
      }
      case CHART_PANEL -> {
        return switch (lafType) {
          case DEFAULT, LIGHT -> null;
          case DARK -> new Color(70, 73, 75);
        };
      }
      case CONFIG_PANEL -> {
        return switch (lafType) {
          case DEFAULT, LIGHT -> null;
          case DARK -> new Color(55, 55, 59);
        };
      }
      case BORDER -> {
        return switch (lafType) {
          case DEFAULT -> Color.darkGray;
          case LIGHT -> Color.gray;
          case DARK -> new Color(89, 89, 93);
        };
      }
      case TABLE_BACKGROUND -> {
        return switch (lafType) {
          case DEFAULT -> Color.white;
          case LIGHT, DARK -> Color.lightGray;
        };
      }
      case TABLE_FONT -> {
        return switch (lafType) {
          case DEFAULT, LIGHT -> null;
          case DARK -> Color.black;
        };
      }
      case LABEL_FONT_COLOR -> {
        return switch (lafType) {
          case DEFAULT, LIGHT -> Color.black;
          case DARK -> new Color(176, 196, 222);
        };
      }
      case TABLE_FONT_COLOR, RANGE_SELECTED_FONT_COLOR -> {
        return switch (lafType) {
          case DEFAULT, LIGHT -> Color.black;
          case DARK -> Color.white;
        };
      }
      case RANGE_NOT_SELECTED_FONT_COLOR -> {
        return switch (lafType) {
          case DEFAULT, LIGHT -> Color.blue;
          case DARK -> new Color(100, 185, 250);
        };
      }
      case CHART_HISTORY_HOUR_FONT -> {
        return switch (lafType) {
          case DEFAULT, LIGHT -> Color.black;
          case DARK -> new Color(196, 189, 199);
        };
      }
      case CHART_HISTORY_DAY_FONT -> {
        return switch (lafType) {
          case DEFAULT, LIGHT -> new Color(113, 122, 222);
          case DARK -> new Color(122, 184, 245);
        };
      }
      case CHART_HISTORY_MONTH_FONT -> {
        return switch (lafType) {
          case DEFAULT, LIGHT -> Color.darkGray;
          case DARK -> Color.lightGray;
        };
      }
      case CHART_HISTORY_YEAR_FONT -> {
        return switch (lafType) {
          case DEFAULT, LIGHT -> new Color(25, 112, 61);
          case DARK -> new Color(147, 215, 175);
        };
      }
      default -> throw new IllegalArgumentException("Unexpected value: " + lafColorGroup);
    }
  }

  public static void setBackgroundConfigPanel(LafColorGroup lafColorGroup,
                                              JPanel panel) {
    Color backgroundColor = getBackgroundColor(lafColorGroup, getLafType());
    panel.setBackground(backgroundColor);
  }

  public static Color getColorBorder(LafColorGroup lafColorGroup) {
    return getBackgroundColor(lafColorGroup, getLafType());
  }

}
