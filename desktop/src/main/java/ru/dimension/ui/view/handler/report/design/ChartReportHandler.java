package ru.dimension.ui.view.handler.report.design;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.component.chart.HelperChart;


@Log4j2
public abstract class ChartReportHandler implements HelperChart {

  protected ExecutorService executorService;
  protected final ProfileManager profileManager;
  protected final EventListener eventListener;
  protected final DStore fStore;


  public ChartReportHandler(ProfileManager profileManager,
                            EventListener eventListener,
                            DStore fStore
  ) {

    this.profileManager = profileManager;
    this.eventListener = eventListener;
    this.fStore = fStore;
    this.executorService = Executors.newSingleThreadExecutor();
  }
}
