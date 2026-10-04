package ru.dimension.ui.collector.collect.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JmxProtocol {

  private boolean local;
  private String url;
  private String host;
  private Integer port;
  private String username;
  private String password;
}
