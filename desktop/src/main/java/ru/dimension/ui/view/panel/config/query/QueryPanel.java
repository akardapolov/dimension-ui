package ru.dimension.ui.view.panel.config.query;

import java.awt.event.KeyEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class QueryPanel extends JPanel {

  private final JTabbedPane mainQuery;
  private final MainQueryPanel mainQueryPanel;
  private final MetadataQueryPanel metadataQueryPanel;
  private final MetricQueryPanel metricQueryPanel;

  @Inject
  public QueryPanel(@Named("mainQueryTab") JTabbedPane mainQuery,
                    @Named("mainQueryPanel") MainQueryPanel mainQueryPanel,
                    @Named("metadataQueryPanel") MetadataQueryPanel metadataQueryPanel,
                    @Named("metricQueryPanel") MetricQueryPanel metricQueryPanel) {
    this.mainQueryPanel = mainQueryPanel;
    this.metadataQueryPanel = metadataQueryPanel;
    this.metricQueryPanel = metricQueryPanel;

    this.mainQuery = mainQuery;

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row()
        .cellXYRemainder(mainQuery).fillXY();
    gbl.done();

    this.mainQuery.add("Main", this.mainQueryPanel);
    this.mainQuery.setMnemonicAt(0, KeyEvent.VK_M);
    this.mainQuery.add("Metadata", this.metadataQueryPanel);
    this.mainQuery.setMnemonicAt(1, KeyEvent.VK_A);
    this.mainQuery.add("Metrics", this.metricQueryPanel);
    this.mainQuery.setMnemonicAt(2, KeyEvent.VK_R);
  }

}