package ru.dimension.ui.component.module;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.module.report.ChartReportPresenter;
import ru.dimension.ui.component.module.report.ChartReportView;
import ru.dimension.ui.component.module.chart.ChartModel;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.state.ChartKey;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

@Log4j2
public class ChartReportModule extends JPanel {

  @Getter
  private final ChartModel model;
  @Getter
  private final ChartReportView view;
  private final ChartReportPresenter presenter;
  private final JTextArea description;

  public ChartReportModule(ChartKey chartKey,
                           ProfileTaskQueryKey key,
                           Metric metric,
                           QueryInfo queryInfo,
                           ChartInfo chartInfo,
                           TableInfo tableInfo,
                           DStore dStore) {
    super(new BorderLayout());

    this.model = new ChartModel(chartKey, key, metric, queryInfo, chartInfo, tableInfo, null, dStore);

    this.view = new ChartReportView();
    this.presenter = new ChartReportPresenter(model, view);

    this.description = new JTextArea(2, 1);
    description.setLineWrap(true);
    description.setWrapStyleWord(true);
    JScrollPane descriptionScrollPane = new JScrollPane(description);

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(descriptionScrollPane, BorderLayout.NORTH);
    contentPanel.add(view, BorderLayout.CENTER);

    add(contentPanel, BorderLayout.CENTER);

    presenter.initializeChart();
  }

  public String getDescription() {
    return description.getText();
  }

  public void setDescription(String text) {
    description.setText(text);
  }
}