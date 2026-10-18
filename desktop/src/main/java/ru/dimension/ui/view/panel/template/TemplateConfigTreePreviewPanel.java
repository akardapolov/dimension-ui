package ru.dimension.ui.view.panel.template;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.jdesktop.swingx.JXTitledSeparator;
import ru.dimension.ui.model.validation.TemplateDraft;

/**
 * Живой предпросмотр того, что будет добавлено в Configuration после Save:
 * Profile -> Task -> Connection, Queries. Обновляется при каждом изменении
 * формы (включая авто-подставленные имена).
 */
public class TemplateConfigTreePreviewPanel extends JPanel {

  private final DefaultMutableTreeNode root;
  private final DefaultTreeModel treeModel;
  private final JTree tree;

  public TemplateConfigTreePreviewPanel() {
    setLayout(new BorderLayout(2, 2));

    this.root = new DefaultMutableTreeNode("Will be added to Configuration");
    this.treeModel = new DefaultTreeModel(root);
    this.tree = new JTree(treeModel);
    this.tree.setRootVisible(true);
    this.tree.setShowsRootHandles(true);

    add(new JXTitledSeparator("Preview"), BorderLayout.NORTH);
    add(new JScrollPane(tree), BorderLayout.CENTER);
  }

  public void update(TemplateDraft draft) {
    root.removeAllChildren();
    if (draft != null) {
      DefaultMutableTreeNode profileNode =
          new DefaultMutableTreeNode("Profile: " + nz(draft.getProfileName()));
      root.add(profileNode);

      DefaultMutableTreeNode taskNode = new DefaultMutableTreeNode(
          "Task: " + nz(draft.getTaskName()) + "  (poll " + draft.getPullTimeoutSec() + " sec)");
      profileNode.add(taskNode);

      String connType = draft.getConnectionType() == null ? "JDBC" : draft.getConnectionType().getName();
      taskNode.add(new DefaultMutableTreeNode(
          "Connection [" + connType + "]: " + nz(draft.getConnName()) + " · " + nz(draft.getConnUrl())));

      DefaultMutableTreeNode queriesNode =
          new DefaultMutableTreeNode("Queries (" + draft.getQueries().size() + ")");
      taskNode.add(queriesNode);
      draft.getQueries().forEach(q -> queriesNode.add(new DefaultMutableTreeNode(nz(q.getName()))));
    }

    treeModel.reload();
    for (int i = 0; i < tree.getRowCount(); i++) {
      tree.expandRow(i);
    }
  }

  private String nz(String s) {
    return (s == null || s.isBlank()) ? "—" : s;
  }
}