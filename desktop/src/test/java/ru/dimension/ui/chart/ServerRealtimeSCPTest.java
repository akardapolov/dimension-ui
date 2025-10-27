package ru.dimension.ui.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.dimension.ui.component.chart.HelperChart.THRESHOLD_SERIES;

import java.util.Random;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.dimension.db.exception.EnumByteExceedException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.realtime.ServerRealtimeSCP;
import ru.dimension.ui.exception.SeriesExceedException;
import ru.dimension.ui.helper.ColorHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.view.SeriesType;

// TODO remove disabled after make ColorHelper static or another way
@Log4j2
@Disabled
public class ServerRealtimeSCPTest extends AbstractBackendTest {

  private static String TABLE_NAME = "server_realtime_scp_test";

  private TProfile tProfile;
  private ChartConfig config;
  private ProfileTaskQueryKey profileTaskQueryKey;

  @BeforeAll
  public void init() {
    filesHelper = createMockFilesHelper();
    configurationManager = createMockConfigurationManagerWithNullProfile("nonexistent");

    colorHelper = new ColorHelper(filesHelper, configurationManager);

    tProfile = getTProfile(TABLE_NAME);
    config = buildChartConfig(tProfile, GatherDataMode.BY_SERVER_JDBC);
    profileTaskQueryKey = config.getChartKey().getProfileTaskQueryKey();

    initialDataLoad(tProfile, profileTaskQueryKey);

    log.info("Last timestamp is: {}", sqlQueryState.getLastTimestamp(profileTaskQueryKey));
  }

  @Test
  public void serverRealtimeSeriesExceedExceptionTest() throws Exception {
    ServerRealtimeSCP serverRealtimeSCP = new ServerRealtimeSCP(
        sqlQueryState, dStore, config, profileTaskQueryKey, null
    );
    serverRealtimeSCP.initialize();

    Random random = new Random(0);
    long currentTime = System.currentTimeMillis();

    // Load data exceeding series threshold
    for (int i = 0; i < THRESHOLD_SERIES + 5; i++) {
      long timestamp = currentTime + i * 1000;
      sqlQueryState.setLastTimestamp(profileTaskQueryKey, timestamp);
      dStore.putDataDirect(tProfile.getTableName(),
                           getData(tProfile, timestamp, random, true));
    }

    // Trigger data loading (should automatically handle exception)
    assertThrows(SeriesExceedException.class, serverRealtimeSCP::loadData);

    // Reinitialize in custom mode
    serverRealtimeSCP.reinitializeChartInCustomMode();

    // Verify automatic switch to custom mode
    assertEquals(SeriesType.CUSTOM, serverRealtimeSCP.getSeriesType());
  }

  @Test
  public void serverRealtimeCustomModeReinitializationTest() throws EnumByteExceedException, SqlColMetadataException {
    ServerRealtimeSCP serverRealtimeSCP = new ServerRealtimeSCP(
        sqlQueryState,
        dStore,
        config,
        profileTaskQueryKey,
        null
    );
    serverRealtimeSCP.initialize();

    // Force series count above threshold
    Random random = new Random(0);
    long timestamp = System.currentTimeMillis();
    for (int i = 0; i < THRESHOLD_SERIES + 5; i++) {
      sqlQueryState.setLastTimestamp(profileTaskQueryKey, timestamp);
      dStore.putDataDirect(tProfile.getTableName(), getData(tProfile, timestamp, random, true));
      timestamp += 1000;
    }

    // Reinitialize in custom mode
    serverRealtimeSCP.reinitializeChartInCustomMode();

    // Verify custom mode activation
    assertEquals(SeriesType.CUSTOM, serverRealtimeSCP.getSeriesType());

    // Verify UI components exist
    assertNotNull(serverRealtimeSCP.getSeriesSearch());
  }
}
