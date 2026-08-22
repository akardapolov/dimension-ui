package ru.dimension.ui.view.panel.config.colors;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SeriesColorEditorPanel extends JPanel {

  private static final int SCROLL_UNIT = 16;
  private static final int SCROLL_BLOCK = 100;

  private final JPanel rowsPanel;
  private final JLabel titleLabel;
  private final JTextField searchField;
  private final JButton addSeriesButton;
  private final JScrollPane scrollPane;

  private final Map<String, SeriesColorRowPanel> rowPanels = new HashMap<>();
  private final List<String> seriesOrder = new ArrayList<>();

  public SeriesColorEditorPanel() {
    setLayout(new BorderLayout());
    setBorder(new EtchedBorder());

    titleLabel = new JLabel("Series colors");
    titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 8));

    searchField = new JTextField();
    searchField.setToolTipText("Filter series by name");
    searchField.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
      @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
      @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
    });

    addSeriesButton = new JButton("Add series");
    addSeriesButton.setToolTipText("Add a series by name to assign a color");
    addSeriesButton.setEnabled(false);

    JPanel titleRow = new JPanel(new BorderLayout());
    titleRow.add(titleLabel, BorderLayout.WEST);
    titleRow.add(addSeriesButton, BorderLayout.EAST);

    JPanel searchRow = new JPanel(new BorderLayout(8, 0));
    searchRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
    searchRow.add(new JLabel("Search:"), BorderLayout.WEST);
    searchRow.add(searchField, BorderLayout.CENTER);

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    topPanel.add(titleRow, BorderLayout.NORTH);
    topPanel.add(searchRow, BorderLayout.CENTER);

    rowsPanel = new JPanel();
    rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));

    scrollPane = new JScrollPane(rowsPanel);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT);
    scrollPane.getVerticalScrollBar().setBlockIncrement(SCROLL_BLOCK);

    add(topPanel, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);

    setPreferredSize(new Dimension(480, 400));
  }

  public void setTitle(String title) {
    SwingUtilities.invokeLater(() -> titleLabel.setText(title));
  }

  public void setAddSeriesEnabled(boolean enabled) {
    SwingUtilities.invokeLater(() -> addSeriesButton.setEnabled(enabled));
  }

  public void clearRows() {
    SwingUtilities.invokeLater(() -> {
      rowPanels.clear();
      seriesOrder.clear();
      rowsPanel.removeAll();
      rowsPanel.revalidate();
      rowsPanel.repaint();
    });
  }

  public void addSeriesRow(String seriesName, Color color,
                           ActionListener colorChangeListener,
                           ActionListener resetListener) {
    SwingUtilities.invokeLater(() -> {
      if (rowPanels.containsKey(seriesName)) {
        return;
      }

      SeriesColorRowPanel row = new SeriesColorRowPanel(seriesName, color);

      row.addPickerListener(e -> {
        Color newColor = JColorChooser.showDialog(
            SeriesColorEditorPanel.this,
            "Choose color for " + seriesName,
            row.getColor()
        );
        if (newColor != null) {
          row.setColor(newColor);
          colorChangeListener.actionPerformed(e);
        }
      });
      row.addHexChangeListener(colorChangeListener);
      row.addResetListener(resetListener);

      rowPanels.put(seriesName, row);
      seriesOrder.add(seriesName);

      if (matchesFilter(seriesName)) {
        rowsPanel.add(row);
        rowsPanel.add(Box.createVerticalStrut(2));
      }

      rowsPanel.revalidate();
      rowsPanel.repaint();
    });
  }

  public void removeSeriesRow(String seriesName) {
    SwingUtilities.invokeLater(() -> {
      SeriesColorRowPanel row = rowPanels.remove(seriesName);
      seriesOrder.remove(seriesName);
      if (row != null) {
        rowsPanel.remove(row);
        rowsPanel.revalidate();
        rowsPanel.repaint();
      }
    });
  }

  public Map<String, Color> getSeriesColors() {
    Map<String, Color> colors = new HashMap<>();
    rowPanels.forEach((name, row) -> colors.put(name, row.getColor()));
    return colors;
  }

  public void setRowColor(String seriesName, Color color) {
    SwingUtilities.invokeLater(() -> {
      SeriesColorRowPanel row = rowPanels.get(seriesName);
      if (row != null) {
        row.setColor(color);
      }
    });
  }

  public void showAddSeriesDialog(ActionListener onAddListener) {
    String name = JOptionPane.showInputDialog(
        this, "Enter series name:", "Add series", JOptionPane.PLAIN_MESSAGE
    );
    if (name == null || name.isBlank()) {
      return;
    }
    String trimmed = name.trim();
    if (rowPanels.containsKey(trimmed)) {
      JOptionPane.showMessageDialog(this, "Series already exists", "Warning", JOptionPane.WARNING_MESSAGE);
      return;
    }
    onAddListener.actionPerformed(new java.awt.event.ActionEvent(this, 0, trimmed));
  }

  public void addAddSeriesListener(ActionListener listener) {
    addSeriesButton.addActionListener(listener);
  }

  private void applyFilter() {
    SwingUtilities.invokeLater(() -> {
      String filter = searchField.getText().trim().toLowerCase();
      rowsPanel.removeAll();
      for (String seriesName : seriesOrder) {
        if (matchesFilter(seriesName, filter)) {
          SeriesColorRowPanel row = rowPanels.get(seriesName);
          if (row != null) {
            rowsPanel.add(row);
            rowsPanel.add(Box.createVerticalStrut(2));
          }
        }
      }
      rowsPanel.revalidate();
      rowsPanel.repaint();
    });
  }

  private boolean matchesFilter(String seriesName) {
    return matchesFilter(seriesName, searchField.getText().trim().toLowerCase());
  }

  private boolean matchesFilter(String seriesName, String filter) {
    if (filter.isEmpty()) return true;
    return seriesName.toLowerCase().contains(filter);
  }
}