package ru.dimension.ui.view.panel.template;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.jdesktop.swingx.JXTextField;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.type.ConnectionType;

@Singleton
public class TemplateCatalogPanel extends JPanel {

  private final JXTextField searchField;
  private final JTree tree;
  private final DefaultTreeModel treeModel;
  private final DefaultMutableTreeNode root;

  private List<Task> allTasks = new ArrayList<>();
  private Map<Integer, Connection> connectionById = new LinkedHashMap<>();

  @Inject
  public TemplateCatalogPanel() {
    setLayout(new BorderLayout(4, 4));
    setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    setPreferredSize(new Dimension(260, 100));

    this.searchField = new JXTextField();
    this.searchField.setPrompt("Search templates...");

    this.root = new DefaultMutableTreeNode("Templates");
    this.treeModel = new DefaultTreeModel(root);
    this.tree = new JTree(treeModel);
    this.tree.setRootVisible(false);
    this.tree.setShowsRootHandles(true);
    this.tree.setCellRenderer(new TemplateTreeCellRenderer());

    searchField.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
      @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
      @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
    });

    add(searchField, BorderLayout.NORTH);
    add(new JScrollPane(tree), BorderLayout.CENTER);
  }

  public void setModel(List<Task> tasks, List<Connection> connections) {
    this.allTasks = tasks;
    this.connectionById = new LinkedHashMap<>();
    for (Connection c : connections) {
      connectionById.put(c.getId(), c);
    }
    rebuildTree(searchField.getText());
  }

  public void addTreeSelectionListener(TreeSelectionListener listener) {
    tree.addTreeSelectionListener(listener);
  }

  public Optional<TemplateTreeItem> getSelectedItem() {
    Object comp = tree.getLastSelectedPathComponent();
    if (comp instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof TemplateTreeItem item) {
      return Optional.of(item);
    }
    return Optional.empty();
  }

  public void selectFirstTemplate() {
    for (int i = 0; i < root.getChildCount(); i++) {
      DefaultMutableTreeNode group = (DefaultMutableTreeNode) root.getChildAt(i);
      if (group.getChildCount() > 0) {
        DefaultMutableTreeNode first = (DefaultMutableTreeNode) group.getChildAt(0);
        TreePath path = new TreePath(first.getPath());
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
        return;
      }
    }
  }

  public void requestSearchFocus() {
    searchField.requestFocusInWindow();
  }

  private void applyFilter() {
    rebuildTree(searchField.getText());
  }

  private void rebuildTree(String filterText) {
    String filter = filterText == null ? "" : filterText.trim().toLowerCase();

    root.removeAllChildren();

    Map<ConnectionType, List<Task>> grouped = new LinkedHashMap<>();
    grouped.put(ConnectionType.JDBC, new ArrayList<>());
    grouped.put(ConnectionType.HTTP, new ArrayList<>());
    grouped.put(ConnectionType.JMX, new ArrayList<>());

    for (Task task : allTasks) {
      Connection connection = connectionById.get(task.getConnectionId());
      if (connection == null) {
        continue;
      }
      if (!filter.isEmpty() && !task.getName().toLowerCase().contains(filter)) {
        continue;
      }
      ConnectionType type = connection.getType() == null ? ConnectionType.JDBC : connection.getType();
      grouped.get(type).add(task);
    }

    for (Map.Entry<ConnectionType, List<Task>> entry : grouped.entrySet()) {
      List<Task> tasksInGroup = entry.getValue();
      tasksInGroup.sort(Comparator.comparing(Task::getName, String.CASE_INSENSITIVE_ORDER));

      DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(
          TemplateTreeItem.group(entry.getKey(), tasksInGroup.size()));

      for (Task task : tasksInGroup) {
        Connection connection = connectionById.get(task.getConnectionId());
        groupNode.add(new DefaultMutableTreeNode(TemplateTreeItem.template(task, connection)));
      }
      root.add(groupNode);
    }

    treeModel.reload();
    for (int i = 0; i < tree.getRowCount(); i++) {
      tree.expandRow(i);
    }
  }

  private static class TemplateTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree treeComp, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
      super.getTreeCellRendererComponent(treeComp, value, sel, expanded, leaf, row, hasFocus);
      if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof TemplateTreeItem item) {
        if (item.isGroup()) {
          setText(groupIcon(item.getConnectionType()) + "  " + item);
          setFont(getFont().deriveFont(Font.BOLD));
        } else {
          setText("\u2022  " + item);
          setFont(getFont().deriveFont(Font.PLAIN));
        }
      }
      return this;
    }

    private String groupIcon(ConnectionType type) {
      if (type == null) {
        return "\uD83D\uDDC4";
      }
      return switch (type) {
        case JDBC -> "\uD83D\uDDC4";
        case HTTP -> "\uD83C\uDF10";
        case JMX -> "\uD83E\uDDE9";
      };
    }
  }
}