package ru.dimension.ui.config.ui;

import static ru.dimension.ui.helper.GUIHelper.getTypedJXTableCase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jdesktop.swingx.JXTextArea;
import ru.dimension.db.model.profile.table.IType;
import ru.dimension.db.model.profile.table.TType;
import ru.dimension.di.DimensionDI;
import ru.dimension.di.ServiceLocator;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.custom.DetailedComboBox;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.connection.ConnectionPanel;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.view.panel.config.profile.ProfilePanel;
import ru.dimension.ui.view.panel.config.query.MainQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetadataQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetricQueryPanel;
import ru.dimension.ui.view.panel.config.query.QueryPanel;
import ru.dimension.ui.view.panel.config.task.MultiSelectQueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;
import ru.dimension.ui.view.panel.template.TemplateConnPanel;
import ru.dimension.ui.view.panel.template.TemplateEditPanel;
import ru.dimension.ui.view.panel.template.TemplateHTTPConnPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.tab.ConnTypeTab;
import ru.dimension.ui.view.table.row.Rows;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

public final class UIComponentsConfig {

  private UIComponentsConfig() {
  }

  private static <T> Supplier<T> s(Supplier<T> supplier) {
    return ServiceLocator.singleton(supplier);
  }

  public static void configure(DimensionDI.Builder builder) {
    builder
        .bindNamed(ProfilePanel.class,          "profileConfigPanel",    ProfilePanel.class)
        .bindNamed(TaskPanel.class,             "taskConfigPanel",       TaskPanel.class)
        .bindNamed(ConnectionPanel.class,       "connectionConfigPanel", ConnectionPanel.class)
        .bindNamed(QueryPanel.class,            "queryConfigPanel",      QueryPanel.class)
        .bindNamed(TemplateConnPanel.class,     "templateConnPanel",     TemplateConnPanel.class)
        .bindNamed(TemplateHTTPConnPanel.class, "templateHTTPConnPanel", TemplateHTTPConnPanel.class)
        .bindNamed(TemplateEditPanel.class,     "templateEditPanel",     TemplateEditPanel.class)

        .provideNamed(JXTableCase.class, "profileConfigCase",    s(() -> getTypedJXTableCase(ProfileRow.class)))
        .provideNamed(JXTableCase.class, "taskConfigCase",       s(() -> getTypedJXTableCase(TaskRow.class)))
        .provideNamed(JXTableCase.class, "connectionConfigCase", s(() -> getTypedJXTableCase(ConnectionRow.class)))
        .provideNamed(JXTableCase.class, "queryConfigCase",      s(() -> getTypedJXTableCase(QueryRow.class)))

        .provideNamed(ConfigTab.class,   "configTab",             s(UIFactory::createConfigTab))
        .provideNamed(ConnTypeTab.class, "templateConnectionTab", s(ConnTypeTab::new))
        .provideNamed(JTabbedPane.class, "mainQueryTab",          s(JTabbedPane::new))

        .provideNamed(DetailedComboBox.class, "taskConnectionComboBox",          s(UIFactory::createTaskConnectionComboBox))
        .provideNamed(DetailedComboBox.class, "queryConnectionMetadataComboBox", s(UIFactory::createQueryConnectionMetadataComboBox))
        .provideNamed(DetailedComboBox.class, "timestampComboBox",               s(UIFactory::createTimestampComboBox))

        .provideNamed(JXTableCase.class, "taskListCase",         s(() -> getTypedJXTableCase(TaskRow.class)))
        .provideNamed(JXTableCase.class, "selectedTaskCase",     s(() -> getTypedJXTableCase(TaskRow.class)))
        .provideNamed(JXTableCase.class, "templateListTaskCase", s(() -> getTypedJXTableCase(TaskRow.class)))

        .provideNamed(JXTableCase.class, "queryListCase",         s(() -> getTypedJXTableCase(Rows.QueryTableRow.class)))
        .provideNamed(JXTableCase.class, "selectedQueryCase",     s(() -> getTypedJXTableCase(Rows.QueryTableRow.class)))
        .provideNamed(JXTableCase.class, "templateListQueryCase", s(() -> getTypedJXTableCase(Rows.QueryTableRow.class)))

        .provideNamed(RSyntaxTextArea.class, "querySqlText",            s(UIFactory::createQuerySqlText))
        .provideNamed(JComboBox.class,       "queryGatherDataComboBox", s(() -> new JComboBox<>(GatherDataMode.values())))

        .provideNamed(JCheckBox.class, "checkboxConfig", s(() -> new JCheckBox("View the hierarchy")))
        .provideNamed(JComboBox.class, "tableType",      s(() -> new JComboBox<>(TType.values())))
        .provideNamed(JComboBox.class, "indexType",      s(() -> new JComboBox<>(IType.values())))
        .provideNamed(JComboBox.class, "groupFunction",  s(() -> new JComboBox<>(GroupFunction.values())))
        .provideNamed(JComboBox.class, "chartType",      s(() -> new JComboBox<>(ChartType.values())))

        .provideNamed(MainQueryPanel.class,     "mainQueryPanel",     s(UIFactory::createMainQueryPanel))
        .provideNamed(MetadataQueryPanel.class, "metadataQueryPanel", s(UIFactory::createMetadataQueryPanel))
        .provideNamed(MetricQueryPanel.class,   "metricQueryPanel",   s(UIFactory::createMetricQueryPanel))

        .provideNamed(TTTable.class, "templateTaskCase",    s(UIFactory::createTemplateTaskCase))
        .provideNamed(TTTable.class, "templateConnCase",    s(UIFactory::createTemplateConnCase))
        .provideNamed(TTTable.class, "templateQueryCase",   s(UIFactory::createTemplateQueryCase))
        .provideNamed(TTTable.class, "templateMetricsCase", s(UIFactory::createTemplateMetricsCase))

        .provideNamed(JXTextArea.class, "templateTaskDescription",  s(UIFactory::createTemplateTaskDescription))
        .provideNamed(JXTextArea.class, "templateQueryDescription", s(UIFactory::createTemplateQueryDescription))
        .provideNamed(JXTextArea.class, "templateProfileDescSave",  s(UIFactory::createTemplateProfileDescSave))
        .provideNamed(JXTextArea.class, "templateTaskDescSave",     s(UIFactory::createTemplateTaskDescSave))

        .provideNamed(RSyntaxTextArea.class, "templateQueryText", s(UIFactory::createTemplateQueryText))

        .provideNamed(JButton.class, "templateLoadJButton", s(UIFactory::createTemplateLoadJButton))
        .provideNamed(JButton.class, "templateSaveJButton", s(UIFactory::createTemplateSaveJButton))

        .provideNamed(ButtonPanel.class, "profileButtonPanel",     s(ButtonPanel::new))
        .provideNamed(ButtonPanel.class, "taskButtonPanel",        s(ButtonPanel::new))
        .provideNamed(ButtonPanel.class, "connectionButtonPanel",  s(ButtonPanel::new))
        .provideNamed(ButtonPanel.class, "queryButtonPanel",       s(ButtonPanel::new))
        .provideNamed(ButtonPanel.class, "metricQueryButtonPanel", s(ButtonPanel::new))

        .provideNamed(JXTableCase.class, "listCase",     s(UIFactory::createListCase))
        .provideNamed(JXTableCase.class, "selectedCase", s(UIFactory::createSelectedCase))

        .provideNamed(JXTableCase.class, "configMetadataCase",     s(UIFactory::createConfigMetadataCase))
        .provideNamed(JXTableCase.class, "configMetricCase",       s(UIFactory::createMetricsCase))
        .provideNamed(JXTableCase.class, "connectionTemplateCase", s(UIFactory::createConnectionTemplateCase))

        .provideNamed(MultiSelectTaskPanel.class,  "multiSelectPanel",      s(UIFactory::createMultiSelectPanel))
        .provideNamed(MultiSelectQueryPanel.class, "multiSelectQueryPanel", s(UIFactory::createMultiSelectQueryPanel))

        .provideNamed(List.class, "columnsCheckBox", s(ArrayList::new));

    UIHandlersConfig.configure(builder);
  }
}