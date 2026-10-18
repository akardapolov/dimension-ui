package ru.dimension.ui.view.panel.template;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTextArea;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.view.table.row.Rows.TemplateMetricRow;
import ru.dimension.ui.view.table.row.Rows.TemplateQueryRow;

/**
 * Главный экран обзора шаблона — без прокрутки, одной колонкой на всю ширину:
 * сверху Task/Connection/Queries в ряд, под ними детализация выбранного
 * запроса (текст + комментарий), а в самом низу — Metrics на всю ширину
 * нижней части экрана.
 */
public class TemplateDetailsPanel extends JPanel {

  private static final Color TASK_COLOR = new Color(0x42, 0x85, 0xF4);
  private static final Color CONN_COLOR = new Color(0x8E, 0x24, 0xAA);
  private static final Color QUERIES_COLOR = new Color(0x00, 0x89, 0x7B);
  private static final Color DETAIL_COLOR = new Color(0xF9, 0xA8, 0x25);
  private static final Color METRICS_COLOR = new Color(0xE5, 0x39, 0x35);

  private final CardLayout rootCardLayout;
  private final JPanel rootPanel;

  private final JLabel titleLabel;
  private final JButton loadButton;

  private final JLabel taskSummaryLabel;
  private final JLabel connSummaryLabel;
  private final TTTable<TemplateQueryRow, JXTable> queryPreviewTable;

  private final JXTextArea queryDescArea;
  private final RSyntaxTextArea queryTextArea;
  private final TTTable<TemplateMetricRow, JXTable> metricsPreviewTable;

  public TemplateDetailsPanel(TTTable<TemplateQueryRow, JXTable> queryPreviewTable,
                              TTTable<TemplateMetricRow, JXTable> metricsPreviewTable,
                              JXTextArea taskDescAreaUnused,
                              JXTextArea queryDescArea,
                              RSyntaxTextArea queryTextArea) {
    super(new BorderLayout());

    this.queryPreviewTable = queryPreviewTable;
    this.metricsPreviewTable = metricsPreviewTable;
    this.queryDescArea = queryDescArea;
    this.queryDescArea.setEditable(false);
    this.queryTextArea = queryTextArea;
    this.queryTextArea.setEditable(false);

    this.titleLabel = new JLabel("Select a template");
    this.titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 15f));
    this.loadButton = new JButton("Load to Configuration ▶");

    this.taskSummaryLabel = new JLabel();
    this.connSummaryLabel = new JLabel();

    JPanel header = new JPanel(new BorderLayout());
    header.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    header.add(titleLabel, BorderLayout.WEST);
    header.add(loadButton, BorderLayout.EAST);

    // Верхний блок: карточки в ряд + детализация запроса (вся ширина).
    JPanel topBlock = new JPanel(new BorderLayout(0, 6));
    topBlock.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
    topBlock.add(buildCardsRow(), BorderLayout.NORTH);
    topBlock.add(buildQueryDetailCard(), BorderLayout.CENTER);

    // Нижний блок: Metrics на всю ширину.
    JPanel bottomBlock = new JPanel(new BorderLayout());
    bottomBlock.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
    bottomBlock.add(buildMetricsCard(), BorderLayout.CENTER);

    JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topBlock, bottomBlock);
    verticalSplit.setResizeWeight(0.62);
    verticalSplit.setBorder(null);
    verticalSplit.setContinuousLayout(true);

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(header, BorderLayout.NORTH);
    contentPanel.add(verticalSplit, BorderLayout.CENTER);

    JPanel emptyState = new JPanel(new BorderLayout());
    JXLabel emptyLabel = new JXLabel("Select a template in the catalog on the left");
    emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
    emptyLabel.setFont(emptyLabel.getFont().deriveFont(14f));
    emptyState.add(emptyLabel, BorderLayout.CENTER);

    this.rootCardLayout = new CardLayout();
    this.rootPanel = new JPanel(rootCardLayout);
    rootPanel.add(emptyState, "EMPTY");
    rootPanel.add(contentPanel, "CONTENT");

    add(rootPanel, BorderLayout.CENTER);
    showEmpty();
  }

  /** Task / Connection / Queries — три карточки в ОДИН РЯД, фиксированной высоты. */
  private JPanel buildCardsRow() {
    JPanel cardsRow = new JPanel(new GridLayout(1, 3, 6, 0));
    cardsRow.add(new SectionCard("Task", taskCardContent(), TASK_COLOR));
    cardsRow.add(new SectionCard("Connection", connectionCardContent(), CONN_COLOR));
    cardsRow.add(new SectionCard("Queries", queriesCardContent(), QUERIES_COLOR));
    cardsRow.setPreferredSize(new Dimension(100, 160));
    return cardsRow;
  }

  private JPanel taskCardContent() {
    JPanel panel = new JPanel(new BorderLayout());
    taskSummaryLabel.setVerticalAlignment(SwingConstants.TOP);
    panel.add(taskSummaryLabel, BorderLayout.CENTER);
    return panel;
  }

  private JPanel connectionCardContent() {
    JPanel panel = new JPanel(new BorderLayout());
    connSummaryLabel.setVerticalAlignment(SwingConstants.TOP);
    panel.add(connSummaryLabel, BorderLayout.CENTER);
    return panel;
  }

  private JPanel queriesCardContent() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(queryPreviewTable.scrollPane(), BorderLayout.CENTER);
    return panel;
  }

  /** Детализация выбранного запроса — текст + комментарий, вся ширина, остаток высоты. */
  private JPanel buildQueryDetailCard() {
    JPanel detailPanel = new JPanel(new BorderLayout(4, 4));
    queryDescArea.setRows(2);
    PainlessGridBag gblDetail = new PainlessGridBag(detailPanel, PGHelper.getPGConfig(0), false);
    gblDetail.row().cellXRemainder(new JScrollPane(queryDescArea)).fillXY();
    gblDetail.row().cellXYRemainder(new JScrollPane(queryTextArea)).fillXY();
    gblDetail.done();

    return new SectionCard("Selected query: text & comment", detailPanel, DETAIL_COLOR);
  }

  /** Metrics — на всю ширину нижней части экрана. */
  private JPanel buildMetricsCard() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(metricsPreviewTable.scrollPane(), BorderLayout.CENTER);
    return new SectionCard("Metrics", panel, METRICS_COLOR);
  }

  public void showEmpty() {
    rootCardLayout.show(rootPanel, "EMPTY");
  }

  public void showTemplate(Task task, Connection connection) {
    rootCardLayout.show(rootPanel, "CONTENT");
    titleLabel.setText(task.getName());
    taskSummaryLabel.setText("<html><b>" + task.getName() + "</b><br>poll " + task.getPullTimeout()
                                 + " sec<br>" + nz(task.getDescription()) + "</html>");
    connSummaryLabel.setText("<html><b>" + connection.getName() + "</b><br>["
                                 + (connection.getType() == null ? "JDBC" : connection.getType().getName())
                                 + "]<br>" + nz(connection.getUrl()) + "</html>");
  }

  private String nz(String s) {
    return s == null ? "" : s;
  }

  public void showQueryPreview(String description, String sqlText) {
    queryDescArea.setText(description);
    queryTextArea.setText(sqlText);
  }

  public JButton getLoadButton() {
    return loadButton;
  }

  public TTTable<TemplateQueryRow, JXTable> getQueryPreviewTable() {
    return queryPreviewTable;
  }

  public TTTable<TemplateMetricRow, JXTable> getMetricsPreviewTable() {
    return metricsPreviewTable;
  }
}