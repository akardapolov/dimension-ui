package ru.dimension.ui.helper;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import ru.dimension.ui.model.validation.ValidationSeverity;

public final class FieldHighlighter {

  public static final Color ERROR_COLOR = new Color(0xE5, 0x39, 0x35);
  public static final Color WARNING_COLOR = new Color(0xFB, 0x8C, 0x00);
  public static final Color OK_COLOR = new Color(0x43, 0xA0, 0x47);
  public static final Color NEUTRAL_COLOR = Color.GRAY;

  private FieldHighlighter() {
  }

  public static void apply(JComponent field, ValidationSeverity severity, String message) {
    Color color = switch (severity) {
      case ERROR -> ERROR_COLOR;
      case WARNING -> WARNING_COLOR;
      default -> NEUTRAL_COLOR;
    };
    int thickness = severity == ValidationSeverity.OK ? 1 : 2;
    field.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(color, thickness),
        BorderFactory.createEmptyBorder(2, 4, 2, 4)
    ));
    field.setToolTipText(message);
  }

  public static void clear(JComponent field) {
    apply(field, ValidationSeverity.OK, null);
  }

  public static String symbol(ValidationSeverity severity) {
    return switch (severity) {
      case ERROR -> "\u2716";
      case WARNING -> "\u26A0";
      default -> "\u2714";
    };
  }

  public static JLabel createSeverityIcon(ValidationSeverity severity) {
    JLabel label = new JLabel(symbol(severity));
    label.setForeground(switch (severity) {
      case ERROR -> ERROR_COLOR;
      case WARNING -> WARNING_COLOR;
      default -> OK_COLOR;
    });
    return label;
  }
}