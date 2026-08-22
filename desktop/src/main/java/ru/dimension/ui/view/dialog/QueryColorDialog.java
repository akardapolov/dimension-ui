package ru.dimension.ui.view.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import lombok.extern.log4j.Log4j2;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.helper.ColorHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.view.panel.config.colors.SeriesColorEditorPanel;

@Log4j2
public class QueryColorDialog extends JDialog {

  private final ProfileTaskQueryKey key;
  private final String colorProfileName;
  private final ColorHelper colorHelper;
  private final MessageBroker broker;
  private final SeriesColorEditorPanel editorPanel;

  public QueryColorDialog(Window owner, ProfileTaskQueryKey key, QueryInfo queryInfo) {
    super(owner, "Colors: " + (queryInfo != null ? queryInfo.getName() : "query"), ModalityType.APPLICATION_MODAL);

    this.key = key;
    this.colorProfileName = key.getColorProfileName();
    this.colorHelper = ServiceLocator.get(ColorHelper.class);
    this.broker = MessageBroker.getInstance();

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout());
    setMinimumSize(new Dimension(540, 420));
    setSize(new Dimension(640, 480));
    setLocationRelativeTo(owner);

    editorPanel = new SeriesColorEditorPanel();
    editorPanel.setTitle("Series colors");
    editorPanel.setAddSeriesEnabled(true);
    editorPanel.addAddSeriesListener(e -> editorPanel.showAddSeriesDialog(this::handleAddSeries));

    loadSeriesColors();

    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(e -> dispose());

    JPanel bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.add(closeButton, BorderLayout.EAST);

    add(editorPanel, BorderLayout.CENTER);
    add(bottomPanel, BorderLayout.SOUTH);
  }

  public static void show(java.awt.Component owner, ProfileTaskQueryKey key, QueryInfo queryInfo) {
    Window window = owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
    QueryColorDialog dialog = new QueryColorDialog(window, key, queryInfo);
    dialog.setVisible(true);
  }

  private void loadSeriesColors() {
    Map<String, Color> colorMap = colorHelper.getColorMap(colorProfileName);
    List<String> seriesNames = colorMap.keySet().stream()
        .sorted(Comparator.comparing(String::toLowerCase))
        .collect(Collectors.toList());

    for (String seriesName : seriesNames) {
      addSeriesRow(seriesName, colorMap.get(seriesName));
    }
  }

  private void addSeriesRow(String seriesName, Color color) {
    ActionListener colorChangeListener = e -> handleColorChanged(seriesName);
    ActionListener resetListener = e -> handleResetColor(seriesName);
    editorPanel.addSeriesRow(seriesName, color, colorChangeListener, resetListener);
  }

  private void handleColorChanged(String seriesName) {
    Color newColor = editorPanel.getSeriesColors().get(seriesName);
    if (newColor == null) {
      return;
    }
    colorHelper.setColor(colorProfileName, seriesName, newColor);
    notifyColorSchemeChanged();
  }

  private void handleResetColor(String seriesName) {
    colorHelper.resetColorToDefault(colorProfileName, seriesName);
    Color newColor = colorHelper.getColor(colorProfileName, seriesName);
    editorPanel.setRowColor(seriesName, newColor);
    notifyColorSchemeChanged();
  }

  private void handleAddSeries(ActionEvent event) {
    String seriesName = event.getActionCommand();
    if (seriesName == null || seriesName.isBlank()) {
      return;
    }
    if (editorPanel.getSeriesColors().containsKey(seriesName)) {
      JOptionPane.showMessageDialog(this, "Series already exists", "Warning", JOptionPane.WARNING_MESSAGE);
      return;
    }

    Color color = colorHelper.getColor(colorProfileName, seriesName);
    colorHelper.setColor(colorProfileName, seriesName, color);
    addSeriesRow(seriesName, color);
    notifyColorSchemeChanged();
  }

  private void notifyColorSchemeChanged() {
    try {
      for (Component targetComponent : Component.values()) {
        if (Component.CONFIGURATION.equals(targetComponent) || Component.PLAYGROUND.equals(targetComponent)) {
          continue;
        }
        Message message = Message.builder()
            .destination(Destination.withDefault(targetComponent, Module.CHARTS))
            .action(Action.COLOR_SCHEME_CHANGED)
            .parameter("key", key)
            .build();
        broker.sendMessage(message);
      }
    } catch (Exception e) {
      log.error("Failed to send color scheme changed message", e);
    }
  }

}
