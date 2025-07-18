package ru.dimension.ui.model.info;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.hc.core5.http.Method;
import ru.dimension.ui.model.db.DBType;
import ru.dimension.ui.model.parse.ParseType;
import ru.dimension.ui.model.type.ConnectionType;

@ToString(exclude = "password")
@Data
@Accessors(chain = true)
public class ConnectionInfo {

  private int id;
  private String name;
  private String userName;
  private String password;
  private String url;
  private String jar;
  private String driver;
  private DBType dbType;

  private ConnectionType type;

  private Method httpMethod;
  private ParseType parseType;
}
