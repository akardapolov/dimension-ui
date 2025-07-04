package ru.dimension.ui.manager;

import java.util.List;

public interface ConfigurationManager {

  <T> void saveConfigList(List<T> configList,
                          Class<T> clazz);

  <T> void addConfig(T config,
                     Class<T> clazz);

  <T> List<T> getConfigList(Class<T> clazz);

  <T> void updateConfig(T config,
                        Class<T> clazz);

  <T> T getConfig(Class<T> clazz,
                  String fileName);

  <T> void deleteConfig(T config,
                        Class<T> clazz);

  <T> void deleteConfig(String entityName,
                        Class<T> clazz);
}
