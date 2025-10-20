package ru.dimension.ui.component.module.adhoc.config;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTitledSeparator;
import ru.dimension.ui.component.panel.CollapseCardPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.laf.LaF;

@Log4j2
public class AdHocConfigView extends JPanel {

  @Getter
  private final HistoryRangePanel historyPanel;
  @Getter
  private final LegendPanel legendPanel;
  @Getter
  private final CollapseCardPanel collapseCardPanel;

  public AdHocConfigView() {
    this.historyPanel = new HistoryRangePanel();
    this.legendPanel = new LegendPanel();
    this.collapseCardPanel = new CollapseCardPanel();

    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    initComponents();
  }

  private void initComponents() {
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    add(createSection("Range", historyPanel));
    add(Box.createRigidArea(new Dimension(10, 0)));
    add(createSection("Legend", legendPanel));
    add(Box.createRigidArea(new Dimension(10, 0)));
    add(createSection("Dashboard", collapseCardPanel));
    add(Box.createHorizontalGlue());
  }

  private JPanel createSection(String title, JPanel content) {
    JPanel sectionPanel = new JPanel();
    sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
    sectionPanel.setOpaque(false);

    JXTitledSeparator separator = new JXTitledSeparator(title);
    separator.setAlignmentX(LEFT_ALIGNMENT);

    content.setAlignmentX(LEFT_ALIGNMENT);

    sectionPanel.add(separator);
    sectionPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    sectionPanel.add(content);

    return sectionPanel;
  }
}