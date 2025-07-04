package ru.dimension.ui.view.structure.template;

import dagger.Lazy;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jdesktop.swingx.JXTextArea;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.view.BaseFrame;
import ru.dimension.ui.view.structure.TemplateView;
import ru.dimension.ui.view.tab.ConnTypeTab;
import ru.dimension.ui.view.panel.template.TemplateConnPanel;
import ru.dimension.ui.view.panel.template.TemplateHTTPConnPanel;

@Log4j2
@Singleton
public class TemplateViewImpl extends JDialog implements TemplateView {

  private final Lazy<BaseFrame> jFrame;
  private final Lazy<TemplatePresenter> templatePresenter;

  private final JButton templateLoadJButton;

  private final JXTableCase templateTaskCase;
  private final JXTableCase templateConnCase;
  private final JXTableCase templateQueryCase;

  private final JPanel jPanelLine;
  private final JPanel jPanel0Line;
  private final JPanel jPanel1Line;
  private final JPanel jPanel2Line;
  private final JPanel jPanel3Line;

  private final TemplateConnPanel templateConnPanel;
  private final TemplateHTTPConnPanel templateHTTPConnPanel;
  private final JXTableCase templateMetricsCase;

  private final JXTextArea taskDescription;
  private final JXTextArea queryDescription;
  private final RSyntaxTextArea queryText;
  private final JTabbedPane connectionTabPane;

  @Inject
  public TemplateViewImpl(Lazy<BaseFrame> jFrame,
                          Lazy<TemplatePresenter> templatePresenter,
                          @Named("templateLoadJButton") JButton templateLoadJButton,
                          @Named("templateTaskCase") JXTableCase templateTaskCase,
                          @Named("templateConnCase") JXTableCase templateConnCase,
                          @Named("templateQueryCase") JXTableCase templateQueryCase,
                          @Named("templateConnPanel") TemplateConnPanel templateConnPanel,
                          @Named("templateHTTPConnPanel") TemplateHTTPConnPanel templateHTTPConnPanel,
                          @Named("templateConnectionTab") ConnTypeTab connectionTabPane,
                          @Named("templateMetricsCase") JXTableCase templateMetricsCase,
                          @Named("templateTaskDescription") JXTextArea taskDescription,
                          @Named("templateQueryDescription") JXTextArea queryDescription,
                          @Named("templateQueryText") RSyntaxTextArea queryText) {
    this.jFrame = jFrame;
    this.templatePresenter = templatePresenter;

    this.templateLoadJButton = templateLoadJButton;

    this.templateTaskCase = templateTaskCase;
    this.templateConnCase = templateConnCase;
    this.templateQueryCase = templateQueryCase;

    this.templateConnPanel = templateConnPanel;
    this.templateHTTPConnPanel = templateHTTPConnPanel;
    this.templateMetricsCase = templateMetricsCase;

    this.taskDescription = taskDescription;
    this.queryDescription = queryDescription;
    this.queryText = queryText;

    this.connectionTabPane = connectionTabPane;
    this.connectionTabPane.add(this.templateConnPanel, ConnectionTypeTabPane.JDBC.getName());
    this.connectionTabPane.add(this.templateHTTPConnPanel, ConnectionTypeTabPane.HTTP.getName());

    this.templateTaskCase.getJxTable().getColumnExt(0).setVisible(false);
    this.templateConnCase.getJxTable().getColumnExt(0).setVisible(false);
    this.templateQueryCase.getJxTable().getColumnExt(0).setVisible(false);

    this.jPanelLine = new JPanel(new GridLayout(1, 7, 5, 5));
    this.jPanel0Line = new JPanel(new GridLayout(1, 3, 5, 5));
    this.jPanel1Line = new JPanel(new GridLayout(1, 3, 3, 3));
    this.jPanel2Line = new JPanel(new GridLayout(1, 3, 3, 3));
    this.jPanel3Line = new JPanel(new GridLayout(1, 2, 3, 3));

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row()
        .cellXYRemainder(fillContentPane()).fillXY();

    gbl.done();

    this.setTitle("Templates");

    this.packTemplate(false);
  }

  private JPanel fillContentPane() {
    JPanel panelSettings = new JPanel();
    panelSettings.setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(panelSettings, PGHelper.getPGConfig(), false);

    jPanelLine.add(templateLoadJButton);
    jPanelLine.add(new JLabel());
    jPanelLine.add(new JLabel());
    jPanelLine.add(new JLabel());
    jPanelLine.add(new JLabel());
    jPanelLine.add(new JLabel());
    jPanelLine.add(new JLabel());

    jPanel0Line.add(new JXTitledSeparator("Task"));
    jPanel0Line.add(new JXTitledSeparator("Connection"));
    jPanel0Line.add(new JXTitledSeparator("Query"));

    jPanel1Line.add(this.templateTaskCase.getJScrollPane());
    jPanel1Line.add(this.templateConnCase.getJScrollPane());
    jPanel1Line.add(this.templateQueryCase.getJScrollPane());

    jPanel2Line.add(fillTaskPanel());
    jPanel2Line.add(fillConnPanel());
    jPanel2Line.add(fillQueryPanel());

    jPanel3Line.setLayout(new GridLayout(1, 2, 3, 3));
    jPanel3Line.add(fillSqlText());
    jPanel3Line.add(fillMetrics());

    gbl.row().cell(jPanelLine).fillX();
    gbl.row().cell(jPanel0Line).fillX();
    gbl.row().cell(jPanel1Line).fillXY();
    gbl.row().cell(jPanel2Line).fillX();
    gbl.row().cellXYRemainder(jPanel3Line).fillXY();

    gbl.done();

    return panelSettings;
  }

  private JPanel fillTaskPanel() {
    JPanel jPanel = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(jPanel, PGHelper.getPGConfig(), false);

    gbl.row()
        .cellXRemainder(new JXTitledSeparator("Description")).fillX();
    gbl.row()
        .cellXYRemainder(new JScrollPane(taskDescription)).fillXY();
    gbl.done();

    return jPanel;
  }

  private JPanel fillQueryPanel() {
    JPanel jPanel = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(jPanel, PGHelper.getPGConfig(), false);

    gbl.row()
        .cell(new JXTitledSeparator("Description")).fillX();
    gbl.row()
        .cellXYRemainder(new JScrollPane(queryDescription)).fillXY();

    gbl.done();

    return jPanel;
  }

  private JPanel fillConnPanel() {
    JPanel jPanel = new JPanel();

    PainlessGridBag gbl = new PainlessGridBag(jPanel, PGHelper.getPGConfig(), false);

    gbl.row()
        .cell(new JXTitledSeparator("Connection details")).fillX();
    gbl.row()
        .cellXYRemainder(connectionTabPane).fillXY();

    gbl.done();
    return jPanel;
  }

  private JPanel fillSqlText() {
    JPanel jPanel = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(jPanel, PGHelper.getPGConfig(), false);

    gbl.row()
        .cell(new JXTitledSeparator("SQL text")).fillX();
    gbl.row()
        .cellXYRemainder(new JScrollPane(queryText)).fillXY();

    gbl.done();
    return jPanel;
  }

  private JPanel fillMetrics() {
    JPanel jPanel = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(jPanel, PGHelper.getPGConfig(), false);

    gbl.row()
        .cell(new JXTitledSeparator("Metrics")).fillX();
    gbl.row()
        .cellXYRemainder(templateMetricsCase.getJScrollPane()).fillXY();

    gbl.done();
    return jPanel;
  }

  @Override
  public void bindPresenter() {
    addWindowListener(templatePresenter.get());

    this.templatePresenter.get().fillModel(Task.class);
    this.templatePresenter.get().fillModel(Connection.class);
    this.templatePresenter.get().fillModel(Query.class);
  }

  @Override
  public void showTemplate() {
    this.packTemplate(true);
  }

  @Override
  public void hideTemplate() {
    this.packTemplate(false);
  }

  private void packTemplate(boolean visible) {
    this.setVisible(visible);
    this.setModal(true);
    this.setResizable(true);
    this.pack();

    this.setSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width - 400,
                               Toolkit.getDefaultToolkit().getScreenSize().height - 100));
    this.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - getWidth() / 2,
                     (Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - getHeight() / 2);
  }

}
