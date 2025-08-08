package ru.dimension.ui.table;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;
import javax.swing.JCheckBox;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.ui.ApplicationFrame;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.component.module.analyze.handler.TableSelectionHandler;

@Log4j2
public class TableSelectionHandlerTest extends ApplicationFrame {

  public TableSelectionHandlerTest(final String title) {
    super(title);

    // Sample Data
    Object[][] data = {{"Table1"}, {"Table2"}, {"Table3"}};
    String[] columnNames = {"Table Name"};
    JXTableCase jxTableCase = GUIHelper.getJXTableCase(10, columnNames);

    for (Object[] d : data) {
      jxTableCase.getDefaultTableModel().addRow(d);
    }

    // Action runner to capture the selected table name
    StringBuilder capturedName = new StringBuilder();
    Consumer<String> actionRunner = capturedName::append;

    //
    JCheckBox checkbox = new JCheckBox("Set blocking action on JTable");
    checkbox.addItemListener(event -> {
      log.info((event.getStateChange()== ItemEvent.SELECTED ? "checked" : "unchecked"));
      if (event.getStateChange()== ItemEvent.SELECTED) {
        jxTableCase.setBlockRunAction(true);
      } else {
        jxTableCase.setBlockRunAction(false);
      }
    });

    // Create and configure the handler
    TableSelectionHandler handler = new TableSelectionHandler(jxTableCase, "Table Name", actionRunner);

    this.add(checkbox, BorderLayout.NORTH);
    this.add(jxTableCase.getJScrollPane(), BorderLayout.CENTER);
  }

  public static void main(String[] args) {
    EventQueue.invokeLater(() -> {
      TableSelectionHandlerTest demo = new TableSelectionHandlerTest("Test");
      demo.pack();
      positionFrameOnScreen(demo, 0.5, 0.5);
      demo.setVisible(true);
    });
  }

  public static void positionFrameOnScreen(final Window frame,
                                           final double horizontalPercent,
                                           final double verticalPercent) {
    final Rectangle s = frame.getGraphicsConfiguration().getBounds();
    final Dimension f = frame.getSize();
    final int w = Math.max(s.width - f.width, 0);
    final int h = Math.max(s.height - f.height, 0);
    final int x = (int) (horizontalPercent * w) + s.x;
    final int y = (int) (verticalPercent * h) + s.y;
    frame.setBounds(x, y, f.width, f.height);
  }
}
