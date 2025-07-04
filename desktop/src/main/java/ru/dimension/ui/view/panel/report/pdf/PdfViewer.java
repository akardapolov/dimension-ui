package ru.dimension.ui.view.panel.report.pdf;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.apache.pdfbox.pdmodel.PDDocument;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.view.panel.report.ImagePanel;

public class PdfViewer extends JPanel {

  private File document;
  private PDDocument doc;

  private PDFRenderer renderer;
  private JPanel panelSelectedPage;
  private JPanel panelControls;

  private int numberOfPages;
  private int currentPageIndex = 0;

  private int width;
  private int height;

  private JTextField txtPageNumber;
  private JButton btnLastPage;
  private JButton btnNextPage;
  private JButton btnPreviousPage;
  private JButton btnFirstPage;

  private void enableDisableButtons(int actionIndex) {
    switch (actionIndex) {
      case 0 -> {
        btnFirstPage.setEnabled(false);
        btnPreviousPage.setEnabled(false);
        btnNextPage.setEnabled(true);
        btnLastPage.setEnabled(true);
      }
      case 1 -> {
        btnFirstPage.setEnabled(true);
        btnPreviousPage.setEnabled(true);
        btnNextPage.setEnabled(false);
        btnLastPage.setEnabled(false);
      }
      default -> {
        btnFirstPage.setEnabled(true);
        btnPreviousPage.setEnabled(true);
        btnNextPage.setEnabled(true);
        btnLastPage.setEnabled(true);
      }
    }
  }

  public PdfViewer(File document) throws Exception {
    this.document = document;
    this.doc = PDDocument.load(document);
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);
    initialize(document);
  }

  public PdfViewer() {
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);
  }

  private void selectPage(int pageIndex) {
    BufferedImage renderImage = null;

    try {
      renderImage = renderer.renderImage(pageIndex, 1);
    } catch (IOException e) {
      e.printStackTrace();
    }
    panelSelectedPage.removeAll();

    ImagePanel imagePanel = new ImagePanel(renderImage, width, height);
    imagePanel.setBorder(new EmptyBorder(0, 0, 0, 0));
    imagePanel.setLayout(new CardLayout(0, 0));
    imagePanel.setPreferredSize(new Dimension(width, height));
    panelSelectedPage.add(panelControls, BorderLayout.NORTH);
    panelSelectedPage.add(imagePanel, BorderLayout.CENTER);
    currentPageIndex = pageIndex;

    String pageText = String.format("%d / %d", pageIndex + 1, numberOfPages);
    txtPageNumber.setText(pageText);

    if (pageIndex == 0) {
      enableDisableButtons(0);
    } else if (pageIndex == (numberOfPages - 1)) {
      enableDisableButtons(1);
    } else {
      enableDisableButtons(-1);
    }

    panelSelectedPage.revalidate();
    panelSelectedPage.repaint();
  }

  private void initialize(File file) {
    float realWidth = doc.getPage(0).getMediaBox().getWidth();
    float realHeight = doc.getPage(0).getMediaBox().getHeight();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    double ratio = 0.8;

    height = (int) (screenSize.getHeight() * ratio);
    width = (int) ((height * realWidth) / realHeight);

    numberOfPages = doc.getNumberOfPages();

    renderer = new PDFRenderer(doc);

    panelControls = new JPanel();
    LaF.setBackgroundConfigPanel(CHART_PANEL, panelControls);
    panelControls.setLayout(new BorderLayout(0, 0));

    Component verticalStrutTop = Box.createVerticalStrut(10);
    panelControls.add(verticalStrutTop, BorderLayout.NORTH);

    Box horizontalBoxControls = Box.createHorizontalBox();
    panelControls.add(horizontalBoxControls);

    Component horizontalStrutLeft = Box.createHorizontalStrut(10);
    horizontalBoxControls.add(horizontalStrutLeft);

    btnFirstPage = new JButton("First Page");
    btnFirstPage.addActionListener(event -> selectPage(0));
    horizontalBoxControls.add(btnFirstPage);

    Component horizontalStrutLeft_1 = Box.createHorizontalStrut(10);
    horizontalBoxControls.add(horizontalStrutLeft_1);

    btnPreviousPage = new JButton("Previous Page");
    btnPreviousPage.addActionListener(e -> {
      if (currentPageIndex > 0) {
        selectPage(currentPageIndex - 1);
      }
    });
    horizontalBoxControls.add(btnPreviousPage);

    Component horizontalStrutLeft_2 = Box.createHorizontalStrut(10);
    horizontalBoxControls.add(horizontalStrutLeft_2);

    txtPageNumber = new JTextField();
    horizontalBoxControls.add(txtPageNumber);
    txtPageNumber.setHorizontalAlignment(SwingConstants.CENTER);
    txtPageNumber.setEditable(false);
    txtPageNumber.setPreferredSize(new Dimension(50, txtPageNumber.getPreferredSize().width / 2));
    txtPageNumber.setMaximumSize(new Dimension(50, txtPageNumber.getPreferredSize().width / 2));
    txtPageNumber.setColumns(10);

    Component horizontalStrutRight_2 = Box.createHorizontalStrut(10);
    horizontalBoxControls.add(horizontalStrutRight_2);

    btnNextPage = new JButton("Next Page");
    btnNextPage.addActionListener(e -> {
      if (currentPageIndex < (numberOfPages - 1)) {
        selectPage(currentPageIndex + 1);
      }
    });
    horizontalBoxControls.add(btnNextPage);

    Component horizontalStrutRight_1 = Box.createHorizontalStrut(10);
    horizontalBoxControls.add(horizontalStrutRight_1);

    btnLastPage = new JButton("Last Page");
    btnLastPage.addActionListener(e -> selectPage(numberOfPages - 1));
    horizontalBoxControls.add(btnLastPage);

    Component horizontalStrutRight = Box.createHorizontalStrut(10);
    horizontalBoxControls.add(horizontalStrutRight);

    Component verticalStrutBottom = Box.createVerticalStrut(10);
    panelControls.add(verticalStrutBottom, BorderLayout.SOUTH);

    Box verticalBoxView = Box.createVerticalBox();
    this.add(verticalBoxView, BorderLayout.WEST);

    Component verticalStrutView = Box.createVerticalStrut(10);
    verticalBoxView.add(verticalStrutView);

    Box horizontalBoxView = Box.createHorizontalBox();
    verticalBoxView.add(horizontalBoxView);

    Component horizontalStrutViewLeft = Box.createHorizontalStrut(10);
    horizontalBoxView.add(horizontalStrutViewLeft);

    panelSelectedPage = new JPanel();
    panelSelectedPage.setBackground(Color.LIGHT_GRAY);
    LaF.setBackgroundConfigPanel(CHART_PANEL, panelSelectedPage);
    horizontalBoxView.add(panelSelectedPage);
    panelSelectedPage.setPreferredSize(new Dimension(width, height));
    panelSelectedPage.setBorder(new EmptyBorder(0, 0, 0, 0));
    panelSelectedPage.setLayout(new BorderLayout(0, 0));

    Component horizontalStrutViewRight = Box.createHorizontalStrut(10);
    horizontalBoxView.add(horizontalStrutViewRight);

    selectPage(0);
  }

  public void closePdfFile() {
    try {
      doc.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
