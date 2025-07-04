package ru.dimension.ui.view.panel.report.pdf;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PageDrawerParameters;

public class PDFRenderer extends org.apache.pdfbox.rendering.PDFRenderer {

  PDFRenderer(PDDocument document) {
    super(document);
  }

  @Override
  protected org.apache.pdfbox.rendering.PageDrawer createPageDrawer(PageDrawerParameters parameters)
      throws IOException {
    return new PageDrawer(parameters);
  }
}