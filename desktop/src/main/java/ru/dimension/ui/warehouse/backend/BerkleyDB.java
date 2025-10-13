package ru.dimension.ui.warehouse.backend;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
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

  public void createDirectory() {
    try {
      if (!Files.exists(Path.of(directory))) {
        Files.createDirectories(Path.of(directory));
      }
    } catch (IOException e) {
      log.error("Failed to create directory: {}", directory, e);
      throw new RuntimeException("Failed to create directory: " + e.getMessage(), e);
    }
  }

  public void cleanDirectory() {
    Path dirPath = Path.of(directory);
    if (!Files.exists(dirPath)) {
      log.info("Directory {} does not exist, nothing to clean", directory);
      return;
    }

    try (Stream<Path> pathStream = Files.walk(dirPath)) {
      pathStream
          .filter(path -> !path.equals(dirPath))
          .sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.delete(path);
            } catch (IOException e) {
              throw new RuntimeException("Failed to delete: " + path, e);
            }
          });
    } catch (IOException e) {
      log.error("Failed to clean directory: {}", directory, e);
      throw new RuntimeException("Failed to clean directory: " + e.getMessage(), e);
    }
  }

  private void setupEnvConfig() {
    try {
      this.envConfig = new EnvironmentConfig();
      this.envConfig.setAllowCreate(true);
      this.envConfig.setTransactional(false);
      this.envConfig.setCachePercent(20);
    } catch (Exception e) {
      log.error("Failed to setup environment configuration", e);
      throw new RuntimeException("Failed to setup environment configuration: " + e.getMessage(), e);
    }
  }

  private void setupEnvironment() {
    try {
      this.env = new Environment(new File(this.directory), envConfig);
    } catch (DatabaseException e) {
      log.error("Failed to setup environment for directory: {}", directory, e);
      throw new RuntimeException("Failed to setup environment: " + e.getMessage(), e);
    }
  }

  private void setupStoreConfig() {
    try {
      this.storeConfig = new StoreConfig();
      this.storeConfig.setAllowCreate(true);
      this.storeConfig.setTransactional(false);
      this.storeConfig.setDeferredWrite(true);

      this.store = new EntityStore(this.env, "ash.db", this.storeConfig);
    } catch (Exception e) {
      log.error("Failed to setup store configuration", e);
      throw new RuntimeException("Failed to setup store configuration: " + e.getMessage(), e);
    }
  }

  public void closeDatabase() {
    try {
      if (this.store != null) {
        this.store.close();
      }
      if (this.env != null) {
        this.env.close();
      }
    } catch (Exception e) {
      log.error("Failed to close database", e);
      throw new RuntimeException("Failed to close database: " + e.getMessage(), e);
    }
  }

  public void syncDatabase() {
    try {
      if (this.store != null) {
        this.store.sync();
      }
      if (this.env != null) {
        this.env.sync();
      }
    } catch (Exception e) {
      log.error("Failed to sync database", e);
      throw new RuntimeException("Failed to sync database: " + e.getMessage(), e);
    }
  }

  public void removeDirectory() {
    Path dirPath = Path.of(directory);
    if (!Files.exists(dirPath)) {
      log.info("Directory {} does not exist, nothing to remove", directory);
      return;
    }

    try (Stream<Path> pathStream = Files.walk(dirPath)) {
      pathStream
          .sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.delete(path);
            } catch (IOException e) {
              throw new RuntimeException("Failed to delete: " + path, e);
            }
          });
    } catch (IOException e) {
      log.error("Failed to remove directory: {}", directory, e);
      throw new RuntimeException("Failed to remove directory: " + e.getMessage(), e);
    }
  }
}