
package ru.dimension.ui.component.module.chart;

import java.awt.Dimension;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.module.chart.unit.HistoryUnitView;
import ru.dimension.ui.component.module.chart.unit.RealtimeUnitView;
import ru.dimension.ui.helper.GUIHelper;

@Data
@Log4j2
public class ChartView extends JPanel {
  private final MessageBroker.Component component;

  private static Dimension dimension = new Dimension(100, 600);

  @Getter
  private JTabbedPane tabbedPane;

  @Getter
  private final RealtimeUnitView realtimeUnitView;
  @Getter
  private final HistoryUnitView historyUnitView;

  public ChartView(MessageBroker.Component component) {
    this.component = component;

    tabbedPane = new JTabbedPane();
    tabbedPane.setBorder(new EtchedBorder());

    realtimeUnitView = new RealtimeUnitView(component);
    historyUnitView = new HistoryUnitView(component);

    tabbedPane.addTab(GUIHelper.getTabName(Panel.REALTIME), realtimeUnitView.getRootComponent());
    tabbedPane.addTab(GUIHelper.getTabName(Panel.HISTORY), historyUnitView.getRootComponent());
  }
}
