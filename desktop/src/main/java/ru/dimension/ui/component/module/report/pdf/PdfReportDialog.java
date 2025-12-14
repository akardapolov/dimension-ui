package ru.dimension.ui.component.module.report.pdf;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import ru.dimension.ui.view.panel.report.pdf.PdfViewer;

public class PdfReportDialog extends JDialog {

  private final PdfViewer pdfViewer;
  private final File pdfFile;

  public PdfReportDialog(Frame parent, File pdfFile) throws Exception {
    super(parent, "PDF Report", true);
    this.pdfFile = pdfFile;
    this.pdfViewer = new PdfViewer(pdfFile);

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    initializeUI();
    installEscToClose();

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        pdfViewer.closePdfFile();
      }
    });
  }

  private void installEscToClose() {
    final String actionKey = "PDF_REPORT_DIALOG_CLOSE_ON_ESC";
    JRootPane root = getRootPane();

    root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);

    root.getActionMap().put(actionKey, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        PdfReportDialog.this.dispatchEvent(
            new WindowEvent(PdfReportDialog.this, WindowEvent.WINDOW_CLOSING)
        );
      }
    });
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    setPreferredSize(new Dimension(700, 1000));
    setMinimumSize(new Dimension(700, 1000));

    JToolBar toolBar = new JToolBar();

    JButton saveButton = new JButton("Save");
    saveButton.setToolTipText("Save PDF file to location");
    saveButton.addActionListener(e -> savePdfFile());
    toolBar.add(saveButton);

    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
    toolBar.add(closeButton);

    add(toolBar, BorderLayout.NORTH);
    add(new JScrollPane(pdfViewer), BorderLayout.CENTER);

    pack();
    setLocationRelativeTo(getParent());
  }

  private void savePdfFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save PDF Report");
    fileChooser.setSelectedFile(new File(pdfFile.getName()));
    fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));

    int userSelection = fileChooser.showSaveDialog(this);

    if (userSelection == JFileChooser.APPROVE_OPTION) {
      File fileToSave = fileChooser.getSelectedFile();

      if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
        fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
      }

      try {
        java.nio.file.Files.copy(
            pdfFile.toPath(),
            fileToSave.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );

        JOptionPane.showMessageDialog(
            this,
            "PDF report saved successfully to:\n" + fileToSave.getAbsolutePath(),
            "Save Successful",
            JOptionPane.INFORMATION_MESSAGE
        );
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            "Failed to save PDF file: " + ex.getMessage(),
            "Save Error",
            JOptionPane.ERROR_MESSAGE
        );
      }
    }
  }

  public static void showReportDialog(Component parent, File pdfFile) throws Exception {
    Frame frame = JOptionPane.getFrameForComponent(parent);
    PdfReportDialog dialog = new PdfReportDialog(frame, pdfFile);
    dialog.setVisible(true);
  }
}