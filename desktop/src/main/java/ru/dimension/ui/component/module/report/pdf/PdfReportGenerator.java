package ru.dimension.ui.component.module.report.pdf;

import static ru.dimension.ui.helper.FilesHelper.CONFIG_FTL_DIR_NAME;
import static ru.dimension.ui.helper.FilesHelper.CONFIG_TTF_DIR_NAME;

import com.itextpdf.text.Document;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerFontProvider;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.module.chart.ReportChartModule;
import ru.dimension.ui.helper.DesignHelper;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;

@Log4j2
public class PdfReportGenerator {
  private final ProfileManager profileManager;
  private final FilesHelper filesHelper;

  public PdfReportGenerator(FilesHelper filesHelper,
                            ProfileManager profileManager) {
    this.filesHelper = filesHelper;
    this.profileManager = profileManager;

    ensureTemplateFilesExist();
  }

  private void ensureTemplateFilesExist() {
    String folderPath = filesHelper.getTemplateDir();
    boolean isEmpty = isFolderEmpty(folderPath);
    File folder = new File(folderPath);
    File[] files = folder.listFiles();
    boolean isTTFFile = false;
    boolean isFTLFile = false;

    if (isEmpty) {
      try {
        loadFileToFolder("default.ftl", folderPath);
        loadFileToFolder("arialuni.ttf", folderPath);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      for (File file : files) {
        if (file.getName().endsWith(".ftl")) {
          isFTLFile = true;
        }
        if (file.getName().endsWith(".ttf")) {
          isTTFFile = true;
        }
      }
      try {
        if (!isFTLFile) {
          loadFileToFolder("default.ftl", folderPath);
        }
        if (!isTTFFile) {
          loadFileToFolder("arialuni.ttf", folderPath);
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public boolean isFolderEmpty(String folderPath) {
    File folder = new File(folderPath);

    if (folder.isDirectory()) {
      String[] files = folder.list();
      return (files == null || files.length == 0);
    }
    return true;
  }

  public void loadFileToFolder(String filename,
                               String folderPath) throws IOException {
    String[] fileNameSplit = filename.split("\\.");

    try {
      if (filesHelper.isJar()) {
        java.util.List<Path> pathList = Collections.emptyList();
        if (CONFIG_FTL_DIR_NAME.equals(fileNameSplit[1])) {
          pathList = filesHelper.getFilePathDirectoryResourcesJar(CONFIG_FTL_DIR_NAME);
        } else if (CONFIG_TTF_DIR_NAME.equals(fileNameSplit[1])) {
          pathList = filesHelper.getFilePathDirectoryResourcesJar(CONFIG_TTF_DIR_NAME);
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
          pathList = filesHelper.getFilePathDirectoryResourcesFromFS(CONFIG_FTL_DIR_NAME);
        } else if (CONFIG_TTF_DIR_NAME.equals(fileNameSplit[1])) {
          pathList = filesHelper.getFilePathDirectoryResourcesFromFS(CONFIG_TTF_DIR_NAME);
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

  public File generateHtmlReport(Map<String, Object> dataReport,
                                 String folderName,
                                 String formattedDateForDir,
                                 int tabIndex,
                                 int pageCount) throws Exception {
    Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
    Path templatePath = filesHelper.getFilePathTemplate("default.ftl");
    cfg.setDirectoryForTemplateLoading(templatePath.toFile());
    cfg.setDefaultEncoding("UTF-8");

    StringWriter stringWriter = new StringWriter();
    Template template = cfg.getTemplate("default.ftl");
    template.process(dataReport, stringWriter);
    String html = stringWriter.toString();

    String designDir = filesHelper.getDesignDir() + filesHelper.getFileSeparator() + folderName;
    String templateFileName = String.format("template_%d_%d_%s.html", tabIndex, pageCount, formattedDateForDir);
    String filePath = designDir + filesHelper.getFileSeparator() + templateFileName;

    File file = new File(filePath);
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
      writer.write(html);
    }
    return file;
  }

  public void generatePdfReport(java.util.List<File> htmlFiles,
                                String folderName,
                                String formattedDateForDir) throws Exception {
    String designDir = filesHelper.getDesignDir() + filesHelper.getFileSeparator() + folderName;
    String reportFileName = String.format("report_%s.pdf", formattedDateForDir);
    String fileReportPath = designDir + filesHelper.getFileSeparator() + reportFileName;

    String fontPath = filesHelper.getFilePathFont("arialuni.ttf").toString();

    Document document = new Document(PageSize.A4);
    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileReportPath));
    document.open();

    XMLWorkerFontProvider fontProvider = new XMLWorkerFontProvider(XMLWorkerFontProvider.DONTLOOKFORFONTS);
    fontProvider.register(fontPath);
    FontFactory.setFontImp(fontProvider);

    for (File htmlFile : htmlFiles) {
      String contentHtml = new String(Files.readAllBytes(htmlFile.toPath()));
      InputStream inputStream = new ByteArrayInputStream(contentHtml.getBytes(StandardCharsets.UTF_8));
      XMLWorkerHelper.getInstance().parseXHtml(writer, document, inputStream, null, null, fontProvider);
      document.newPage();
    }
    document.close();
  }

  public Map<String, Object> createChartData(ProfileTaskQueryKey key,
                                             ReportChartModule chartModule,
                                             String folderName) {
    Map<String, Object> dataReport = new HashMap<>();

    int profileId = key.getProfileId();
    String profileName = profileManager.getProfileInfoById(profileId).getName();

    int taskId = key.getTaskId();
    String taskName = profileManager.getTaskInfoById(taskId).getName();

    int queryId = key.getQueryId();
    String queryName = profileManager.getQueryInfoById(queryId).getName();

    String dateFrom = getDateFormat(chartModule.getModel().getChartInfo().getCustomBegin());
    String dateTo = getDateFormat(chartModule.getModel().getChartInfo().getCustomEnd());

    String fileName = chartModule.getModel().getMetric().getName().trim().replace(" ", "_").toLowerCase();
    String description = chartModule.getModel().getDescription().getText();
    String nameFunction = chartModule.getModel().getMetric().getGroupFunction().name();

    String designDir = filesHelper.getDesignDir() + filesHelper.getFileSeparator() + folderName
        + filesHelper.getFileSeparator() + "profileId_" + profileId
        + "_taskId_" + taskId
        + "_queryId_" + queryId;

    String filePath = designDir + filesHelper.getFileSeparator() + fileName + ".png";

    BufferedImage image = new BufferedImage(chartModule.getWidth(),
                                            chartModule.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();
    chartModule.printAll(g2d);
    g2d.dispose();

    try {
      Files.createDirectories(Paths.get(designDir));
      File outputFile = new File(filePath);
      ImageIO.write(image, "png", outputFile);
      String pathPNG = designDir + filesHelper.getFileSeparator() + fileName + ".png";

      dataReport.put("profileName", profileName);
      dataReport.put("taskName", taskName);
      dataReport.put("queryName", queryName);
      dataReport.put("dateFrom", dateFrom);
      dataReport.put("dateTo", dateTo);
      dataReport.put("nameCard", chartModule.getModel().getMetric().getName());
      dataReport.put("nameFunction", nameFunction);
      dataReport.put("description", description);
      dataReport.put("pathChart", pathPNG);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    return dataReport;
  }

  public void deleteOldReportFiles(String folderName) throws IOException {
    String designDir = filesHelper.getDesignDir() + filesHelper.getFileSeparator() + folderName;
    Path designPath = Paths.get(designDir);

    if (Files.exists(designPath)) {
      Files.walk(designPath)
          .filter(path -> {
            String fileName = path.getFileName().toString();
            return fileName.endsWith(".pdf") || fileName.endsWith(".html");
          })
          .forEach(path -> {
            try {
              Files.deleteIfExists(path);
            } catch (IOException e) {
              log.warn("Failed to delete old report file: {}", path, e);
            }
          });
    }
  }

  public String getDateFormat(long date) {
    return Instant.ofEpochMilli(date)
        .atZone(ZoneId.systemDefault())
        .format(DesignHelper.getDateFormatFormatter());
  }
}
