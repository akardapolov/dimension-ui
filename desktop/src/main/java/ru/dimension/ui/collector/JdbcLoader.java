package ru.dimension.ui.collector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import org.apache.logging.log4j.Logger;

public interface JdbcLoader {

  default long getSysdate(String statement,
                          Connection connection,
                          Logger log) {
    long sysdate = 0;

    log.info("Query to get sysdate: {}", statement);

    try (PreparedStatement ps = connection.prepareStatement(statement)) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Object obj = rs.getObject(1);

          if (obj == null) {
            continue;
          }

          log.info("DB returned class: {}", obj.getClass().getName());

          switch (obj) {
            case Timestamp      ts ->     sysdate = ts.getTime();
            case OffsetDateTime odt ->    sysdate = odt.toInstant().toEpochMilli();
            case LocalDateTime  ldt ->    sysdate = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            case ZonedDateTime  zdt ->    sysdate = zdt.toInstant().toEpochMilli();
            case Instant        inst ->   sysdate = inst.toEpochMilli();
            case Date           date ->   sysdate = date.getTime();
            default -> log.warn("Unknown date object type: {} - Value: {}", obj.getClass().getName(), obj);
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