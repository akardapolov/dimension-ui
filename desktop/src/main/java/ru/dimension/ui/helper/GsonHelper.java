package ru.dimension.ui.helper;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.exception.FileNameExistException;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.UnknownConfigClassException;
import ru.dimension.ui.model.config.ConfigClasses;
import ru.dimension.ui.model.config.ConfigEntity;
import ru.dimension.ui.model.config.Table;
import ru.dimension.ui.model.report.DesignReportData;

@Log4j2
@Singleton
public class GsonHelper {

  private final FilesHelper filesHelper;
  private final ReportHelper reportHelper;
  private final Gson gson;


  @Inject
  public GsonHelper(FilesHelper filesHelper,
                    Gson gson,
                    ReportHelper reportHelper) {
    this.filesHelper = filesHelper;
    this.reportHelper = reportHelper;
    this.gson = gson;
  }

  public <T> void createConfigDir(Class<T> clazz) throws IOException {
    filesHelper.createConfigDirectory(getEntityTypeName(clazz));
  }

  public <T> void add(T entity,
                      Class<T> clazz) throws IOException {
    if (filesHelper.ifFileExist(getEntityTypeName(clazz), getFileName(entity, clazz))) {
      throw new FileNameExistException("File name: " + getFileName(entity, clazz) +
                                           " already exist. Please choose another one..");
    }

    FileWriter fileWriter = filesHelper.getFileWriter(getEntityTypeName(clazz), getFileName(entity, clazz));
    gson.toJson(entity, fileWriter);

    fileWriter.close();
  }

  public void saveDesign(DesignReportData designData) throws IOException {
    String dirName = filesHelper.getDesignDir() + filesHelper.getFileSeparator() + designData.getFolderName();
    Files.createDirectories(Paths.get(dirName));

    String fileName = "design.json";
    FileWriter fileWriter = filesHelper.getFileWriterCommon(dirName, fileName);
    gson.toJson(designData, fileWriter);
    fileWriter.close();
  }

  public DesignReportData loadDesign(String folderName) throws IOException {
    Path dirPath = Paths.get(filesHelper.getDesignDir() + filesHelper.getFileSeparator() + folderName);
    Path file = PathHelper.searchRegularFilesStartsWith(dirPath, "design", ".json")
        .stream().findAny().orElseThrow(() ->
                                            new NotFoundException("Design file not found in " + dirPath));

    try (FileReader fileReader = new FileReader(file.toFile())) {
      return gson.fromJson(fileReader, DesignReportData.class);
    }
  }

  public <T> void update(T entity,
                         Class<T> clazz) throws IOException {
    FileWriter fileWriter = filesHelper.getFileWriter(getEntityTypeName(clazz), getFileName(entity, clazz));
    gson.toJson(entity, fileWriter);

    fileWriter.close();
  }

  public <T> List<T> getConfigListResources(Class<T> clazz) {
    List<T> entityList = new ArrayList<>();

    try {
      if (filesHelper.isJar()) {
        List<String> pathList = filesHelper.getFileContentDirectoryResourcesJar(
            FilesHelper.CONFIG_JSON_DIR_NAME + FilesHelper.FILE_SEPARATOR_JAVA + getEntityTypeName(clazz));
        pathList.forEach(file -> {
          T object = gson.fromJson(file, clazz);

          if (Objects.nonNull(object)) {
            entityList.add(object);
          }
        });
      } else {
        List<Path> pathList = filesHelper.getFilePathDirectoryResourcesFromFS(
            FilesHelper.CONFIG_JSON_DIR_NAME + FilesHelper.FILE_SEPARATOR_JAVA + getEntityTypeName(clazz));
        pathList.forEach(file -> addFileToEntityList(clazz, file, entityList));
      }
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }

    return entityList;
  }

  public <T> List<T> getConfigList(Class<T> clazz) {
    Path filePath = filesHelper.getFilePathDirectory(getEntityTypeName(clazz));

    List<T> entityList = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(filePath)) {
      paths.filter(Files::isRegularFile)
          .forEach(file -> addFileToEntityList(clazz, file, entityList));
    } catch (IOException e) {
      log.catching(e);
      return Collections.emptyList();
    }

    return entityList;
  }

  public <T> void addFileToEntityList(Class<T> clazz,
                                      Path file,
                                      List<T> entityList) {
    try (FileReader fileReader = new FileReader(file.toFile())) {

      T object = gson.fromJson(fileReader, clazz);

      if (Objects.nonNull(object)) {
        entityList.add(object);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> T getConfig(Class<T> clazz,
                         String fileName) {
    Path dirPath = filesHelper.getFilePathDirectory(getEntityTypeName(clazz));

    Path file;
    try {
      file = PathHelper.searchRegularFilesStartsWith(dirPath, normalizeFileName(fileName), ".json").
          stream().findAny().orElseThrow(() ->
                                             new NotFoundException(
                                                 "File: " + normalizeFileName(fileName) + ".json not found in "
                                                     + dirPath));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try (FileReader fileReader = new FileReader(file.toFile())) {

      T object = gson.fromJson(fileReader, clazz);

      if (Objects.nonNull(object)) {
        return object;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    throw new RuntimeException("Configuration file:" + fileName +
                                   " not found in folder: " + getEntityTypeName(clazz));
  }

  private <T> String getEntityTypeName(Class<T> clazz) {
    return switch (ConfigClasses.fromClass(clazz)) {
      case Profile -> FilesHelper.PROFILES_DIR_NAME;
      case Task -> FilesHelper.TASKS_DIR_NAME;
      case Connection -> FilesHelper.CONNECTIONS_DIR_NAME;
      case Query -> FilesHelper.QUERIES_DIR_NAME;
      case Table -> FilesHelper.TABLES_DIR_NAME;
      case ColorProfile -> FilesHelper.COLORS_DIR_NAME;
      default -> throw new UnknownConfigClassException("");
    };
  }

  private <T> String getFileName(T entity,
                                 Class<T> clazz) {
    if (ConfigClasses.fromClass(clazz) != ConfigClasses.Table) {
      ConfigEntity configEntity = (ConfigEntity) entity;
      return normalizeFileName(configEntity.getName()) + ".json";
    } else {
      Table baseEntity = (Table) entity;
      return normalizeFileName(baseEntity.getTableName()) + ".json";
    }
  }

  private <T> String getFileName(String entityName,
                                 Class<T> clazz) {
    return normalizeFileName(entityName) + ".json";
  }

  private String normalizeFileName(String fileName) {
    return fileName.replaceAll("[\\\\/:;$*?\"<>|]", "_")
        .replace(" ", "_")
        .toLowerCase();
  }

  public void deleteConfig(String configName) throws IOException {
    filesHelper.removeConfigFile(configName);
  }

  public void deleteDesign(String configName) throws IOException {
    filesHelper.removeDesignFile(configName);
  }

  public <T> void delete(T entity,
                         Class<T> clazz) throws IOException {
    filesHelper.removeEntityConfig(getEntityTypeName(clazz), getFileName(entity, clazz));
  }

  public <T> void delete(String entityName,
                         Class<T> clazz) throws IOException {
    filesHelper.removeEntityConfig(getEntityTypeName(clazz), getFileName(entityName, clazz));
  }
}
