package ru.dimension.ui.view.structure.workspace.handler;

import static ru.dimension.ui.model.view.ProcessType.HISTORY;
import static ru.dimension.ui.model.view.ProcessTypeWorkspace.VISUALIZE;
import static ru.dimension.ui.model.view.RangeHistory.CUSTOM;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.cache.AppCache;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.structure.workspace.query.DetailsControlPanel;
import ru.dimension.ui.view.tab.HistoryTab;
import ru.dimension.ui.view.tab.RealTimeTab;
import ru.dimension.ui.view.tab.TaskTab;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.info.gui.RangeInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.panel.TimeRangeRelativePanel;

@Log4j2
public class TimeRangeRelativeHandler extends ChartHandler implements ActionListener, ChangeListener, MouseListener {

  private final TimeRangeRelativePanel timeRangeRelativePanel;
  private String selectedFrom;
  private LocalDateTime dateSelectedFrom;
  private LocalDateTime dateChangedFrom;
  private String selectedTo;
  private LocalDateTime dateSelectedTo;
  private LocalDateTime dateChangedTo;
  private int addCountFrom;
  private int countFrom;
  private int previousValueFrom;
  private int addCountTo;
  private int countTo;
  private int previousValueTo;

  @Inject
  @Named("eventListener")
  EventListener eventListener;

  @Inject
  @Named("profileManager")
  ProfileManager profileManager;

  @Inject
  @Named("appCache")
  AppCache appCache;

  public TimeRangeRelativeHandler(JXTableCase jxTableCaseMetrics,
                                  JXTableCase jxTableCaseColumns,
                                  TimeRangeRelativePanel timeRangeRelativePanel,
                                  TaskTab taskTab,
                                  RealTimeTab realTimeTab,
                                  HistoryTab historyTab,
                                  JSplitPane visualizeRealTime,
                                  JSplitPane visualizeHistory,
                                  JPanel analyzeRealTime,
                                  JPanel analyzeHistory,
                                  ProfileTaskQueryKey profileTaskQueryKey,
                                  QueryInfo queryInfo,
                                  TableInfo tableInfo,
                                  ChartInfo chartInfo,
                                  DetailsControlPanel detailsControlPanel,
                                  WorkspaceQueryComponent workspaceQueryComponent) {
    super(taskTab, realTimeTab, historyTab, jxTableCaseMetrics, jxTableCaseColumns, tableInfo, queryInfo, chartInfo,
          profileTaskQueryKey, visualizeRealTime, visualizeHistory, analyzeRealTime, analyzeHistory, detailsControlPanel, workspaceQueryComponent);

    this.timeRangeRelativePanel = timeRangeRelativePanel;
    this.timeRangeRelativePanel.getJButtonFrom().addActionListener(this);
    this.timeRangeRelativePanel.getJButtonTo().addActionListener(this);
    this.timeRangeRelativePanel.getJButtonGo().addActionListener(this);

    this.timeRangeRelativePanel.getJComboBoxFrom().addActionListener(this);
    this.timeRangeRelativePanel.getJComboBoxTo().addActionListener(this);

    this.timeRangeRelativePanel.getJSpinnerFrom().addChangeListener(this);
    this.timeRangeRelativePanel.getJSpinnerTo().addChangeListener(this);

    this.selectedFrom = timeRangeRelativePanel.getJLabelPromptTextFrom().getText();
    this.dateSelectedFrom = LocalDateTime.parse(selectedFrom, DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));
    this.dateChangedFrom = dateSelectedFrom;

    this.selectedTo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));
    this.dateSelectedTo = LocalDateTime.parse(selectedTo, DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));
    this.dateChangedTo = dateSelectedTo;

    this.countFrom = 0;
    this.previousValueFrom = 0;
    this.countTo = 0;
    this.previousValueTo = 0;
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == timeRangeRelativePanel.getJButtonFrom()) {
      timeRangeRelativePanel.getJLabelPromptTextFrom()
          .setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH)));
      timeRangeRelativePanel.getJSpinnerFrom().setValue(0);
    }

    if (e.getSource() == timeRangeRelativePanel.getJButtonTo()) {
      timeRangeRelativePanel.getJLabelPromptTextTo()
          .setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH)));
      timeRangeRelativePanel.getJSpinnerTo().setValue(0);
    }
    int dayNow = LocalDateTime.now().getDayOfYear();
    if (e.getSource() == timeRangeRelativePanel.getJComboBoxFrom()) {
      if (timeRangeRelativePanel.getJComboBoxFrom().getSelectedItem().equals("Seconds ago")) {
        countFrom = 0;
        timeRangeRelativePanel.getJCheckBoxFrom().setText("round to the second");
        SpinnerModel spinnerModel = null;
        if (dayNow != dateSelectedFrom.getDayOfYear()) {
          spinnerModel = new SpinnerNumberModel(0, dateSelectedFrom.getSecond() - 59,
                                                dateSelectedFrom.getSecond(), 1);
        } else {
          spinnerModel = new SpinnerNumberModel(0, 0, 59, 1);
        }
        timeRangeRelativePanel.getJSpinnerFrom().setModel(spinnerModel);

      } else if (timeRangeRelativePanel.getJComboBoxFrom().getSelectedItem().equals("Minutes ago")) {
        countFrom = 0;
        timeRangeRelativePanel.getJCheckBoxFrom().setText("round to the minute");
        SpinnerModel spinnerModel;
        if (dayNow != dateSelectedFrom.getDayOfYear()) {
          spinnerModel = new SpinnerNumberModel(0, dateSelectedFrom.getMinute() - 59,
                                                dateSelectedFrom.getMinute(), 1);
        } else {
          spinnerModel = new SpinnerNumberModel(0, 0, 59, 1);
        }
        timeRangeRelativePanel.getJSpinnerFrom().setModel(spinnerModel);

      } else if (timeRangeRelativePanel.getJComboBoxFrom().getSelectedItem().equals("Hours ago")) {
        countFrom = 0;
        timeRangeRelativePanel.getJCheckBoxFrom().setText("round to the hour");
        SpinnerModel spinnerModel;
        if (dayNow != dateSelectedFrom.getDayOfYear()) {
          spinnerModel = new SpinnerNumberModel(0, dateSelectedFrom.getHour() - 23,
                                                dateSelectedFrom.getHour(), 1);
        } else {
          spinnerModel = new SpinnerNumberModel(0, 0, 23, 1);
        }
        timeRangeRelativePanel.getJSpinnerFrom().setModel(spinnerModel);

      } else if (timeRangeRelativePanel.getJComboBoxFrom().getSelectedItem().equals("Days ago")) {
        countFrom = 0;
        timeRangeRelativePanel.getJCheckBoxFrom().setText("round to the day");
        SpinnerModel spinnerModel;
        if (dayNow > dateSelectedFrom.getDayOfYear()) {
          spinnerModel = new SpinnerNumberModel(0,
                                                dateSelectedFrom.getDayOfYear() - (dayNow + 1), 730, 1);
        } else {
          spinnerModel = new SpinnerNumberModel(0, 0, 730, 1);
        }
        timeRangeRelativePanel.getJSpinnerFrom().setModel(spinnerModel);
      }
    }

    if (e.getSource() == timeRangeRelativePanel.getJComboBoxTo()) {
      if (timeRangeRelativePanel.getJComboBoxTo().getSelectedItem().equals("Seconds ago")) {
        countTo = 0;
        timeRangeRelativePanel.getJCheckBoxTo().setText("round to the second");
        SpinnerModel spinnerModel;
        if (dayNow != dateSelectedTo.getDayOfYear()) {
          spinnerModel = new SpinnerNumberModel(0, dateSelectedTo.getSecond() - 59,
                                                dateSelectedTo.getSecond(), 1);
        } else {
          spinnerModel = new SpinnerNumberModel(0, 0, 59, 1);
        }
        timeRangeRelativePanel.getJSpinnerTo().setModel(spinnerModel);
      } else if (timeRangeRelativePanel.getJComboBoxTo().getSelectedItem().equals("Minutes ago")) {
        countTo = 0;
        timeRangeRelativePanel.getJCheckBoxTo().setText("round to the minute");
        SpinnerModel spinnerModel;
        if (dayNow != dateSelectedTo.getDayOfYear()) {
          spinnerModel = new SpinnerNumberModel(0, dateSelectedTo.getMinute() - 59,
                                                dateSelectedTo.getMinute(), 1);
        } else {
          spinnerModel = new SpinnerNumberModel(0, 0, 59, 1);
        }
        timeRangeRelativePanel.getJSpinnerTo().setModel(spinnerModel);
      } else if (timeRangeRelativePanel.getJComboBoxTo().getSelectedItem().equals("Hours ago")) {
        countTo = 0;
        timeRangeRelativePanel.getJCheckBoxTo().setText("round to the hour");
        SpinnerModel spinnerModel;
        if (dayNow != dateSelectedTo.getDayOfYear()) {
          spinnerModel = new SpinnerNumberModel(0, dateSelectedTo.getHour() - 23,
                                                dateSelectedTo.getHour(), 1);
        } else {
          spinnerModel = new SpinnerNumberModel(0, 0, 23, 1);
        }
        timeRangeRelativePanel.getJSpinnerTo().setModel(spinnerModel);
      } else if (timeRangeRelativePanel.getJComboBoxTo().getSelectedItem().equals("Days ago")) {
        countTo = 0;
        timeRangeRelativePanel.getJCheckBoxTo().setText("round to the day");
        SpinnerModel spinnerModel;
        if (dayNow > dateSelectedTo.getDayOfYear()) {
          spinnerModel = new SpinnerNumberModel(0,
                                                dateSelectedTo.getDayOfYear() - (dayNow + 1), 730, 1);
        } else {
          spinnerModel = new SpinnerNumberModel(0, 0, 730, 1);
        }
        timeRangeRelativePanel.getJSpinnerTo().setModel(spinnerModel);
      }
    }

    if (e.getSource() == timeRangeRelativePanel.getJButtonGo()) {
      taskTab.setSelectedTab(HISTORY);
      historyTab.setSelectedTab(VISUALIZE);
      chartInfo.setRangeHistory(CUSTOM);
      LocalDateTime end = LocalDateTime.now();

      if (!timeRangeRelativePanel.getJLabelPromptTextTo().getText().equals("Now")) {
        end = LocalDateTime.parse(timeRangeRelativePanel.getJLabelPromptTextTo().getText(),
                                  DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));
      }
      LocalDateTime begin = LocalDateTime.parse(timeRangeRelativePanel.getJLabelPromptTextFrom().getText(),
                                                DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));

      this.chartInfo.setCustomBegin(begin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
      this.chartInfo.setCustomEnd(end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

      this.loadChart(HISTORY);

      RangeInfo rangeInfo = new RangeInfo();
      long selectionIndex = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
      rangeInfo.setCreatedAt(selectionIndex);
      rangeInfo.setBegin(begin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
      rangeInfo.setEnd(end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
      rangeInfo.setSourceTab("R");

      appCache.putRangeInfo(profileTaskQueryKey, rangeInfo);

      eventListener.fireOnAddToAppCache(profileTaskQueryKey);
    }
  }

  @Override
  public void stateChanged(ChangeEvent e) {

    //FOR FROM
    addCountFrom = Integer.parseInt(timeRangeRelativePanel.getJSpinnerFrom().getValue().toString());
    selectedFrom = timeRangeRelativePanel.getJLabelPromptTextFrom().getText();
    dateSelectedFrom = LocalDateTime.parse(selectedFrom, DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));
    dateChangedFrom = dateSelectedFrom;

    if (e.getSource() == timeRangeRelativePanel.getJSpinnerFrom()) {
      if (timeRangeRelativePanel.getJComboBoxFrom().getSelectedItem().equals("Seconds ago")) {
        countFrom++;
        if (countFrom - addCountFrom > previousValueFrom) {
          dateChangedFrom = dateSelectedFrom.plusSeconds(1);
        } else {
          dateChangedFrom = dateSelectedFrom.minusSeconds(1);
        }
        timeRangeRelativePanel.getJLabelPromptTextFrom()
            .setText(dateChangedFrom.format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH)));
        previousValueFrom = countFrom - addCountFrom;

      } else if (timeRangeRelativePanel.getJComboBoxFrom().getSelectedItem().equals("Minutes ago")) {
        countFrom++;
        if (countFrom - addCountFrom > previousValueFrom) {
          dateChangedFrom = dateSelectedFrom.plusMinutes(1);
        } else {
          dateChangedFrom = dateSelectedFrom.minusMinutes(1);
        }
        timeRangeRelativePanel.getJLabelPromptTextFrom()
            .setText(dateChangedFrom.format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH)));
        previousValueFrom = countFrom - addCountFrom;

      } else if (timeRangeRelativePanel.getJComboBoxFrom().getSelectedItem().equals("Hours ago")) {
        countFrom++;
        if (countFrom - addCountFrom > previousValueFrom) {
          dateChangedFrom = dateSelectedFrom.plusHours(1);
        } else {
          dateChangedFrom = dateSelectedFrom.minusHours(1);
        }
        timeRangeRelativePanel.getJLabelPromptTextFrom()
            .setText(dateChangedFrom.format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH)));
        previousValueFrom = countFrom - addCountFrom;
      } else if (timeRangeRelativePanel.getJComboBoxFrom().getSelectedItem().equals("Days ago")) {
        countFrom++;
        if (countFrom - addCountFrom > previousValueFrom) {
          dateChangedFrom = dateSelectedFrom.plusDays(1);
        } else {
          dateChangedFrom = dateSelectedFrom.minusDays(1);
        }
        timeRangeRelativePanel.getJLabelPromptTextFrom()
            .setText(dateChangedFrom.format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH)));
        previousValueFrom = countFrom - addCountFrom;
      }
    }

    //FOR TO
    int dayNow = LocalDateTime.now().getDayOfYear();
    addCountTo = Integer.parseInt(timeRangeRelativePanel.getJSpinnerTo().getValue().toString());
    if (!timeRangeRelativePanel.getJLabelPromptTextTo().getText().equals("Now")) {
      selectedTo = timeRangeRelativePanel.getJLabelPromptTextTo().getText();
    } else {
      selectedTo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));
    }
    dateSelectedTo = LocalDateTime.parse(selectedTo, DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));
    dateChangedTo = dateSelectedTo;

    if (e.getSource() == timeRangeRelativePanel.getJSpinnerTo()) {
      if (timeRangeRelativePanel.getJComboBoxTo().getSelectedItem().equals("Seconds ago")) {
        countTo++;
        if (countTo - addCountTo > previousValueTo) {
          dateChangedTo = dateSelectedTo.plusSeconds(1);
        } else {
          dateChangedTo = dateSelectedTo.minusSeconds(1);
        }
        timeRangeRelativePanel.getJLabelPromptTextTo()
            .setText(dateChangedTo.format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH)));
        previousValueTo = countTo - addCountTo;
      } else if (timeRangeRelativePanel.getJComboBoxTo().getSelectedItem().equals("Minutes ago")) {
        countTo++;
        if (countTo - addCountTo > previousValueTo) {
          dateChangedTo = dateSelectedTo.plusMinutes(1);
        } else {
          dateChangedTo = dateSelectedTo.minusMinutes(1);
        }
        timeRangeRelativePanel.getJLabelPromptTextTo()
            .setText(dateChangedTo.format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH)));
        previousValueTo = countTo - addCountTo;
      } else if (timeRangeRelativePanel.getJComboBoxTo().getSelectedItem().equals("Hours ago")) {
        countTo++;
        if (countTo - addCountTo > previousValueTo) {
          dateChangedTo = dateSelectedTo.plusHours(1);
        } else {
          dateChangedTo = dateSelectedTo.minusHours(1);
        }
        timeRangeRelativePanel.getJLabelPromptTextTo()
            .setText(dateChangedTo.format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH)));
        previousValueTo = countTo - addCountTo;
      } else if (timeRangeRelativePanel.getJComboBoxTo().getSelectedItem().equals("Days ago")) {
        countTo++;
        if (countTo - addCountTo > previousValueTo) {
          dateChangedTo = dateSelectedTo.plusDays(1);
        } else {
          dateChangedTo = dateSelectedTo.minusDays(1);
        }
        timeRangeRelativePanel.getJLabelPromptTextTo()
            .setText(dateChangedTo.format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH)));
        previousValueTo = countTo - addCountTo;
      }
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    JXTable source = (JXTable) e.getSource();

    this.setSourceConfig(source);

    log.info("Source config (relative): " + sourceConfig);
  }

  @Override
  public void mousePressed(MouseEvent e) {
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }

}