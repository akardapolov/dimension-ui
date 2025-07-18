package ru.dimension.ui.view.structure.adhoc;

import dagger.Lazy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.BaseFrame;
import ru.dimension.ui.view.handler.adhoc.AdHocSelectionHandler;
import ru.dimension.ui.view.panel.adhoc.AdHocPanel;
import ru.dimension.ui.view.structure.AdHocView;

@Log4j2
@Singleton
public class AdHocViewImpl extends JPanel implements AdHocView {

  private final Lazy<BaseFrame> jFrame;
  private final Lazy<AdHocPresenter> adHocPresenter;
  private final Lazy<AdHocPanel> adHocPanel;
  private final JSplitPane adHocMain;
  private final JXTableCase connectionCase;
  private final JComboBox<String> schemaCatalogCBox;
  private final JTabbedPane tableViewPane;
  private final JTabbedPane timeStampColumnMetricPane;
  private final JXTableCase viewCase;
  private final JXTableCase tableCase;
  private final JXTableCase timestampCase;
  private final JXTableCase metricCase;
  private final JXTableCase columnCase;
  private final AdHocSelectionHandler adHocSelectionHandler;

  @Inject
  public AdHocViewImpl(Lazy<BaseFrame> jFrame,
                       @Named("adHocPanel") Lazy<AdHocPanel> adHocPanel,
                       @Named("adHocPresenter") Lazy<AdHocPresenter> adHocPresenter,
                       @Named("connectionAdHocCase") JXTableCase connectionCase,
                       @Named("schemaCatalogAdHocCBox") JComboBox<String> schemaCatalogCBox,
                       @Named("tableAdHocCase") JXTableCase tableCase,
                       @Named("viewAdHocCase") JXTableCase viewCase,
                       @Named("timestampAdHocCase") JXTableCase timestampCase,
                       @Named("metricAdHocCase") JXTableCase metricCase,
                       @Named("columnAdHocCase") JXTableCase columnCase,
                       @Named("adHocPanelHandler") AdHocSelectionHandler adHocSelectionHandler) {

    this.jFrame = jFrame;
    this.adHocPanel = adHocPanel;
    this.adHocPresenter = adHocPresenter;

    this.connectionCase = connectionCase;
    this.schemaCatalogCBox = schemaCatalogCBox;
    this.tableCase = tableCase;
    this.viewCase = viewCase;
    this.timestampCase = timestampCase;
    this.metricCase = metricCase;
    this.columnCase = columnCase;

    this.tableViewPane = new JTabbedPane();
    this.tableViewPane.addTab("Table", this.tableCase.getJScrollPane());
    this.tableViewPane.addTab("View", this.viewCase.getJScrollPane());

    this.timeStampColumnMetricPane = new JTabbedPane();
    this.timeStampColumnMetricPane.addTab("Timestamp", timestampCase.getJScrollPane());
    this.timeStampColumnMetricPane.addTab("Column", columnCase.getJScrollPane());
    this.timeStampColumnMetricPane.addTab("Metric", metricCase.getJScrollPane());

    this.adHocSelectionHandler = adHocSelectionHandler;

    this.adHocMain = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 240);

    this.adHocMain.setLeftComponent(fillEntitiesPane());
    this.adHocMain.setRightComponent(this.adHocPanel.get());

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(adHocMain).fillXY();
    gbl.done();
  }

  private JPanel fillEntitiesPane() {
    JPanel panel = new JPanel();
    LaF.setBackgroundConfigPanel(LafColorGroup.CONFIG_PANEL, panel);
    panel.setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(panel, PGHelper.getPGConfig(), false);

    gbl.row()
        .cell(new JXTitledSeparator("Connection")).fillX();
    gbl.row()
        .cell(connectionCase.getJScrollPane()).fillX();
    gbl.row()
        .cell(new JXTitledSeparator("Schema/Catalog")).fillX();
    gbl.row()
        .cell(schemaCatalogCBox).fillX();
    gbl.row()
        .cell(tableViewPane).fillX();
    gbl.row()
        .cell(timeStampColumnMetricPane).fillXY();

    gbl.done();

    return panel;
  }


  @Override
  public void bindPresenter() {
    this.adHocPresenter.get().fillModel(Connection.class);
    connectionCase.getJxTable().getColumnModel().getColumn(0).setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());

  }

  @Override
  public void showAdHoc() {
    this.setVisible(true);
  }
}
