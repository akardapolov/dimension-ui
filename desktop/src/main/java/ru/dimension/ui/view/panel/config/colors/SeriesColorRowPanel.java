package ru.dimension.ui.view.panel.config.colors;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import ru.dimension.ui.laf.LaF;

public class SeriesColorRowPanel extends JPanel {

  private static final int WRAP_THRESHOLD = 40;

  private final String seriesName;
  private final JLabel colorPreview;
  private final JTextArea nameArea;
  private final JTextField hexField;
  private final JButton pickerButton;
  private final JButton resetButton;

  public SeriesColorRowPanel(String seriesName, Color initialColor) {
    this.seriesName = seriesName;

    setLayout(new BorderLayout(8, 0));

    colorPreview = new JLabel("  ");
    colorPreview.setOpaque(true);
    colorPreview.setPreferredSize(new Dimension(24, 24));
    colorPreview.setMinimumSize(new Dimension(24, 24));
    colorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    updateColorPreview(initialColor);

    nameArea = new JTextArea(seriesName);
    nameArea.setOpaque(false);
    nameArea.setEditable(false);
    nameArea.setFocusable(false);
    nameArea.setLineWrap(true);
    nameArea.setWrapStyleWord(true);
    nameArea.setBorder(BorderFactory.createEmptyBorder());
    nameArea.setFont(new JLabel().getFont());
    nameArea.setHighlighter(null);

    hexField = new JTextField(colorToHex(initialColor), 8);
    hexField.setMaximumSize(new Dimension(80, 24));

    pickerButton = new JButton("...");
    pickerButton.setToolTipText("Choose color");
    pickerButton.setPreferredSize(new Dimension(32, 24));

    resetButton = new JButton("Reset");
    resetButton.setToolTipText("Reset to default/generated color");

    JPanel left = new JPanel(new BorderLayout(8, 0));
    left.add(colorPreview, BorderLayout.WEST);
    left.add(nameArea, BorderLayout.CENTER);

    JPanel right = new JPanel(new BorderLayout(4, 0));
    right.add(hexField, BorderLayout.CENTER);
    right.add(pickerButton, BorderLayout.EAST);

    add(left, BorderLayout.CENTER);
    add(right, BorderLayout.EAST);
    add(resetButton, BorderLayout.WEST);

    Border empty = BorderFactory.createEmptyBorder(2, 4, 2, 4);
    Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY);
    setBorder(BorderFactory.createCompoundBorder(line, empty));

    if (seriesName.length() <= WRAP_THRESHOLD) {
      setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
    }

    LaF.setBackgroundColor(CHART_PANEL, this, left, right);
    nameArea.setBackground(getBackground());
  }

  public String getSeriesName() {
    return seriesName;
  }

  public Color getColor() {
    try {
      return Color.decode(hexField.getText().trim());
    } catch (NumberFormatException e) {
      return colorPreview.getBackground();
    }
  }

  public void setColor(Color color) {
    SwingUtilities.invokeLater(() -> {
      updateColorPreview(color);
      hexField.setText(colorToHex(color));
    });
  }

  public String getHexValue() {
    return hexField.getText().trim();
  }

  public void addPickerListener(ActionListener listener) {
    pickerButton.addActionListener(listener);
  }

  public void addResetListener(ActionListener listener) {
    resetButton.addActionListener(listener);
  }

  public void addHexChangeListener(ActionListener listener) {
    hexField.addActionListener(listener);
  }

  private void updateColorPreview(Color color) {
    colorPreview.setBackground(color);
  }

  private static String colorToHex(Color color) {
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }
}