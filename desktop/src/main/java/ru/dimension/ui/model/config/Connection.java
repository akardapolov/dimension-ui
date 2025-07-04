package ru.dimension.ui.model.config;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.hc.core5.http.Method;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.parse.ParseType;

@EqualsAndHashCode(callSuper = true)
@Data
public class Connection extends ConfigEntity {

  private String userName;
  private String password;
  private String url;
  private String jar;
  private String driver;

  private ConnectionType type;

  private Method httpMethod;
  private ParseType parseType;
}
