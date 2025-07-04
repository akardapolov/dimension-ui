package ru.dimension.ui.view.detail.raw;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.config.prototype.detail.WorkspaceRawModule;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.detail.RawDataPanelCommon;

@Log4j2
public class RawDataPanel extends RawDataPanelCommon {

  private final WorkspaceQueryComponent workspaceQueryComponent;

  @Inject
  @Named("localDB")
  DStore dStore;

  public RawDataPanel(WorkspaceQueryComponent workspaceQueryComponent,
                      TableInfo tableInfo,
                      CProfile cProfile,
                      long begin,
                      long end,
                      boolean useFetchSize) {
    super(tableInfo, cProfile, useFetchSize);

    this.workspaceQueryComponent = workspaceQueryComponent;
    this.workspaceQueryComponent.initRaw(new WorkspaceRawModule(this)).inject(this);

    this.loadResultSet(tableInfo.getTableName(), begin, end);
    this.loadRawData(tableInfo.getTableName(), begin, end);
  }

  @Override
  protected void loadResultSet(String tableName,
                               long begin,
                               long end) {
    if (useFetchSize) {
      batchResultSet = dStore.getBatchResultSet(tableName, begin, end, fetchSize);
    }
  }

  @Override
  protected void loadRawData(String tableName,
                           long begin,
                           long end) {
    log.info("Parameters begin: {}, end: {}", getDate(begin), getDate(end));

    List<List<Object>> rawData;
    if (useFetchSize) {
      rawData = batchResultSet.next() ? batchResultSet.getObject() : Collections.emptyList();
    } else {
      rawData = dStore.getRawDataAll(tableName, begin, end);
    }

    if (!rawData.isEmpty()) {
      hasData = true;
    }

    loadToModel(rawData);
  }
}
