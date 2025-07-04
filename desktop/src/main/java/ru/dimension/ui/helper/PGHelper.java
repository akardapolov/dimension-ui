package ru.dimension.ui.helper;

import javax.swing.JComponent;
import lombok.extern.log4j.Log4j2;
import org.painlessgridbag.PainlessGridBag;
import org.painlessgridbag.PainlessGridbagConfiguration;

@Log4j2
public class PGHelper {

  private PGHelper() {
  }

  public static PainlessGridbagConfiguration getPGConfig() {
    PainlessGridbagConfiguration config = new PainlessGridbagConfiguration();
    config.setFirstColumnLeftSpacing(5);
    config.setFirstRowTopSpacing(5);
    config.setLastColumnRightSpacing(5);
    config.setLastRowBottomSpacing(5);
    return config;
  }

  public static PainlessGridbagConfiguration getPGConfig(int spacing) {
    PainlessGridbagConfiguration config = new PainlessGridbagConfiguration();
    config.setFirstColumnLeftSpacing(spacing);
    config.setFirstRowTopSpacing(spacing);
    config.setLastColumnRightSpacing(spacing);
    config.setLastRowBottomSpacing(spacing);
    return config;
  }

  public static void cellXYRemainder(java.awt.Container container,
                                     javax.swing.JComponent component,
                                     boolean debug) {
    container.removeAll();

    PainlessGridBag gbl = new PainlessGridBag(container, getPGConfig(), debug);
    gbl.row().cellXYRemainder(component).fillXY();
    gbl.done();
  }

  public static void cellXYRemainder(java.awt.Container container,
                                     javax.swing.JComponent component,
                                     int spacing,
                                     boolean debug) {
    container.removeAll();

    PainlessGridBag gbl = new PainlessGridBag(container, getPGConfig(spacing), debug);
    gbl.row().cellXYRemainder(component).fillXY();
    gbl.done();
  }

  public static void setConstrainsInsets(PainlessGridBag gbl,
                                         JComponent component,
                                         int value) {
    gbl.constraints(component).insets.top = value;
    gbl.constraints(component).insets.bottom = value;
    gbl.constraints(component).insets.left = value;
    gbl.constraints(component).insets.right = value;
  }

  public static void setConstrainsInsets(PainlessGridBag gbl,
                                         JComponent component,
                                         int topBottom,
                                         int leftRight) {
    gbl.constraints(component).insets.top = topBottom;
    gbl.constraints(component).insets.bottom = topBottom;
    gbl.constraints(component).insets.left = leftRight;
    gbl.constraints(component).insets.right = leftRight;
  }
}
