package ru.dimension.ui.view.detail;

import static ru.dimension.ui.laf.LafColorGroup.TABLE_BACKGROUND;
import static ru.dimension.ui.laf.LafColorGroup.TABLE_FONT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXFindBar;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.action.AbstractActionExt;
import org.jdesktop.swingx.search.AbstractSearchable;
import org.jdesktop.swingx.search.SearchFactory;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.service.mapping.Mapper;
import ru.dimension.db.sql.BatchResultSet;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.detail.raw.searchable.DecoratorFactory;
import ru.dimension.ui.view.detail.raw.searchable.MatchingTextHighlighter;
import ru.dimension.ui.view.detail.raw.searchable.XMatchingTextHighlighter;

@Log4j2
public abstract class RawDataPanelCommon extends JPanel {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  private final TableInfo tableInfo;
  private final CProfile cProfile;

  private final JXTable table;
  private final JXFindBar findBar;
  private final ResultSetRawDataJPanel resultSetRawDataJPanel;
  private final DefaultTableModel tableModel;

  private int rowCount = 0;

  protected JLabel jLabelRowCount;
  protected int fetchSize = 1000;
  protected final boolean useFetchSize;
  protected boolean hasData = false;

  protected BatchResultSet batchResultSet;

  public RawDataPanelCommon(TableInfo tableInfo,
                            CProfile cProfile,
                            boolean useFetchSize) {
    this.tableInfo = tableInfo;
    this.cProfile = cProfile;

    this.setLayout(new BorderLayout());

    this.useFetchSize = useFetchSize;

    this.jLabelRowCount = new JLabel("Rows: " + rowCount);

    this.resultSetRawDataJPanel = new ResultSetRawDataJPanel(jLabelRowCount);

    this.tableModel = new DefaultTableModel(getColumnHeaders(), 0);

    this.table = new JXTable(tableModel);

    this.table.setColumnControlVisible(true);
    this.table.setHorizontalScrollEnabled(true);
    this.table.setShowVerticalLines(true);
    this.table.setShowHorizontalLines(true);
    this.table.setBackground(LaF.getBackgroundColor(TABLE_BACKGROUND, LaF.getLafType()));
    this.table.setForeground(LaF.getBackgroundColor(TABLE_FONT, LaF.getLafType()));
    this.table.packAll();

    this.findBar = SearchFactory.getInstance().createFindBar();

    MatchingTextHighlighter matchingTextMarker = new XMatchingTextHighlighter();
    matchingTextMarker.setPainter(DecoratorFactory.createPlainPainter());
    ((AbstractSearchable) this.table.getSearchable()).setMatchHighlighter(matchingTextMarker);

    JScrollPane tableRawDataPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    tableRawDataPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

    tableRawDataPane.setViewportView(this.table);
    tableRawDataPane.setVerticalScrollBar(tableRawDataPane.getVerticalScrollBar());

    JPanel searchablePanel = new JPanel(new BorderLayout());
    final JXCollapsiblePane collapsible = connectCollapsibleFindBarWithTable();

    searchablePanel.add(collapsible, BorderLayout.NORTH);
    searchablePanel.add(tableRawDataPane);
    searchablePanel.setBorder(BorderFactory.createCompoundBorder(
        new TitledBorder("Selected Items: "), new EmptyBorder(4, 4, 4, 4)));

    this.add(searchablePanel, BorderLayout.CENTER);
  }

  protected abstract void loadResultSet(String tableName,
                                        long begin,
                                        long end);

  protected abstract void loadRawData(String tableName,
                                      long begin,
                                      long end);

  protected void loadToModel(List<List<Object>> rawData) {
    List<CProfile> timeStampIndex = tableInfo.getCProfiles()
        .stream()
        .filter(f -> (f.getCsType().isTimeStamp() || f.getColDbTypeName().contains("TIMESTAMP")))
        .toList();

    if (rawData == null || rawData.isEmpty()) {
      if (useFetchSize) {
        if (hasData) {
          JOptionPane.showMessageDialog(this,
                                        "No raw data found", "Warning", JOptionPane.WARNING_MESSAGE);
        }
      } else {
        log.warn("No raw data found");
      }
    } else {
      rowCount = rowCount + rawData.size();
      resultSetRawDataJPanel.updateJLabelRowCount(rowCount);

      rawData.forEach(row -> {
        Object[] rawObj = row.toArray();

        timeStampIndex.forEach(cProfile -> {
          if (cProfile.getCsType().isTimeStamp()) {
            long tsValue = Mapper.convertRawToLong(rawObj[cProfile.getColId()], cProfile);
            rawObj[cProfile.getColId()] = getDate(tsValue);
          }
        });

        tableModel.addRow(rawObj);
      });
    }

    table.packAll();
  }

  protected String getDate(long l) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault()).format(FORMATTER);
  }

  private String[] getColumnHeaders() {
    String[] columnHeaders = new String[tableInfo.getCProfiles().size()];
    AtomicInteger at = new AtomicInteger(0);

    tableInfo.getCProfiles().forEach(e -> columnHeaders[at.getAndIncrement()] = e.getColName());

    return columnHeaders;
  }

  private JXCollapsiblePane connectCollapsibleFindBarWithTable() {
    if (useFetchSize) {
      this.findBar.add(resultSetRawDataJPanel);
    } else {
      this.findBar.add(jLabelRowCount);
    }

    final JXCollapsiblePane collapsible = new JXCollapsiblePane();
    this.table.putClientProperty(AbstractSearchable.MATCH_HIGHLIGHTER, Boolean.TRUE);
    this.findBar.setSearchable(this.table.getSearchable());
    collapsible.add(this.findBar);
    collapsible.setCollapsed(false);

    Action openFindBar = new AbstractActionExt() {
      public void actionPerformed(ActionEvent e) {
        collapsible.setCollapsed(false);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(findBar);
      }
    };
    Action closeFindBar = new AbstractActionExt() {
      public void actionPerformed(ActionEvent e) {
        collapsible.setCollapsed(true);
        table.requestFocusInWindow();
      }
    };

    this.table.getActionMap().put("find", openFindBar);
    this.findBar.getActionMap().put("close", closeFindBar);

    return collapsible;
  }

  private class CustomRenderer extends DefaultTableCellRenderer {

    private final DefaultTableModel model;

    public CustomRenderer(DefaultTableModel model) {
      this.model = model;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int col) {
      Component c = super.getTableCellRendererComponent(
          table, value, isSelected, hasFocus, row, col);

      if (cProfile != null &&
          model.getColumnName(table.convertColumnIndexToModel(col)).equals(cProfile.getColName())) {
        c.setBackground(Color.lightGray);
      }

      if (isSelected) {
        c.setBackground(table.getSelectionBackground());
      } else {
        c.setBackground(table.getBackground());
      }
      return c;
    }
  }

  class ResultSetRawDataJPanel extends JXPanel {

    protected JLabel jLabelRowCount;
    protected JButton findNext;

    public ResultSetRawDataJPanel(JLabel jLabelRowCount) {
      this.jLabelRowCount = jLabelRowCount;

      this.setLayout(new FlowLayout(10));

      this.findNext = new JButton("Next " + fetchSize + " rows");

      this.findNext.addActionListener(e -> {
        if (e.getSource() == this.findNext) {
          log.info("Fetch next batch of " + fetchSize + " rows..");
          loadToModel(batchResultSet.next() ? batchResultSet.getObject() : Collections.emptyList());
        }
      });

      this.add(this.jLabelRowCount);
      this.add(this.findNext);
    }

    public void updateJLabelRowCount(int rowCount) {
      jLabelRowCount.setText("Rows: " + rowCount);
      jLabelRowCount.paintImmediately(jLabelRowCount.getVisibleRect());
    }
  }
}
