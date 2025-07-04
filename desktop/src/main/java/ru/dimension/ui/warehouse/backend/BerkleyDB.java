package ru.dimension.ui.warehouse.backend;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BerkleyDB {

  @Getter
  @Setter
  String directory;
  @Getter
  private EnvironmentConfig envConfig;
  @Getter
  private Environment env;
  @Getter
  private StoreConfig storeConfig;
  @Getter
  private EntityStore store;

  public BerkleyDB(String directory) {
    this.directory = directory;
    this.createDirectory();
    this.setupEnvConfig();
    this.setupEnvironment();
    this.setupStoreConfig();
  }

  @SneakyThrows
  public void createDirectory() {
    if (!Files.exists(Path.of(directory))) {
      Files.createDirectories(Path.of(directory));
    }
  }

  @SneakyThrows
  public void cleanDirectory() {
    if (Files.exists(Path.of(directory))) {
      Files.walk(Path.of(directory))
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  @SneakyThrows
  private void setupEnvConfig() {
    this.envConfig = new EnvironmentConfig();
    this.envConfig.setAllowCreate(true);
    this.envConfig.setTransactional(false);
    this.envConfig.setCachePercent(20);
  }

  @SneakyThrows
  private void setupEnvironment() {
    this.env = new Environment(new File(this.directory), envConfig);
  }

  @SneakyThrows
  private void setupStoreConfig() {
    this.storeConfig = new StoreConfig();
    this.storeConfig.setAllowCreate(true);
    this.storeConfig.setTransactional(false);
    this.storeConfig.setDeferredWrite(true);

    this.store = new EntityStore(this.env, "ash.db", this.storeConfig);
  }

  @SneakyThrows
  public void closeDatabase() {
    this.getStore().close();
    this.getEnv().close();
  }

  public void syncDatabase() {
    this.getStore().sync();
    this.getEnv().sync();
  }

  @SneakyThrows
  public void removeDirectory() {
    Files.walk(Path.of(directory))
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

}
