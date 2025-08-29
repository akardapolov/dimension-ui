package ru.dimension.ui.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class FilesHelper {

  private String fileSeparator;
  private String rootDir;

  private String configDir;
  private String databaseDir;
  private String externalDir;
  private String reportDir;
  private String templateDir;
  private String designDir;
  private String colorsDir;

  public static String PROFILES_DIR_NAME = "profiles";
  public static String TASKS_DIR_NAME = "tasks";
  public static String CONNECTIONS_DIR_NAME = "connections";
  public static String QUERIES_DIR_NAME = "queries";
  public static String TABLES_DIR_NAME = "tables";
  public static String COLORS_DIR_NAME = "colors";

  public static String CONFIG_JSON_DIR_NAME = "json";
  public static String CONFIG_FTL_DIR_NAME = "ftl";
  public static String CONFIG_TTF_DIR_NAME = "ttf";
  public static String FILE_SEPARATOR_JAVA = "/";

  public FilesHelper(String rootDir) {
    try {
      setRootDir(rootDir);
      setFileSeparator(System.getProperty("file.separator"));
      setConfigDir(getRootDir() + getFileSeparator() + "configuration");
      setDatabaseDir(getRootDir() + getFileSeparator() + "database");
      setExternalDir(getRootDir() + getFileSeparator() + "external");
      setReportDir(getRootDir() + getFileSeparator() + "report-data");
      setTemplateDir(getReportDir() + getFileSeparator() + "templates");
      setDesignDir(getReportDir() + getFileSeparator() + "design");
      setColorsDir(getConfigDir() + getFileSeparator() + COLORS_DIR_NAME);

      createDirectory(configDir);
      createDirectory(databaseDir);
      createDirectory(externalDir);
      createDirectory(reportDir);
      createDirectory(templateDir);
      createDirectory(designDir);
      createDirectory(colorsDir);
    } catch (IOException e) {
      log.error(e.getMessage());
      log.error(Arrays.toString(e.getStackTrace()));
    }
  }

  public void createDirectory(String directoryName) throws IOException {
    Files.createDirectories(Paths.get(directoryName));
  }

  public void createExternalDirectory(String directoryName) throws IOException {
    Path path = Paths.get(externalDir + fileSeparator + directoryName);
    if (!Files.exists(path)) {
      Files.createDirectories(path);
    }
  }

  public void createConfigDirectory(String directoryName) throws IOException {
    Path newPath = Paths.get(configDir + fileSeparator + directoryName);
    if (!Files.exists(newPath)) {
      Files.createDirectories(newPath);
    }
  }

  public void createFile(String fileName) throws IOException {
    Path newFilePath = Paths.get(fileName);
    if (!Files.exists(newFilePath)) {
      Files.createFile(newFilePath);
    }
  }

  public List<Path> listFilesInDirectory(String dirName) throws IOException {
    try (Stream<Path> paths = Files.walk(Paths.get(dirName))) {
      return paths.filter(Files::isRegularFile)
          .collect(Collectors.toList());
    }
  }

  public Path getFilePathDirectory(String typeName) {
    return Paths.get(getConfigDir() + fileSeparator + typeName);
  }

  public Path getFilePathTemplate(String fileName) {
    return Paths.get(getReportDir() + fileSeparator + "templates");
  }

  public String getFilePathFont(String fileName) {
    return getReportDir() + fileSeparator + "templates" + fileSeparator + fileName;
  }

  public boolean isJar() throws URISyntaxException {
    return getJarPath().endsWith(".jar");
  }

  private String getJarPath() throws URISyntaxException {
    return getClass().getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .toURI()
        .getPath();
  }

  public List<Path> getFilePathDirectoryResourcesJar(String configTypeName) throws URISyntaxException, IOException {
    List<Path> result = new ArrayList<>();

    String jarPath = getJarPath();

    URI uri = URI.create("jar:file:" + jarPath);

    try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
      try (Stream<Path> paths = Files.walk(fs.getPath(configTypeName))) {
        paths.filter(Files::isRegularFile).forEach(result::add);
      } catch (IOException e) {
        log.catching(e);
        return Collections.emptyList();
      }
    }

    return result;
  }

  public List<String> getFileContentDirectoryResourcesJar(String configTypeName)
      throws URISyntaxException, IOException {
    List<String> result = new ArrayList<>();

    String jarPath = getJarPath();

    URI uri = URI.create("jar:file:" + jarPath);

    try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
      try (Stream<Path> paths = Files.walk(fs.getPath(configTypeName))) {
        paths.filter(Files::isRegularFile).forEach(path -> {
          result.add(getFileFromResource(path.toString()));
        });
      } catch (IOException e) {
        log.catching(e);
        return Collections.emptyList();
      }
    }

    return result;
  }

  public List<Path> getFilePathDirectoryResourcesFromFS(String configTypeName) throws URISyntaxException, IOException {
    List<Path> result = new ArrayList<>();

    String jarPath = getJarPath();

    URI uri = URI.create("file://" + jarPath + FILE_SEPARATOR_JAVA + configTypeName);
    try (Stream<Path> paths = Files.walk(Paths.get(uri))) {
      paths.filter(Files::isRegularFile).forEach(result::add);
    } catch (IOException e) {
      log.catching(e);
      return Collections.emptyList();
    }

    return result;
  }

  private String getFileFromResource(String fileName) {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream is = classLoader.getResourceAsStream(fileName);

    if (is == null) {
      throw new IllegalArgumentException("file not found! " + fileName);
    }

    StringBuilder stringBuilder = new StringBuilder();

    try (InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader)) {

      String line;
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return stringBuilder.toString();
  }

  public FileWriter getFileWriter(String typeName,
                                  String fileName) throws IOException {
    return new FileWriter(getConfigDir() + fileSeparator + typeName + fileSeparator + fileName);
  }

  public FileWriter getFileWriterCommon(String dirName,
                                        String fileName) throws IOException {
    return new FileWriter(dirName + fileSeparator + fileName);
  }

  public boolean ifFileExist(String typeName,
                             String fileName) {
    File f = new File(getConfigDir() + fileSeparator + typeName + fileSeparator + fileName);
    return f.exists() && f.isFile();
  }

  public boolean ifFileExistCommon(String dirName,
                                   String fileName) {
    File f = new File(dirName + fileSeparator + fileName);
    return f.exists() && f.isFile();
  }

  public void removeConfigFile(String configFileName) throws IOException {
    Files.deleteIfExists(Paths.get(getConfigDir() + getFileSeparator() + configFileName));
  }

  public void removeDesignFile(String configFileName) throws IOException {
    Files.deleteIfExists(Paths.get(getDesignDir() + getFileSeparator() + configFileName));
  }

  public void removeEntityConfig(String typeName,
                                 String fileName) throws IOException {
    File f = new File(getConfigDir() + fileSeparator + typeName + fileSeparator + fileName);
    Files.deleteIfExists(Paths.get(f.getPath()));
  }

  public void cleanDir() {
    deleteDirectory(Paths.get(configDir).toFile());
    deleteDirectory(Paths.get(databaseDir).toFile());
  }

  boolean deleteDirectory(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file);
      }
    }
    return directoryToBeDeleted.delete();
  }

  public void loadFileToFolder(String filename,
                               String folderPath) throws IOException {
    String[] fileNameSplit = filename.split("\\.");

    try {
      if (isJar()) {
        List<Path> pathList = Collections.emptyList();
        if (CONFIG_FTL_DIR_NAME.equals(fileNameSplit[1])) {
          pathList = getFilePathDirectoryResourcesJar(CONFIG_FTL_DIR_NAME);
        } else if (CONFIG_TTF_DIR_NAME.equals(fileNameSplit[1])) {
          pathList = getFilePathDirectoryResourcesJar(CONFIG_TTF_DIR_NAME);
        }

        pathList.forEach(file -> {
          Path targetPath = Path.of(folderPath, filename);
          ClassLoader classLoader = getClass().getClassLoader();
          try (InputStream is = classLoader.getResourceAsStream(file.toString())) {
            if (is == null) {
              throw new IllegalArgumentException("Resource not found: " + file);
            }
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException e) {
            log.catching(e);
          }
        });
      } else {
        List<Path> pathList = Collections.emptyList();
        if (CONFIG_FTL_DIR_NAME.equals(fileNameSplit[1])) {
          pathList = getFilePathDirectoryResourcesFromFS(CONFIG_FTL_DIR_NAME);
        } else if (CONFIG_TTF_DIR_NAME.equals(fileNameSplit[1])) {
          pathList = getFilePathDirectoryResourcesFromFS(CONFIG_TTF_DIR_NAME);
        }

        pathList.forEach(file -> {
          Path targetPath = Path.of(folderPath, filename);
          try {
            Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException e) {
            log.catching(e);
          }
        });
      }
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void loadFileToFolder(String filename, String resourceDir, String folderPath) throws IOException {
    try {
      if (isJar()) {
        List<Path> pathList = getFilePathDirectoryResourcesJar(resourceDir);

        pathList.forEach(file -> {
          // Check if the file matches the requested filename
          if (file.getFileName().toString().equals(filename)) {
            Path targetPath = Path.of(folderPath, filename);
            ClassLoader classLoader = getClass().getClassLoader();

            // Construct the full resource path including the directory
            String fullResourcePath = resourceDir + "/" + filename;
            try (InputStream is = classLoader.getResourceAsStream(fullResourcePath)) {
              if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + fullResourcePath);
              }
              Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
              log.catching(e);
            }
          }
        });
      } else {
        List<Path> pathList = getFilePathDirectoryResourcesFromFS(resourceDir);

        pathList.forEach(file -> {
          if (file.getFileName().toString().equals(filename)) {
            Path targetPath = Path.of(folderPath, filename);
            try {
              Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
              log.catching(e);
            }
          }
        });
      }
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
