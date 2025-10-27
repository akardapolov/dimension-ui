package ru.dimension.ui.helper;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.TABLE_FONT_COLOR;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.net.URL;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTextArea;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.db.model.profile.cstype.SType;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.textfield.JTextFieldCase;
import ru.dimension.ui.view.panel.config.ButtonPanel;

@Log4j2
public class GUIHelper {

  private GUIHelper() {}

  public static JXTable getJXTable(int rowCount) {
    JXTable jxTable = new JXTable();
    jxTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    jxTable.setColumnControlVisible(true);
    jxTable.setHorizontalScrollEnabled(true);
    jxTable.setEditable(false);
    jxTable.setVisibleRowCount(rowCount);
    jxTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    jxTable.setShowVerticalLines(true);
    jxTable.setShowHorizontalLines(true);

    return jxTable;
  }

  public static JScrollPane getJScrollPane(JXTable jxTable) {
    JScrollPane jScrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

    jScrollPane.setViewportView(jxTable);
    jScrollPane.setVerticalScrollBar(jScrollPane.getVerticalScrollBar());

    return jScrollPane;
  }

  public static JScrollPane getJScrollPane(JPanel jPanel) {
    JScrollPane jScrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

    jScrollPane.setViewportView(jPanel);
    jScrollPane.setVerticalScrollBar(jScrollPane.getVerticalScrollBar());

    return jScrollPane;
  }

  public static JScrollPane getJScrollPane(Component component, int scrollBarWidth) {
    JScrollPane scrollPane = new JScrollPane(
        component,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    );

    // Set scrollbar width if specified (>0)
    if (scrollBarWidth > 0) {
      JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
      verticalScrollBar.setPreferredSize(new Dimension(scrollBarWidth, verticalScrollBar.getPreferredSize().height));

      JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
      horizontalScrollBar.setPreferredSize(new Dimension(horizontalScrollBar.getPreferredSize().width, scrollBarWidth));
    }

    return scrollPane;
  }

  public static JScrollPane getJScrollPane(JLabel jLabel) {
    JScrollPane jScrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

    jScrollPane.setViewportView(jLabel);
    jScrollPane.setVerticalScrollBar(jScrollPane.getVerticalScrollBar());
    jScrollPane.setBorder(null);
    jScrollPane.setMaximumSize(new Dimension(10, 75));
    jScrollPane.setMinimumSize(new Dimension(10, 50));
    jScrollPane.setSize(new Dimension(10, 50));

    return jScrollPane;
  }



  public static JXTextArea getJXTextArea(int rows,
                                         int columns) {
    JXTextArea jTextArea = new JXTextArea();
    jTextArea.setRows(rows);
    jTextArea.setColumns(columns);
    jTextArea.setFont(new Font("Serif", Font.ITALIC, 16));
    jTextArea.setLineWrap(true);
    jTextArea.setAutoscrolls(true);
    jTextArea.setEditable(true);

    return jTextArea;
  }

  public static JButton getJButton(String text) {
    JButton button = new JButton(text);
    return button;
  }

  public static JSplitPane getJSplitPane(int orientation,
                                         int dividerSize,
                                         int dividerLocation) {
    JSplitPane chartGanttPane = new JSplitPane();
    chartGanttPane.setOrientation(orientation);
    chartGanttPane.setOneTouchExpandable(true);
    chartGanttPane.setDividerSize(dividerSize);
    chartGanttPane.setDividerLocation(dividerLocation);

    return chartGanttPane;
  }

  public static int getIdByColumnName(JXTableCase jxTableCase,
                                      String columnName) {
    return GUIHelper.getIdByColumnName(jxTableCase.getJxTable(),
                                       jxTableCase.getDefaultTableModel(),
                                       jxTableCase.getJxTable().getSelectionModel(),
                                       columnName);
  }

  public static int getIdByColumnName(JXTable jxTable,
                                      DefaultTableModel defaultTableModel,
                                      ListSelectionModel listSelectionModel,
                                      String profileColumnName) {
    int profileId = 0;
    int minIndex = listSelectionModel.getMinSelectionIndex();
    int maxIndex = listSelectionModel.getMaxSelectionIndex();
    for (int i = minIndex; i <= maxIndex; i++) {
      if (listSelectionModel.isSelectedIndex(i)) {
        profileId = (int) jxTable.getModel().getValueAt(i, defaultTableModel.findColumn(profileColumnName));
      }
    }

    return profileId;
  }

  public static String getNameByColumnName(JXTable jxTable,
                                           DefaultTableModel defaultTableModel,
                                           ListSelectionModel listSelectionModel,
                                           String columnName) {
    String name = "";
    int minIndex = listSelectionModel.getMinSelectionIndex();
    int maxIndex = listSelectionModel.getMaxSelectionIndex();
    for (int i = minIndex; i <= maxIndex; i++) {
      if (listSelectionModel.isSelectedIndex(i)) {
        name = (String) jxTable.getModel().getValueAt(i, defaultTableModel.findColumn(columnName));
      }
    }

    return name;
  }

  public static String getNameByColumnName(JXTableCase jxTableCase,
                                           ListSelectionModel listSelectionModel,
                                           String profileColumnName) {
    return GUIHelper.getNameByColumnName(jxTableCase.getJxTable(),
                                         jxTableCase.getDefaultTableModel(),
                                         listSelectionModel,
                                         profileColumnName);
  }

  public static JCheckBox getJCheckBox(String text) {
    return new JCheckBox(text);
  }

  public static JXTableCase getJXTableCase(int rowCountTable,
                                           Object[] columnNamesModel) {
    JXTable jxTable = getJXTable(rowCountTable);
    DefaultTableModel defaultTableModel = new DefaultTableModel(columnNamesModel, 0);
    jxTable.setModel(defaultTableModel);
    jxTable.setSortable(false);

    JScrollPane jScrollPane = getJScrollPane(jxTable);

    return new JXTableCase(jxTable, defaultTableModel, jScrollPane);
  }

  public static JTextFieldCase getJTextFieldCase(String jLabelText) {
    JPanel jPanel = new JPanel();
    JLabel jLabel = new JLabel(jLabelText);
    JTextField jTextField = new JTextField();

    PainlessGridBag gbl = new PainlessGridBag(jPanel, PGHelper.getPGConfig(), false);
    gbl.row()
        .cell(jLabel).cell(jTextField).fillX();
    gbl.done();

    return new JTextFieldCase(jPanel, jLabel, jTextField);
  }

  public static JXTableCase getEditJXTableCase(int rowCountTable,
                                               Object[] columnNamesModel) {
    JXTable jxTable = getJXTable(rowCountTable);
    jxTable.setEditable(true);
    DefaultTableModel defaultTableModel = new DefaultTableModel(columnNamesModel, 0);

    jxTable.setModel(defaultTableModel);
    jxTable.setSortable(false);

    JScrollPane jScrollPane = getJScrollPane(jxTable);

    return new JXTableCase(jxTable, defaultTableModel, jScrollPane);
  }

  public static JXTableCase getJXTableCaseMetadata(Object[] columnNamesModel) {
    DefaultTableModel defaultTableModel = new DefaultTableModel(columnNamesModel, 0) {
      @Override
      public boolean isCellEditable(int row,
                                    int column) {
        return column > 3;
      }
    };

    JComboBox<SType> comboSType = new JComboBox<>(SType.values());
    JComboBox<CType> comboCType = new JComboBox<>(CType.values());

    JXTable jxTable = new JXTable(defaultTableModel) {
      //  Determine editor to be used by row
      public TableCellEditor getCellEditor(int row,
                                           int column) {
        int modelColumn = convertColumnIndexToModel(column);

        if (modelColumn == 4) {
          return new DefaultCellEditor(comboSType);
        } else if (modelColumn == 5) {
          return new DefaultCellEditor(comboCType);
        } else {
          return super.getCellEditor(row, column);
        }
      }
    };
    jxTable.setColumnControlVisible(true);
    jxTable.setSortable(false);
    jxTable.setShowVerticalLines(true);
    jxTable.setShowHorizontalLines(true);

    JScrollPane jScrollPane = getJScrollPane(jxTable);

    return new JXTableCase(jxTable, defaultTableModel, jScrollPane);
  }

  public static JXTableCase getJXTableCaseCheckBox(int rowCountTable,
                                                   Object[] columnNamesModel,
                                                   int BOOLEAN_COL) {

    JXTable jxTable = getJXTable(rowCountTable);
    DefaultTableModel defaultTableModel = new DefaultTableModel(columnNamesModel, 0) {
      @Override
      public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == BOOLEAN_COL) {
          return Boolean.class;
        } else {
          return String.class;
        }
      }
    };
    jxTable.setModel(defaultTableModel);
    jxTable.setSortable(false);
    jxTable.setEditable(true);
    jxTable.setShowVerticalLines(true);
    jxTable.setShowHorizontalLines(true);

    JScrollPane jScrollPane = getJScrollPane(jxTable);

    return new JXTableCase(jxTable, defaultTableModel, jScrollPane);
  }

  public static JXTableCase getJXTableCaseCheckBoxAdHoc(int rowCountTable,
                                                        Object[] columnNamesModel,
                                                        int BOOLEAN_COL) {

    JXTable jxTable = getJXTable(rowCountTable);
    DefaultTableModel defaultTableModel = new DefaultTableModel(columnNamesModel, 0) {
      @Override
      public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == BOOLEAN_COL) {
          return Boolean.class;
        } else {
          return String.class;
        }
      }

      @Override
      public boolean isCellEditable(int row, int column) {
        return column == BOOLEAN_COL;
      }
    };
    jxTable.setModel(defaultTableModel);
    jxTable.setSortable(false);
    jxTable.setEditable(true);
    jxTable.setShowVerticalLines(true);
    jxTable.setShowHorizontalLines(true);

    JScrollPane jScrollPane = getJScrollPane(jxTable);

    return new JXTableCase(jxTable, defaultTableModel, jScrollPane);
  }

  public static JXTableCase getJXTableCaseCheckBoxWithColorRow(int rowCountTable,
                                                               Object[] columnNamesModel,
                                                               int BOOLEAN_COL) {
    DefaultTableModel defaultTableModel = new DefaultTableModel(columnNamesModel, 0) {
      @Override
      public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == BOOLEAN_COL) {
          return Boolean.class;
        } else {
          return String.class;
        }
      }
    };
    JXTable jxTable = new JXTable(defaultTableModel) {
      @Override
      public Component prepareRenderer(TableCellRenderer renderer,
                                       int row,
                                       int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        if (isRowSelected(0)) {
          component.setForeground(LaF.getBackgroundColor(TABLE_FONT_COLOR, LaF.getLafType()));
        } else if (row == 0) {
          component.setForeground(Color.GRAY);
        } else {
          component.setForeground(LaF.getBackgroundColor(TABLE_FONT_COLOR, LaF.getLafType()));
        }
        return component;
      }
    };

    jxTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    jxTable.setColumnControlVisible(true);
    jxTable.setHorizontalScrollEnabled(true);
    jxTable.setVisibleRowCount(rowCountTable);
    jxTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    jxTable.setShowVerticalLines(true);
    jxTable.setShowHorizontalLines(true);

    jxTable.setSortable(false);
    jxTable.setEditable(true);

    JScrollPane jScrollPane = getJScrollPane(jxTable);

    return new JXTableCase(jxTable, defaultTableModel, jScrollPane);
  }

  public static void addToJSplitPane(JSplitPane jSplitPane,
                                     Component component,
                                     Object constraints,
                                     int location) {
    jSplitPane.add(component, constraints);
    jSplitPane.setDividerLocation(location);
    jSplitPane.repaint();
    jSplitPane.revalidate();
  }

  public static void addToJSplitPaneProgressBar(JSplitPane jSplitPane,
                                                String message,
                                                Object constraints,
                                                int location) {
    JPanel component = createProgressBar(message, new Dimension(450, 30));
    jSplitPane.add(component, constraints);
    jSplitPane.setDividerLocation(location);
    jSplitPane.repaint();
    jSplitPane.revalidate();
  }

  public static void addToJSplitPane(JSplitPane jSplitPane,
                                     Component component,
                                     Object constraints) {
    jSplitPane.add(component, constraints);
    jSplitPane.repaint();
    jSplitPane.revalidate();
  }

  public static Border getEtchedBorder() {
    Border inner = new EtchedBorder();
    Border outer = new EmptyBorder(2, 2, 2, 2);

    return BorderFactory.createCompoundBorder(inner, outer);
  }

  public static Border getConfigureBorder(int inset) {
    Border inner = new EtchedBorder();
    Border outer = new EmptyBorder(inset, inset, inset, inset);

    return BorderFactory.createCompoundBorder(inner, outer);
  }

  public static Border getGrayBorder() {
    Border inner = BorderFactory.createEmptyBorder(2, 6, 2, 0);
    Border outer = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY);

    return BorderFactory.createCompoundBorder(outer, inner);
  }

  public static Border getBorderForButton(Color color) {
    Border inner = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    Border outer = BorderFactory.createMatteBorder(1, 1, 1, 1, color);

    return BorderFactory.createCompoundBorder(outer, inner);
  }

  public static class ActiveColumnCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int col) {
      return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
    }
  }

  public static void disableButton(ButtonPanel buttonPanel,
                                   Boolean isSelected) {
    buttonPanel.getBtnNew().setEnabled(isSelected);
    buttonPanel.getBtnDel().setEnabled(isSelected);
    buttonPanel.getBtnCopy().setEnabled(isSelected);
    buttonPanel.getBtnEdit().setEnabled(isSelected);
    buttonPanel.getBtnSave().setEnabled(false);
    buttonPanel.getBtnCancel().setEnabled(false);
  }

  public static JXTableCase getJXTableCaseMetrics(Object[] columnNamesModel) {
    DefaultTableModel defaultTableModel = new DefaultTableModel(columnNamesModel, 0);

    JComboBox<SType> comboSType = new JComboBox<>(SType.values());
    AutoCompleteDecorator.decorate(comboSType);
    JComboBox<CType> comboCType = new JComboBox<>(CType.values());
    AutoCompleteDecorator.decorate(comboCType);
    JXTable jxTable = new JXTable(defaultTableModel) {
      //  Determine editor to be used by row
      public TableCellEditor getCellEditor(int row,
                                           int column) {
        int modelColumn = convertColumnIndexToModel(column);

        if (modelColumn == 4) {
          return new DefaultCellEditor(comboSType);
        } else if (modelColumn == 5) {
          return new DefaultCellEditor(comboCType);
        } else {
          return super.getCellEditor(row, column);
        }
      }
    };
    jxTable.setColumnControlVisible(true);
    jxTable.setSortable(false);

    JScrollPane jScrollPane = getJScrollPane(jxTable);

    return new JXTableCase(jxTable, defaultTableModel, jScrollPane);
  }

  public static void setScrolling(JScrollPane jsp) {
    jsp.setWheelScrollingEnabled(true);
    JScrollBar verticalScrollBar = jsp.getVerticalScrollBar();
    verticalScrollBar.setUnitIncrement(20);
    JScrollBar horizontalScrollBar = jsp.getHorizontalScrollBar();
    horizontalScrollBar.setUnitIncrement(20);
  }

  public static JSeparator verticalSeparator() {
    JSeparator vSeparator = new JSeparator(SwingConstants.VERTICAL);
    vSeparator.setPreferredSize(new Dimension(5, 25));
    return vSeparator;
  }

  public static JRadioButton getSelectedButton(ButtonGroup group) {
    for (Enumeration<AbstractButton> buttons = group.getElements(); buttons.hasMoreElements(); ) {
      AbstractButton button = buttons.nextElement();
      if (button.isSelected()) {
        return (JRadioButton) button;
      }
    }
    return null;
  }

  public static JLabel getLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }

  public static ImageIcon loadIcon(String path) {
    URL url = GUIHelper.class.getResource(path);
    if (url != null) {
      return new ImageIcon(url);
    }
    log.warn("Icon not found: {}", path);
    return null;
  }

  public static String getTabName(MessageBroker.Panel panel) {
    return switch (panel) {
      case REALTIME -> "Real-time";
      case HISTORY -> "History";
      case INSIGHT -> "Insight";
      default -> "";
    };
  }
}
