package ru.dimension.ui.view.handler.report.report;

import dagger.Lazy;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ReportHelper;
import ru.dimension.ui.model.column.ReportColumnNames;
import ru.dimension.ui.view.BaseFrame;
import ru.dimension.ui.view.panel.report.ReportTabsPane;
import ru.dimension.ui.view.panel.report.pdf.PathPdfInfo;
import ru.dimension.ui.view.panel.report.pdf.PdfViewer;

@Log4j2
@Singleton
public class ReportPanelHandler implements ActionListener, ListSelectionListener {

  private final ReportTabsPane reportTabsPane;
  private final JFileChooser saveFC;
  private final Lazy<BaseFrame> jFrame;
  private final PathPdfInfo reportPdfPath;
  private final FilesHelper filesHelper;
  private final ReportHelper reportHelper;
  private PdfViewer pdfViewer;

  @Inject
  public ReportPanelHandler(@Named("reportTaskPanel") ReportTabsPane reportTabsPane,
                            @Named("reportPdfPath") PathPdfInfo reportPdfPath,
                            FilesHelper filesHelper,
                            ReportHelper reportHelper,
                            Lazy<BaseFrame> jFrame) {
    this.reportTabsPane = reportTabsPane;
    this.reportPdfPath = reportPdfPath;

    this.filesHelper = filesHelper;
    this.reportHelper = reportHelper;

    this.jFrame = jFrame;

    this.saveFC = new JFileChooser();
    this.saveFC.setDialogTitle("Сохранить PDF файл");
    this.saveFC.setFileFilter(new FileNameExtensionFilter("PDF файлы", "pdf"));
    this.saveFC.setSelectedFile(new File("report.pdf"));

    this.pdfViewer = new PdfViewer();

    this.reportTabsPane.getSaveBtnPDFReport().addActionListener(this);
    this.reportTabsPane.getDelBtnReport().addActionListener(this);
    this.reportTabsPane.getSavedReportCase().getJxTable().getSelectionModel().addListSelectionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == reportTabsPane.getSaveBtnPDFReport()) {
      int userSelection = saveFC.showSaveDialog(jFrame.get());
      if (userSelection == JFileChooser.APPROVE_OPTION) {

        String savePath = saveFC.getSelectedFile().getAbsolutePath();

        try {
          Path sourcePath = Paths.get(reportPdfPath.getReportPdfPath());
          Path targetPath = Paths.get(savePath);

          Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

          JOptionPane.showMessageDialog(null,
                                        "PDF file successfully saved to the selected path: " + savePath);

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    if (event.getSource() == reportTabsPane.getDelBtnReport()) {

      int selectedRow = reportTabsPane.getSavedReportCase().getJxTable().getSelectedRow();

      if (selectedRow == -1) {
        JOptionPane.showMessageDialog(null, "Not selected design. Please select and try again!",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      } else {
        String message =
            "Do you want to delete configuration: " + reportTabsPane.getSavedReportCase().getDefaultTableModel()
                .getValueAt(selectedRow, 0) + "?";

        int input = JOptionPane.showConfirmDialog(new JDialog(),
                                                  message, "Information", JOptionPane.YES_NO_OPTION);

        if (input == 0) {
          String dirName = reportPdfPath.getDirDesignName();
          String folderPath = filesHelper.getDesignDir() + filesHelper.getFileSeparator() + dirName;

          reportTabsPane.getSavedReportCase().getDefaultTableModel().removeRow(selectedRow);
          reportTabsPane.getSavedReportCase().getJxTable().clearSelection();

          reportTabsPane.getScrollPanePDF().getViewport().removeAll();
          reportTabsPane.getScrollPanePDF().getViewport().revalidate();

          reportTabsPane.getDelBtnReport().setEnabled(false);
          reportTabsPane.getSaveBtnPDFReport().setEnabled(false);

          File folder = new File(folderPath);
          if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
              for (File file : files) {
                if (file.getName().endsWith(".pdf")) {
                  pdfViewer.closePdfFile();
                  file.delete();
                }
              }
            }
          }

        }
      }
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

    if (!e.getValueIsAdjusting()) {
      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing profile fields");
      } else {

        if (e.getSource() == reportTabsPane.getSavedReportCase().getJxTable().getSelectionModel()) {

          String designDir = GUIHelper.getNameByColumnName(reportTabsPane.getSavedReportCase().getJxTable(),
                                                           reportTabsPane.getSavedReportCase().getDefaultTableModel(),
                                                           listSelectionModel, ReportColumnNames.REPORT_NAME.getColName());
          String dateStr = designDir.substring(designDir.indexOf("-") + 1).trim();
          LocalDateTime dateTime = LocalDateTime.parse(dateStr, reportHelper.getDateTimeFormatter());
          String folderDate = dateTime.format(reportHelper.getDateTimeFormatterFused());
          String folderName = "design_" + folderDate;
          reportPdfPath.setDirDesignName(folderName);

          File designFolder = new File(filesHelper.getDesignDir() + filesHelper.getFileSeparator() + folderName);
          if (designFolder.exists() && designFolder.isDirectory()) {
            File[] files = designFolder.listFiles();
            if (files != null) {
              for (File file : files) {

                if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                  String fileReportPath = filesHelper.getDesignDir()
                      + filesHelper.getFileSeparator() + folderName
                      + filesHelper.getFileSeparator() + file.getName();

                  try {
                    pdfViewer = new PdfViewer(new File(fileReportPath));
                    reportTabsPane.getScrollPanePDF().setViewportView(pdfViewer);
                  } catch (Exception ex) {
                    throw new RuntimeException(ex);
                  }
                  reportTabsPane.getSaveBtnPDFReport().setEnabled(true);
                }
              }
            }
          }
          reportTabsPane.getDelBtnReport().setEnabled(true);
        }
      }
    }
  }
}
