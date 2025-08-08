package ru.dimension.ui.component.module.analyze.handler;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.handler.MouseListenerImpl;

@Log4j2
public class TableSelectionHandler extends MouseListenerImpl implements ListSelectionListener, KeyListener {

  private final JXTableCase jxTableCase;
  private final String columnName;
  private final Consumer<String> actionRunner;
  private final JComboBox<String> schemaCatalogCBox;

  private boolean actionRunning = false; // Flag to prevent double execution
  private String columnNameValue = null; // Current selected column name value

  public TableSelectionHandler(JXTableCase jxTableCase,
                               String columnName,
                               Consumer<String> actionRunner) {
    this(jxTableCase, columnName, actionRunner, null);
  }

  public TableSelectionHandler(JXTableCase jxTableCase,
                               String columnName,
                               Consumer<String> actionRunner,
                               JComboBox<String> schemaCatalogCBox) {
    this.jxTableCase = jxTableCase;
    this.columnName = columnName;
    this.actionRunner = actionRunner;
    this.schemaCatalogCBox = schemaCatalogCBox;
    this.jxTableCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    this.jxTableCase.getJxTable().addMouseListener(this);
    this.jxTableCase.getJxTable().addKeyListener(this);
  }

  public void initializeTableModelListener(int nameId, int checkBoxId) {
    DefaultTableModel model = jxTableCase.getDefaultTableModel();
    model.addTableModelListener(e -> {
      if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == checkBoxId) {
        int modelRow = e.getFirstRow();
        try {
          String name = (String) model.getValueAt(modelRow, nameId);
          runAction(name);
        } catch (ClassCastException ex) {
          log.error("Error getting name from table: ", ex);
        }
      }
    });
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() > 1) {
      return;
    }

    log.info("Action mouseClicked");

    if (jxTableCase.isBlockRunAction()) {
      return;
    }

    JXTable source = (JXTable) e.getSource();

    if (source.equals(jxTableCase.getJxTable())) {
      if (!jxTableCase.getJxTable().getSelectionModel().getValueIsAdjusting()) {

        String name = GUIHelper.getNameByColumnName(jxTableCase, jxTableCase.getJxTable().getSelectionModel(), columnName);

        if (columnNameValue == null) {
          columnNameValue = name;
          return;
        }
        if (!columnNameValue.equals(name)) {
          columnNameValue = name;
          return;
        }

        runAction(name);
      }
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    log.info("Action valueChanged");

    if (jxTableCase.isBlockRunAction()) {
      return;
    }

    if (actionRunning) {
      return; // Prevent re-entry
    }

    actionRunning = true; // Set flag
    try {
      ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

      if (!e.getValueIsAdjusting() && e.getSource().equals(jxTableCase.getJxTable().getSelectionModel())) {
        String name = GUIHelper.getNameByColumnName(jxTableCase, listSelectionModel, columnName);

        if (columnNameValue == null) {
          columnNameValue = name;
          return;
        }
        if (!columnNameValue.equals(name)) {
          columnNameValue = name;
          return;
        }

        runAction(name);
      }
    } finally {
      actionRunning = false; // Reset flag
    }
  }

  private void runAction(String name) {
    log.info("Run action with name: " + name);

    if (schemaCatalogCBox != null) {
      String selectedSchemaCatalog = (String) schemaCatalogCBox.getSelectedItem();
      assert selectedSchemaCatalog != null;
      if (!selectedSchemaCatalog.isEmpty()) {
        actionRunner.accept(selectedSchemaCatalog + "." + name);
      }
    } else {
      actionRunner.accept(name);
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {}

  @Override
  public void keyPressed(KeyEvent e) {}

  @Override
  public void keyReleased(KeyEvent e) {
    log.info("Action keyReleased: " + KeyEvent.getKeyText(e.getKeyCode()));
    if (jxTableCase.isBlockRunAction()) {
      return;
    }

    if (e.getKeyCode() == KeyEvent.VK_UP) {
      handleSelectionChange(e);
    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
      handleSelectionChange(e);
    }
  }

  private void handleSelectionChange(KeyEvent e) {
    JXTable source = (JXTable) e.getSource();

    if (source.equals(jxTableCase.getJxTable())) {
      if (!jxTableCase.getJxTable().getSelectionModel().getValueIsAdjusting()) {

        String name = GUIHelper.getNameByColumnName(jxTableCase, jxTableCase.getJxTable().getSelectionModel(), columnName);

        columnNameValue = name;

        runAction(name);
      }
    }
  }
}