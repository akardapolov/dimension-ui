package ru.dimension.ui.view.panel.config.query;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.view.panel.config.ButtonPanel;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class QueryPanel extends JPanel {

  private final ButtonPanel queryButtonPanel;
  private final MainQueryPanel mainQueryPanel;
  private final MetadataQueryPanel metadataQueryPanel;
  private final MetricQueryPanel metricQueryPanel;
  private final JSplitPane mainSplitPane;
  private final JTabbedPane rightTabbedPane;

  @Inject
  public QueryPanel(@Named("queryButtonPanel") ButtonPanel queryButtonPanel,
                    @Named("mainQueryPanel") MainQueryPanel mainQueryPanel,
                    @Named("metadataQueryPanel") MetadataQueryPanel metadataQueryPanel,
                    @Named("metricQueryPanel") MetricQueryPanel metricQueryPanel) {
    this.queryButtonPanel = queryButtonPanel;
    this.mainQueryPanel = mainQueryPanel;
    this.metadataQueryPanel = metadataQueryPanel;
    this.metricQueryPanel = metricQueryPanel;

    setLayout(new BorderLayout());

    rightTabbedPane = new JTabbedPane();
    rightTabbedPane.addTab("Table", metadataQueryPanel);
    rightTabbedPane.addTab("Metric", metricQueryPanel);
    rightTabbedPane.setMinimumSize(new Dimension(100, 100));
    rightTabbedPane.setBorder(GUIHelper.getGrayBorder());

    mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainQueryPanel, rightTabbedPane);
    mainSplitPane.setResizeWeight(0.5);
    mainSplitPane.setDividerSize(3);
    mainSplitPane.setContinuousLayout(true);

    JPanel content = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(content, PGHelper.getPGConfig(), false);
    gbl.row()
        .cellXRemainder(queryButtonPanel).fillX();
    gbl.row()
        .cellXYRemainder(mainSplitPane).fillXY();
    gbl.done();

    add(content, BorderLayout.CENTER);
  }
}