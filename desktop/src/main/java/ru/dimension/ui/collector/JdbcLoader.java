package ru.dimension.ui.collector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.logging.log4j.Logger;

public interface JdbcLoader {

  default long getSysdate(String statement,
                          Connection connection,
                          Logger log) {
    long sysdate = 0;

    log.info("Query to get sysdate: " + statement);

    try (PreparedStatement ps = connection.prepareStatement(statement)) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Object obj = rs.getObject(1);

          if (obj instanceof Timestamp ts) {
            sysdate = ts.getTime();
          } else if (obj instanceof LocalDateTime dt) {
            sysdate = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
          }
        }
      } catch (Exception eRs) {
        log.catching(eRs);
        throw new RuntimeException(eRs);
      }
    } catch (Exception ePs) {
      log.catching(ePs);
      throw new RuntimeException(ePs);
    }

    return sysdate;
  }
}
