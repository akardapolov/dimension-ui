package ru.dimension.ui.manager.impl;

import java.io.IOException;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.helper.GsonHelper;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.model.config.ColorProfile;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Table;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.security.EncryptDecrypt;

@Log4j2
@Singleton
public class ConfigurationManagerImpl implements ConfigurationManager {

  private final GsonHelper gsonHelper;

  @Inject
  public ConfigurationManagerImpl(GsonHelper gsonHelper,
                                  EncryptDecrypt encryptDecrypt) {
    this.gsonHelper = gsonHelper;

    try {
      saveEmptyConfigToFileIfNotExist(Connection.class);
      saveEmptyConfigToFileIfNotExist(Query.class);
      saveEmptyConfigToFileIfNotExist(Task.class);
      saveEmptyConfigToFileIfNotExist(Profile.class);
      saveEmptyConfigToFileIfNotExist(Table.class);
      saveEmptyConfigToFileIfNotExist(ColorProfile.class);
    } catch (IOException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  private <T> void saveEmptyConfigToFileIfNotExist(Class<T> clazz) throws IOException {
    if (getConfigList(clazz).stream().findAny().isEmpty()) {
      gsonHelper.createConfigDir(clazz);
    }
  }

  @Override
  public <T> void saveConfigList(List<T> configList,
                                 Class<T> clazz) {
    configList.forEach(e -> {
      try {
        gsonHelper.add(e, clazz);
      } catch (IOException ex) {
        log.catching(ex);
        throw new RuntimeException(ex);
      }
    });
  }

  @Override
  public <T> List<T> getConfigList(Class<T> clazz) {
    return gsonHelper.getConfigList(clazz);
  }

  @Override
  public <T> void addConfig(T config,
                            Class<T> clazz) {
    try {
      gsonHelper.add(config, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> void updateConfig(T config,
                               Class<T> clazz) {
    try {
      gsonHelper.update(config, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T getConfig(Class<T> clazz,
                         String fileName) {
    return gsonHelper.getConfig(clazz, fileName);
  }

  @Override
  public <T> void deleteConfig(T config,
                               Class<T> clazz) {
    try {
      gsonHelper.delete(config, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> void deleteConfig(String entityName,
                               Class<T> clazz) {
    try {
      gsonHelper.delete(entityName, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
