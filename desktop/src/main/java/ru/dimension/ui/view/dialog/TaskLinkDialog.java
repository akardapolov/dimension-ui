package ru.dimension.ui.view.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.TaskLinkRow;

public class TaskLinkDialog extends JDialog {

  private final TTTable<TaskLinkRow, JXTable> ttTable;
  private Integer selectedTaskId = null;
  private boolean linked = false;

  public TaskLinkDialog(List<TaskInfo> taskInfoList, Map<Integer, String> connectionNameMap) {
    super(getWindows()[0], "Link Query to Task", ModalityType.APPLICATION_MODAL);

    setLayout(new BorderLayout());
    setSize(500, 300);
    setLocationRelativeTo(getParent());

    List<TaskLinkRow> rows = taskInfoList.stream()
        .map(task -> new TaskLinkRow(
            task.getId(),
            task.getName(),
            connectionNameMap.getOrDefault(task.getConnectionId(), "")
        ))
        .collect(Collectors.toList());

    ttTable = ru.dimension.tt.swingx.JXTableTables.create(
        GUIHelper.getRegistry(),
        TaskLinkRow.class,
        TableUi.<TaskLinkRow>builder()
            .rowIcon(ModelIconProviders.forTaskLinkRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = ttTable.table();
    GUIHelper.configureStandardTableProperties(table);

    ttTable.setItems(rows);

    add(ttTable.scrollPane(), BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    JButton linkButton = new JButton("Link");
    linkButton.addActionListener(e -> {
      int selectedRow = table.getSelectedRow();
      if (selectedRow != -1) {
        int modelRow = table.convertRowIndexToModel(selectedRow);
        List<TaskLinkRow> items = ttTable.model().items();
        if (modelRow >= 0 && modelRow < items.size()) {
          selectedTaskId = items.get(modelRow).getId();
          linked = true;
          dispose();
        }
      }
    });

    JButton skipButton = new JButton("Skip");
    skipButton.addActionListener(e -> {
      linked = false;
      selectedTaskId = null;
      dispose();
    });

    buttonPanel.add(linkButton);
    buttonPanel.add(skipButton);
    add(buttonPanel, BorderLayout.SOUTH);
  }

  public static Integer show(List<TaskInfo> taskInfoList, List<ConnectionInfo> connectionInfoList) {
    Map<Integer, String> connectionNameMap = new java.util.HashMap<>();
    for (ConnectionInfo ci : connectionInfoList) {
      connectionNameMap.put(ci.getId(), ci.getName());
    }
    TaskLinkDialog dialog = new TaskLinkDialog(taskInfoList, connectionNameMap);
    dialog.setVisible(true);
    return dialog.linked ? dialog.selectedTaskId : null;
  }
}