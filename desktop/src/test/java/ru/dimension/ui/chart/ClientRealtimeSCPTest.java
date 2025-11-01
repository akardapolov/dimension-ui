package ru.dimension.ui.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.realtime.ClientRealtimeSCP;
import ru.dimension.ui.exception.SeriesExceedException;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.view.SeriesType;

@Log4j2
public class ClientRealtimeSCPTest extends AbstractBackendTest {

  private static String TABLE_NAME = "client_realtime_scp_test";

  private TProfile tProfile;

  private ChartConfig config;
  private ProfileTaskQueryKey profileTaskQueryKey;

  @BeforeAll
  public void init() {
    tProfile = getTProfile(TABLE_NAME);
    config = buildChartConfig(tProfile, GatherDataMode.BY_CLIENT_JDBC);
    profileTaskQueryKey = config.getChartKey().getProfileTaskQueryKey();

    initialDataLoad(tProfile, profileTaskQueryKey);

    log.info("Last timestamp is: {}", sqlQueryState.getLastTimestamp(profileTaskQueryKey));
  }

  @Test
  public void clientRealtimeSeriesExceedExceptionTest() {
    ClientRealtimeSCP clientRealtimeSCP = new ClientRealtimeSCP(
        sqlQueryState,
        dStore,
        config,
        profileTaskQueryKey,
        null
    );
    clientRealtimeSCP.initialize();

    Random random = new Random(0);
    long currentTime = System.currentTimeMillis();

    assertThrows(SeriesExceedException.class, () -> {
      sqlQueryState.setLastTimestamp(profileTaskQueryKey, currentTime);
      dStore.putDataDirect(tProfile.getTableName(), getData(tProfile, currentTime, random, true));

      log.info("##### Data  #####");
      List<List<Object>> data = dStore.getRawDataAll(tProfile.getTableName(), 0, sqlQueryState.getLastTimestamp(profileTaskQueryKey));
      log.info(data);
      log.info(data.size());

      clientRealtimeSCP.loadData();
    });

    clientRealtimeSCP.reinitializeChartInCustomMode();
    assertEquals(SeriesType.CUSTOM, clientRealtimeSCP.getSeriesType());
  }
}
