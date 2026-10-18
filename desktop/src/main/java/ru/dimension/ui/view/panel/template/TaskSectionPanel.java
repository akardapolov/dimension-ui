package ru.dimension.ui.view.panel.template;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.jdesktop.swingx.JXTextArea;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.validation.TemplateDraft;
import ru.dimension.ui.model.validation.ValidationIssue;
import ru.dimension.ui.model.validation.ValidationSection;

public class TaskSectionPanel extends JPanel {

  private final ValidatedField taskName;
  private final JXTextArea taskDesc;
  private final JSpinner pullTimeout;

  public TaskSectionPanel() {
    setLayout(new BorderLayout(4, 4));

    this.taskName = new ValidatedField("Task name...");
    this.taskName.setFieldKey("task.name");

    this.taskDesc = new JXTextArea();
    this.taskDesc.setLineWrap(true);
    this.taskDesc.setWrapStyleWord(true);
    this.taskDesc.setRows(4);

    this.pullTimeout = new JSpinner(new SpinnerNumberModel(30, 1, 86400, 1));

    JPanel content = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(content, PGHelper.getPGConfig(0), false);
    gbl.row().cell(new JLabel("Name")).cellXRemainder(taskName).fillX();
    gbl.row().cell(new JLabel("Poll timeout, sec")).cell(pullTimeout).fillX();
    gbl.row().cell(new JLabel("Description")).fillX();
    gbl.row().cellXYRemainder(new JScrollPane(taskDesc)).fillXY();
    gbl.done();

    add(new JXTitledSeparator("Task"), BorderLayout.NORTH);
    add(content, BorderLayout.CENTER);
  }

  public void setOnChange(Runnable onChange) {
    taskName.setOnChange(onChange);
    taskDesc.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
      @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
      @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    });
    pullTimeout.addChangeListener(e -> onChange.run());
  }

  public void loadFrom(TemplateDraft draft) {
    taskName.setText(draft.getTaskName());
    taskDesc.setText(draft.getTaskDescription());
    pullTimeout.setValue(draft.getPullTimeoutSec() <= 0 ? 30 : draft.getPullTimeoutSec());
  }

  public void applyTo(TemplateDraft draft) {
    draft.setTaskName(taskName.getText());
    draft.setTaskDescription(taskDesc.getText());
    draft.setPullTimeoutSec((Integer) pullTimeout.getValue());
  }

  public void applyIssues(List<ValidationIssue> issues) {
    ValidationIssue nameIssue = issues.stream()
        .filter(i -> i.getSection() == ValidationSection.TASK && "task.name".equals(i.getFieldKey()))
        .findFirst().orElse(null);
    taskName.setIssue(nameIssue);
  }

  public ValidatedField getTaskNameField() {
    return taskName;
  }
}