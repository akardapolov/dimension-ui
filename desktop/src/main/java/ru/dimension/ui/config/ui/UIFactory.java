package ru.dimension.ui.config.ui;

import static ru.dimension.ui.model.view.TemplateAction.LOAD;
import static ru.dimension.ui.model.view.TemplateAction.SAVE;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JComboBox;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTextArea;
import javax.swing.table.TableColumn;
import ru.dimension.di.ServiceLocator;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swingx.JXTableTables;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.column.ConnectionColumnNames;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.custom.DetailedComboBox;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.view.panel.config.query.MainQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetadataQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetricQueryPanel;
import ru.dimension.ui.view.panel.config.task.MultiSelectQueryPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.ConnectionTemplateRow;
import ru.dimension.ui.view.table.row.Rows.MetadataRow;
import ru.dimension.ui.view.table.row.Rows.TemplateConnectionRow;
import ru.dimension.ui.view.table.row.Rows.TemplateMetricRow;
import ru.dimension.ui.view.table.row.Rows.TemplateQueryRow;
import ru.dimension.ui.view.table.row.Rows.TemplateTaskRow;

public final class UIFactory {

  private UIFactory() {
  }

  public static ConfigTab createConfigTab() {
    return new ConfigTab(
        ServiceLocator.get(JXTableCase.class, "profileConfigCase"),
        ServiceLocator.get(JXTableCase.class, "taskConfigCase"),
        ServiceLocator.get(JXTableCase.class, "connectionConfigCase"),
        ServiceLocator.get(JXTableCase.class, "queryConfigCase")
    );
  }

  public static DetailedComboBox createTaskConnectionComboBox() {
    String[] columns = new String[]{"Name", "Username", "Url", "Jar", "Driver", "Type"};
    int[] widths = new int[]{131, 131, 132, 132, 132, 131};
    return new DetailedComboBox(columns, widths, 0);
  }

  public static DetailedComboBox createQueryConnectionMetadataComboBox() {
    String[] columns = new String[]{"Name", "Username", "Url", "Jar", "Driver", "Type"};
    int[] widths = new int[]{131, 131, 132, 132, 132, 131};
    DetailedComboBox detailedComboBox = new DetailedComboBox(columns, widths, 0);
    detailedComboBox.setPreferredSize(new Dimension(120, 26));
    return detailedComboBox;
  }

  public static RSyntaxTextArea createQuerySqlText() {
    RSyntaxTextArea textArea = new RSyntaxTextArea(5, 60);
    textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
    textArea.setCodeFoldingEnabled(true);
    textArea.setBorder(GUIHelper.getGrayBorder());
    textArea.setBackground(LaF.getBackgroundColor(LafColorGroup.TEXTAREA, LaF.getLafType()));
    return textArea;
  }

  public static DetailedComboBox createTimestampComboBox() {
    return new DetailedComboBox(new String[]{"Name", "Storage type"}, new int[]{120, 120}, 0);
  }

  public static MainQueryPanel createMainQueryPanel() {
    return new MainQueryPanel(
        ServiceLocator.get(ButtonPanel.class, "queryButtonPanel"),
        ServiceLocator.get(JComboBox.class, "queryGatherDataComboBox"),
        ServiceLocator.get(RSyntaxTextArea.class, "querySqlText")
    );
  }

  public static MetadataQueryPanel createMetadataQueryPanel() {
    return new MetadataQueryPanel(
        ServiceLocator.get(JXTableCase.class, "configMetadataCase"),
        ServiceLocator.get(DetailedComboBox.class, "queryConnectionMetadataComboBox"),
        ServiceLocator.get(DetailedComboBox.class, "timestampComboBox"),
        ServiceLocator.get(JComboBox.class, "tableType"),
        ServiceLocator.get(JComboBox.class, "indexType")
    );
  }

  public static MetricQueryPanel createMetricQueryPanel() {
    return new MetricQueryPanel(
        ServiceLocator.get(ButtonPanel.class, "metricQueryButtonPanel"),
        ServiceLocator.get(JXTableCase.class, "configMetricCase"),
        ServiceLocator.get(JComboBox.class, "groupFunction"),
        ServiceLocator.get(JComboBox.class, "chartType")
    );
  }

  private static TTRegistry getSharedRegistry() {
    return TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();
  }

  private static void configureStandardJXTable(JXTable table) {
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(Color.GRAY);
    table.setIntercellSpacing(new Dimension(1, 1));
    table.getTableHeader().setVisible(true);
  }

  public static TTTable<TemplateTaskRow, JXTable> createTemplateTaskCase() {
    TTTable<TemplateTaskRow, JXTable> tt = JXTableTables.create(
        getSharedRegistry(),
        TemplateTaskRow.class,
        TableUi.<TemplateTaskRow>builder()
            .rowIcon(ModelIconProviders.forTemplateTaskRow())
            .rowIconInColumn("name")
            .build()
    );
    configureStandardJXTable(tt.table());
    return tt;
  }

  public static JXTextArea createTemplateTaskDescription() {
    JXTextArea taskDescription = new JXTextArea("Task description...");
    taskDescription.setEditable(false);
    taskDescription.setBorder(GUIHelper.getGrayBorder());
    taskDescription.setRows(12);
    taskDescription.setColumns(1);
    taskDescription.setLineWrap(true);
    return taskDescription;
  }

  public static JXTextArea createTemplateQueryDescription() {
    JXTextArea queryDescription = new JXTextArea("Query description...");
    queryDescription.setEditable(false);
    queryDescription.setBorder(GUIHelper.getGrayBorder());
    queryDescription.setRows(12);
    queryDescription.setColumns(1);
    queryDescription.setLineWrap(true);
    return queryDescription;
  }

  public static JXTextArea createTemplateProfileDescSave() {
    JXTextArea profileDescription = new JXTextArea("Profile description");
    profileDescription.setRows(6);
    profileDescription.setColumns(1);
    profileDescription.setLineWrap(true);
    return profileDescription;
  }

  public static JXTextArea createTemplateTaskDescSave() {
    JXTextArea taskDescription = new JXTextArea("Task description");
    taskDescription.setRows(6);
    taskDescription.setColumns(1);
    taskDescription.setLineWrap(true);
    return taskDescription;
  }

  public static RSyntaxTextArea createTemplateQueryText() {
    RSyntaxTextArea textArea = new RSyntaxTextArea(15, 1);
    textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
    textArea.setCodeFoldingEnabled(true);
    textArea.setBorder(GUIHelper.getGrayBorder());
    textArea.setBackground(LaF.getBackgroundColor(LafColorGroup.TEXTAREA, LaF.getLafType()));
    textArea.setLineWrap(true);
    textArea.setAutoscrolls(true);
    return textArea;
  }

  public static TTTable<TemplateConnectionRow, JXTable> createTemplateConnCase() {
    TTTable<TemplateConnectionRow, JXTable> tt = JXTableTables.create(
        getSharedRegistry(),
        TemplateConnectionRow.class,
        TableUi.<TemplateConnectionRow>builder()
            .rowIcon(ModelIconProviders.forTemplateConnectionRow())
            .rowIconInColumn("name")
            .build()
    );
    configureStandardJXTable(tt.table());
    return tt;
  }

  public static TTTable<TemplateQueryRow, JXTable> createTemplateQueryCase() {
    TTTable<TemplateQueryRow, JXTable> tt = JXTableTables.create(
        getSharedRegistry(),
        TemplateQueryRow.class,
        TableUi.<TemplateQueryRow>builder()
            .rowIcon(ModelIconProviders.forTemplateQueryRow())
            .rowIconInColumn("name")
            .build()
    );
    configureStandardJXTable(tt.table());
    return tt;
  }

  public static JButton createTemplateLoadJButton() {
    JButton jButton = new JButton();
    jButton.setActionCommand(LOAD.name());
    jButton.setMnemonic('L');
    jButton.setText("Load");
    jButton.setPreferredSize(new Dimension(120, 30));
    return jButton;
  }

  public static JButton createTemplateSaveJButton() {
    JButton jButton = new JButton();
    jButton.setActionCommand(SAVE.name());
    jButton.setMnemonic('S');
    jButton.setText("Save");
    jButton.setPreferredSize(new Dimension(120, 30));
    return jButton;
  }

  public static JXTableCase createListCase() {
    return GUIHelper.getJXTableCase(4, new String[]{
        ConnectionColumnNames.ID.getColName(),
        ConnectionColumnNames.NAME.getColName()
    });
  }

  public static JXTableCase createSelectedCase() {
    return GUIHelper.getJXTableCase(4, new String[]{
        ConnectionColumnNames.ID.getColName(),
        ConnectionColumnNames.NAME.getColName()
    });
  }

  public static JXTableCase createConfigMetadataCase() {
    TTRegistry registry = TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();

    TTTable<MetadataRow, JXTable> tt = JXTableTables.create(
        registry,
        MetadataRow.class,
        TableUi.<MetadataRow>builder()
            .rowIcon(ModelIconProviders.forMetadataRow())
            .rowIconInColumn("colName")
            .build()
    );

    JXTable table = tt.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(Color.GRAY);
    table.setIntercellSpacing(new Dimension(1, 1));
    table.setEditable(true);

    if (table.getColumnExt("Column ID") != null) {
      table.getColumnExt("Column ID").setVisible(false);
    }

    if (table.getColumnExt("Column ID SQL") != null) {
      table.getColumnExt("Column ID SQL").setVisible(false);
    }

    return new JXTableCase(tt);
  }

  public static JXTableCase createMetricsCase() {
    JXTableCase jxTableCase = GUIHelper.getJXTableCaseCheckBox(7, new String[]{
        MetricsColumnNames.ID.getColName(),
        MetricsColumnNames.NAME.getColName(),
        MetricsColumnNames.IS_DEFAULT.getColName(),
        MetricsColumnNames.X_AXIS.getColName(),
        MetricsColumnNames.Y_AXIS.getColName(),
        MetricsColumnNames.GROUP.getColName(),
        MetricsColumnNames.METRIC_FUNCTION.getColName(),
        MetricsColumnNames.CHART_TYPE.getColName()
    }, 2);
    jxTableCase.getJxTable().getColumnExt(0).setVisible(false);
    jxTableCase.getJxTable().setEditable(false);

    TableColumn col = jxTableCase.getJxTable().getColumnModel().getColumn(1);
    col.setMinWidth(30);
    col.setMaxWidth(50);

    return jxTableCase;
  }

  public static TTTable<TemplateMetricRow, JXTable> createTemplateMetricsCase() {
    TTTable<TemplateMetricRow, JXTable> tt = JXTableTables.create(
        getSharedRegistry(),
        TemplateMetricRow.class,
        TableUi.<TemplateMetricRow>builder()
            .rowIcon(ModelIconProviders.forTemplateMetricRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    configureStandardJXTable(table);
    table.setEditable(false);

    if (table.getColumnExt(MetricsColumnNames.ID.getColName()) != null) {
      table.getColumnExt(MetricsColumnNames.ID.getColName()).setVisible(false);
    }

    if (table.getColumnCount() > 1) {
      TableColumn col = table.getColumnModel().getColumn(1);
      col.setMinWidth(30);
      col.setMaxWidth(50);
    }

    return tt;
  }

  public static JXTableCase createConnectionTemplateCase() {
    TTRegistry registry = TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();

    TTTable<ConnectionTemplateRow, JXTable> tt = JXTableTables.create(
        registry,
        ConnectionTemplateRow.class,
        TableUi.<ConnectionTemplateRow>builder()
            .rowIcon(ModelIconProviders.forConnectionTemplateRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(Color.GRAY);
    table.setIntercellSpacing(new Dimension(1, 1));

    return new JXTableCase(tt);
  }

  public static MultiSelectTaskPanel createMultiSelectPanel() {
    return new MultiSelectTaskPanel(
        ServiceLocator.get(JXTableCase.class, "taskListCase"),
        ServiceLocator.get(JXTableCase.class, "selectedTaskCase"),
        ServiceLocator.get(JXTableCase.class, "templateListTaskCase")
    );
  }

  public static MultiSelectQueryPanel createMultiSelectQueryPanel() {
    return new MultiSelectQueryPanel(
        ServiceLocator.get(JXTableCase.class, "queryListCase"),
        ServiceLocator.get(JXTableCase.class, "selectedQueryCase"),
        ServiceLocator.get(JXTableCase.class, "templateListQueryCase")
    );
  }
}