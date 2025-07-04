package ru.dimension.ui.view.analyze.module;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.view.analyze.model.ChartCardState;
import ru.dimension.ui.view.analyze.model.ChartLegendState;
import ru.dimension.ui.view.analyze.router.Message;
import ru.dimension.ui.view.analyze.router.MessageAction;
import ru.dimension.ui.view.analyze.router.MessageRouter;
import ru.dimension.ui.view.analyze.router.MessageRouter.Action;
import ru.dimension.ui.view.analyze.router.MessageRouter.Destination;

public class ChartConfigModule extends JPanel implements MessageAction, ActionListener {

  private final MessageRouter router;

  protected final ButtonGroup buttonGroup;
  protected final JRadioButton showRadioButton;
  protected final JRadioButton hideRadioButton;
  private final JCheckBox collapseCheckBox;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public ChartConfigModule(MessageRouter router) {
    this.router = router;

    this.showRadioButton = new JRadioButton("Show", true);
    this.hideRadioButton = new JRadioButton("Hide");

    this.buttonGroup = new ButtonGroup();
    this.buttonGroup.add(showRadioButton);
    this.buttonGroup.add(hideRadioButton);

    this.showRadioButton.addActionListener(this);
    this.hideRadioButton.addActionListener(this);

    this.collapseCheckBox = new JCheckBox(ChartCardState.COLLAPSE_ALL.getName());
    this.collapseCheckBox.addActionListener(this);

    this.initUI();
  }

  private void initUI() {
    PainlessGridBag gblL = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    gblL.row()
        .cell(new JLabel("Legend"))
        .cell(showRadioButton)
        .cell(hideRadioButton)
        .cell(new JLabel(" | "))
        .cell(collapseCheckBox)
        .cellXRemainder(new JLabel())
        .fillX();
    gblL.done();
  }

  @Override
  public void receive(Message message) {

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == showRadioButton) {
      showRadioButton.setSelected(true);
      hideRadioButton.setSelected(false);

      router.sendMessage(Message.builder()
                             .destination(Destination.CHART_LIST)
                             .action(Action.SET_CHART_LEGEND_STATE)
                             .parameter("chartLegendState", ChartLegendState.SHOW)
                             .build());
    }
    if (e.getSource() == hideRadioButton) {
      hideRadioButton.setSelected(true);
      showRadioButton.setSelected(false);

      router.sendMessage(Message.builder()
                             .destination(Destination.CHART_LIST)
                             .action(Action.SET_CHART_LEGEND_STATE)
                             .parameter("chartLegendState", ChartLegendState.HIDE)
                             .build());
    }

    if (e.getSource() == collapseCheckBox) {
      ChartCardState currentState = Arrays.stream(ChartCardState.values())
          .filter(state -> state.getName().equals(collapseCheckBox.getText()))
          .findFirst()
          .orElse(ChartCardState.COLLAPSE_ALL);

      ChartCardState newState = currentState.toggle();

      collapseCheckBox.setText(newState.getName());

      router.sendMessage(Message.builder()
                             .destination(Destination.CHART_LIST)
                             .action(Action.SET_CHART_CARD_STATE)
                             .parameter("chartCardState", currentState)
                             .build());
    }
  }
}
