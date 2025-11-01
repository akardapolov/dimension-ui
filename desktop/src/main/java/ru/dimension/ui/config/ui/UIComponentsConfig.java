package ru.dimension.ui.config.ui;

import static ru.dimension.ui.model.view.TemplateAction.LOAD;
import static ru.dimension.ui.model.view.TemplateAction.SAVE;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.table.TableColumn;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jdesktop.swingx.JXTextArea;
import ru.dimension.db.model.profile.table.IType;
import ru.dimension.db.model.profile.table.TType;
import ru.dimension.di.DimensionDI;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.column.ConnectionColumnNames;
import ru.dimension.ui.model.column.MetadataColumnNames;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.column.ProfileColumnNames;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.model.column.TaskColumnNames;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.custom.DetailedComboBox;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.view.panel.config.query.MainQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetadataQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetricQueryPanel;
import ru.dimension.ui.view.panel.config.task.MultiSelectQueryPanel;
import ru.dimension.ui.view.panel.template.TemplateConnPanel;
import ru.dimension.ui.view.panel.template.TemplateEditPanel;
import ru.dimension.ui.view.panel.template.TemplateHTTPConnPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.tab.ConnTypeTab;

public final class UIComponentsConfig {

  private UIComponentsConfig() {
  }

  public static void configure(DimensionDI.Builder builder) {
    builder
        // Panels (bind as components)
        .bindNamed(ru.dimension.ui.view.panel.config.profile.ProfilePanel.class, "profileConfigPanel",
                   ru.dimension.ui.view.panel.config.profile.ProfilePanel.class)
        .bindNamed(ru.dimension.ui.view.panel.config.task.TaskPanel.class, "taskConfigPanel",
                   ru.dimension.ui.view.panel.config.task.TaskPanel.class)
        .bindNamed(ru.dimension.ui.view.panel.config.connection.ConnectionPanel.class, "connectionConfigPanel",
                   ru.dimension.ui.view.panel.config.connection.ConnectionPanel.class)
        .bindNamed(ru.dimension.ui.view.panel.config.query.QueryPanel.class, "queryConfigPanel",
                   ru.dimension.ui.view.panel.config.query.QueryPanel.class)
        .bindNamed(TemplateConnPanel.class, "templateConnPanel", TemplateConnPanel.class)
        .bindNamed(TemplateHTTPConnPanel.class, "templateHTTPConnPanel", TemplateHTTPConnPanel.class)
        .bindNamed(TemplateEditPanel.class, "templateEditPanel", TemplateEditPanel.class)

        // Tables for config tabs
        .provideNamed(JXTableCase.class, "profileConfigCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(10, new String[]{
                ProfileColumnNames.ID.getColName(),
                ProfileColumnNames.NAME.getColName()
            })
        ))
        .provideNamed(JXTableCase.class, "taskConfigCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(10, new String[]{
                TaskColumnNames.ID.getColName(),
                TaskColumnNames.NAME.getColName()
            })
        ))
        .provideNamed(JXTableCase.class, "connectionConfigCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(10, new String[]{
                ConnectionColumnNames.ID.getColName(),
                ConnectionColumnNames.NAME.getColName(),
                ConnectionColumnNames.TYPE.getColName()
            })
        ))
        .provideNamed(JXTableCase.class, "queryConfigCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(10, new String[]{
                TaskColumnNames.ID.getColName(),
                TaskColumnNames.NAME.getColName()
            })
        ))

        // Tabs
        .provideNamed(ConfigTab.class, "jTabbedPaneConfig", ServiceLocator.singleton(ConfigTab::new))
        .provideNamed(ConnTypeTab.class, "templateConnectionTab", ServiceLocator.singleton(ConnTypeTab::new))
        .provideNamed(JTabbedPane.class, "mainQueryTab", ServiceLocator.singleton(JTabbedPane::new))

        // Connection comboboxes
        .provideNamed(DetailedComboBox.class, "taskConnectionComboBox", ServiceLocator.singleton(() -> {
          String[] columns = new String[]{"Name", "Username", "Url", "Jar", "Driver", "Type"};
          int[] widths = new int[]{131, 131, 132, 132, 132, 131};
          return new DetailedComboBox(columns, widths, 0);
        }))
        .provideNamed(DetailedComboBox.class, "queryConnectionMetadataComboBox", ServiceLocator.singleton(() -> {
          String[] columns = new String[]{"Name", "Username", "Url", "Jar", "Driver", "Type"};
          int[] widths = new int[]{131, 131, 132, 132, 132, 131};
          DetailedComboBox detailedComboBox = new DetailedComboBox(columns, widths, 0);
          detailedComboBox.setPreferredSize(new Dimension(120, 26));
          return detailedComboBox;
        }))

        // Task, Query list/selected/template tables
        .provideNamed(JXTableCase.class, "taskListCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(6, new String[]{
                TaskColumnNames.ID.getColName(),
                TaskColumnNames.NAME.getColName()
            })
        ))
        .provideNamed(JXTableCase.class, "selectedTaskCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(6, new String[]{
                TaskColumnNames.ID.getColName(),
                TaskColumnNames.NAME.getColName()
            })
        ))
        .provideNamed(JXTableCase.class, "templateListTaskCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(6, new String[]{
                TaskColumnNames.ID.getColName(),
                TaskColumnNames.NAME.getColName()
            })
        ))
        .provideNamed(JXTableCase.class, "queryListCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(10, new String[]{
                QueryColumnNames.ID.getColName(),
                QueryColumnNames.NAME.getColName(),
                QueryColumnNames.DESCRIPTION.getColName(),
                QueryColumnNames.TEXT.getColName()
            })
        ))
        .provideNamed(JXTableCase.class, "selectedQueryCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(10, new String[]{
                QueryColumnNames.ID.getColName(),
                QueryColumnNames.NAME.getColName(),
                QueryColumnNames.DESCRIPTION.getColName(),
                QueryColumnNames.TEXT.getColName()
            })
        ))
        .provideNamed(JXTableCase.class, "templateListQueryCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(10, new String[]{
                QueryColumnNames.ID.getColName(),
                QueryColumnNames.NAME.getColName(),
                QueryColumnNames.DESCRIPTION.getColName(),
                QueryColumnNames.TEXT.getColName()
            })
        ))

        // SQL editing
        .provideNamed(RSyntaxTextArea.class, "querySqlText", ServiceLocator.singleton(() -> {
          RSyntaxTextArea textArea = new RSyntaxTextArea(5, 60);
          textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
          textArea.setCodeFoldingEnabled(true);
          textArea.setBorder(GUIHelper.getGrayBorder());
          textArea.setBackground(LaF.getBackgroundColor(LafColorGroup.TEXTAREA, LaF.getLafType()));
          return textArea;
        }))
        .provideNamed(JComboBox.class, "queryGatherDataComboBox", ServiceLocator.singleton(
            () -> new JComboBox<>(GatherDataMode.values()))
        )

        // Metadata panel controls
        .provideNamed(DetailedComboBox.class, "timestampComboBox", ServiceLocator.singleton(
            () -> new DetailedComboBox(new String[]{"Name", "Storage type"}, new int[]{120, 120}, 0)))
        .provideNamed(JCheckBox.class, "checkboxConfig", ServiceLocator.singleton(
            () -> new JCheckBox("View the hierarchy")))
        .provideNamed(JComboBox.class, "tableType", ServiceLocator.singleton(
            () -> new JComboBox<>(TType.values())))
        .provideNamed(JComboBox.class, "indexType", ServiceLocator.singleton(
            () -> new JComboBox<>(IType.values())))

        // Query sub-panels
        .provideNamed(MainQueryPanel.class, "mainQueryPanel", ServiceLocator.singleton(
            () -> new MainQueryPanel(
                ServiceLocator.get(ButtonPanel.class, "queryButtonPanel"),
                ServiceLocator.get(JComboBox.class, "queryGatherDataComboBox"),
                ServiceLocator.get(RSyntaxTextArea.class, "querySqlText")
            )
        ))
        .provideNamed(MetadataQueryPanel.class, "metadataQueryPanel", ServiceLocator.singleton(
            () -> new MetadataQueryPanel(
                ServiceLocator.get(JXTableCase.class, "configMetadataCase"),
                ServiceLocator.get(DetailedComboBox.class, "queryConnectionMetadataComboBox"),
                ServiceLocator.get(DetailedComboBox.class, "timestampComboBox"),
                ServiceLocator.get(JComboBox.class, "tableType"),
                ServiceLocator.get(JComboBox.class, "indexType")
            )
        ))
        .provideNamed(MetricQueryPanel.class, "metricQueryPanel", ServiceLocator.singleton(
            () -> new MetricQueryPanel(
                ServiceLocator.get(ButtonPanel.class, "metricQueryButtonPanel"),
                ServiceLocator.get(JXTableCase.class, "configMetricCase"),
                ServiceLocator.get(JComboBox.class, "groupFunction"),
                ServiceLocator.get(JComboBox.class, "chartType")
            )
        ))

        // Template-related tables and areas
        .provideNamed(JXTableCase.class, "templateTaskCase", ServiceLocator.singleton(() -> {
          JXTableCase jxTableCase = GUIHelper.getJXTableCase(7, new String[]{
              TaskColumnNames.ID.getColName(),
              TaskColumnNames.NAME.getColName(),
              TaskColumnNames.PULL_TIMEOUT.getColName()
          });
          jxTableCase.getJxTable().getTableHeader().setVisible(true);
          return jxTableCase;
        }))
        .provideNamed(JXTextArea.class, "templateTaskDescription", ServiceLocator.singleton(() -> {
          JXTextArea taskDescription = new JXTextArea("Task description...");
          taskDescription.setEditable(false);
          taskDescription.setBorder(GUIHelper.getGrayBorder());
          taskDescription.setRows(12);
          taskDescription.setColumns(1);
          taskDescription.setLineWrap(true);
          return taskDescription;
        }))
        .provideNamed(JXTextArea.class, "templateQueryDescription", ServiceLocator.singleton(() -> {
          JXTextArea queryDescription = new JXTextArea("Query description...");
          queryDescription.setEditable(false);
          queryDescription.setBorder(GUIHelper.getGrayBorder());
          queryDescription.setRows(12);
          queryDescription.setColumns(1);
          queryDescription.setLineWrap(true);
          return queryDescription;
        }))
        .provideNamed(JXTextArea.class, "templateProfileDescSave", ServiceLocator.singleton(() -> {
          JXTextArea profileDescription = new JXTextArea("Profile description");
          profileDescription.setRows(6);
          profileDescription.setColumns(1);
          profileDescription.setLineWrap(true);
          return profileDescription;
        }))
        .provideNamed(JXTextArea.class, "templateTaskDescSave", ServiceLocator.singleton(() -> {
          JXTextArea taskDescription = new JXTextArea("Task description");
          taskDescription.setRows(6);
          taskDescription.setColumns(1);
          taskDescription.setLineWrap(true);
          return taskDescription;
        }))
        .provideNamed(RSyntaxTextArea.class, "templateQueryText", ServiceLocator.singleton(() -> {
          RSyntaxTextArea textArea = new RSyntaxTextArea(15, 1);
          textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
          textArea.setCodeFoldingEnabled(true);
          textArea.setBorder(GUIHelper.getGrayBorder());
          textArea.setBackground(LaF.getBackgroundColor(LafColorGroup.TEXTAREA, LaF.getLafType()));
          textArea.setLineWrap(true);
          textArea.setAutoscrolls(true);
          return textArea;
        }))
        .provideNamed(JXTableCase.class, "templateConnCase", ServiceLocator.singleton(() -> {
          JXTableCase jxTableCase = GUIHelper.getJXTableCase(7, new String[]{
              ConnectionColumnNames.ID.getColName(),
              ConnectionColumnNames.NAME.getColName(),
              ConnectionColumnNames.TYPE.getColName()
          });
          jxTableCase.getJxTable().getTableHeader().setVisible(true);
          return jxTableCase;
        }))
        .provideNamed(JXTableCase.class, "templateQueryCase", ServiceLocator.singleton(() -> {
          JXTableCase jxTableCase = GUIHelper.getJXTableCase(7, new String[]{
              QueryColumnNames.ID.getColName(),
              QueryColumnNames.NAME.getColName(),
              QueryColumnNames.GATHER.getColName()
          });
          jxTableCase.getJxTable().getTableHeader().setVisible(true);
          return jxTableCase;
        }))
        .provideNamed(JButton.class, "templateLoadJButton", ServiceLocator.singleton(() -> {
          JButton jButton = new JButton();
          jButton.setActionCommand(LOAD.name());
          jButton.setMnemonic('L');
          jButton.setText("Load");
          jButton.setPreferredSize(new Dimension(120, 30));
          return jButton;
        }))
        .provideNamed(JButton.class, "templateSaveJButton", ServiceLocator.singleton(() -> {
          JButton jButton = new JButton();
          jButton.setActionCommand(SAVE.name());
          jButton.setMnemonic('S');
          jButton.setText("Save");
          jButton.setPreferredSize(new Dimension(120, 30));
          return jButton;
        }))

        // Button panels
        .provideNamed(ButtonPanel.class, "profileButtonPanel", ServiceLocator.singleton(ButtonPanel::new))
        .provideNamed(ButtonPanel.class, "taskButtonPanel", ServiceLocator.singleton(ButtonPanel::new))
        .provideNamed(ButtonPanel.class, "connectionButtonPanel", ServiceLocator.singleton(ButtonPanel::new))
        .provideNamed(ButtonPanel.class, "queryButtonPanel", ServiceLocator.singleton(ButtonPanel::new))
        .provideNamed(ButtonPanel.class, "metricQueryButtonPanel", ServiceLocator.singleton(ButtonPanel::new))

        // Generic list/selected tables
        .provideNamed(JXTableCase.class, "listCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(4, new String[]{
                ConnectionColumnNames.ID.getColName(),
                ConnectionColumnNames.NAME.getColName()
            })
        ))
        .provideNamed(JXTableCase.class, "selectedCase", ServiceLocator.singleton(
            () -> GUIHelper.getJXTableCase(4, new String[]{
                ConnectionColumnNames.ID.getColName(),
                ConnectionColumnNames.NAME.getColName()
            })
        ))

        // Metadata config table
        .provideNamed(JXTableCase.class, "configMetadataCase", ServiceLocator.singleton(() -> {
          JXTableCase jxTableCase = GUIHelper.getJXTableCaseCheckBox(10, new String[]{
              MetadataColumnNames.COLUMN_ID.getColName(),
              MetadataColumnNames.COLUMN_ID_SQL.getColName(),
              MetadataColumnNames.COLUMN.getColName(),
              MetadataColumnNames.COLUMN_DB_TYPE_NAME.getColName(),
              MetadataColumnNames.STORAGE.getColName(),
              MetadataColumnNames.COLUMN_TYPE.getColName(),
              MetadataColumnNames.DIMENSION.getColName()
          }, 6);
          jxTableCase.getJxTable().getColumnExt(0).setVisible(false);
          jxTableCase.getJxTable().getColumnExt(0).setVisible(false);

          Enumeration<TableColumn> columns = jxTableCase.getJxTable().getColumnModel().getColumns();
          while (columns.hasMoreElements()) {
            TableColumn column = columns.nextElement();
            if (MetadataColumnNames.DIMENSION.getColName().equals(column.getHeaderValue())) {
              column.setCellRenderer(new ru.dimension.ui.view.custom.BorderCellCheckBoxRenderer());
              column.setMinWidth(50);
              column.setMaxWidth(70);
            }
          }
          return jxTableCase;
        }))

        // Metrics tables
        .provideNamed(JXTableCase.class, "templateMetricsCase", ServiceLocator.singleton(UIComponentsConfig::getMetricsCase))
        .provideNamed(JXTableCase.class, "configMetricCase", ServiceLocator.singleton(UIComponentsConfig::getMetricsCase))

        // Connection template table (full connection details)
        .provideNamed(JXTableCase.class, "connectionTemplateCase", ServiceLocator.singleton(() -> {
          JXTableCase jxTableCase = GUIHelper.getJXTableCase(3, new String[]{
              ConnectionColumnNames.ID.getColName(),
              ConnectionColumnNames.NAME.getColName(),
              ConnectionColumnNames.USER_NAME.getColName(),
              ConnectionColumnNames.PASSWORD.getColName(),
              ConnectionColumnNames.URL.getColName(),
              ConnectionColumnNames.JAR.getColName(),
              ConnectionColumnNames.DRIVER.getColName(),
              ConnectionColumnNames.TYPE.getColName(),
              ConnectionColumnNames.HTTP_METHOD.getColName(),
              ConnectionColumnNames.HTTP_PARSE_TYPE.getColName()
          });
          jxTableCase.getJxTable().getColumnExt(0).setVisible(false);
          return jxTableCase;
        }))

        // Multi-select panels
        .provideNamed(MultiSelectTaskPanel.class, "multiSelectPanel", ServiceLocator.singleton(() ->
                                                                                                   new MultiSelectTaskPanel(
                                                                                                       ServiceLocator.get(JXTableCase.class, "taskListCase"),
                                                                                                       ServiceLocator.get(JXTableCase.class, "selectedTaskCase"),
                                                                                                       ServiceLocator.get(JXTableCase.class, "templateListTaskCase")
                                                                                                   )
        ))
        .provideNamed(MultiSelectQueryPanel.class, "multiSelectQueryPanel", ServiceLocator.singleton(() ->
                                                                                                         new MultiSelectQueryPanel(
                                                                                                             ServiceLocator.get(JXTableCase.class, "queryListCase"),
                                                                                                             ServiceLocator.get(JXTableCase.class, "selectedQueryCase"),
                                                                                                             ServiceLocator.get(JXTableCase.class, "templateListQueryCase")
                                                                                                         )
        ))

        // Misc
        .provideNamed(JComboBox.class, "groupFunction", ServiceLocator.singleton(() -> new JComboBox<>(GroupFunction.values())))
        .provideNamed(JComboBox.class, "chartType", ServiceLocator.singleton(() -> new JComboBox<>(ChartType.values())))
        .provideNamed(List.class, "columnsCheckBox", ServiceLocator.singleton(ArrayList::new));
  }

  // Helper (moved from GlobalConfig)
  private static JXTableCase getMetricsCase() {
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
}