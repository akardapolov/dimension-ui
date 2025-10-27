package ru.dimension.ui.view.panel.config.profile;

import java.util.List;
import java.util.ResourceBundle;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdesktop.swingx.JXTextField;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.panel.config.ButtonPanel;


@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class ProfilePanel extends JPanel {

  private final ButtonPanel profileButtonPanel;

  private final JLabel labelNameProfile;
  private final JLabel labelNameTask;
  private final JLabel labelDescription;
  private final JXTextField jTextFieldDescription;
  private final JXTextField jTextFieldProfile;
  private final MultiSelectTaskPanel multiSelectTaskPanel;

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;

  private final ResourceBundle bundleDefault;


  @Inject
  public ProfilePanel(@Named("profileManager") ProfileManager profileManager,
                      @Named("templateManager") TemplateManager templateManager,
                      @Named("profileButtonPanel") ButtonPanel profileButtonPanel,
                      @Named("multiSelectPanel") MultiSelectTaskPanel multiSelectTaskPanel) {
    this.profileManager = profileManager;
    this.templateManager = templateManager;
    this.profileButtonPanel = profileButtonPanel;
    this.bundleDefault = Internationalization.getInternationalizationBundle();
    this.labelNameProfile = new JLabel("Name");
    this.labelNameTask = new JLabel("Task");
    this.labelDescription = new JLabel("Description");
    this.jTextFieldDescription = new JXTextField();
    this.jTextFieldDescription.setEditable(false);
    this.jTextFieldProfile = new JXTextField();
    this.jTextFieldProfile.setEditable(false);
    this.jTextFieldProfile.setPrompt(bundleDefault.getString("pName"));
    this.jTextFieldDescription.setPrompt(bundleDefault.getString("pDesc"));
    this.multiSelectTaskPanel = multiSelectTaskPanel;

    this.multiSelectTaskPanel.getSelectedTaskCase().getDefaultTableModel().getDataVector().removeAllElements();
    this.multiSelectTaskPanel.getSelectedTaskCase().getDefaultTableModel().fireTableDataChanged();
    this.multiSelectTaskPanel.getTaskListCase().getDefaultTableModel().getDataVector().removeAllElements();
    this.multiSelectTaskPanel.getTaskListCase().getDefaultTableModel().fireTableDataChanged();
    this.multiSelectTaskPanel.getTemplateListTaskCase().getDefaultTableModel().getDataVector().removeAllElements();
    multiSelectTaskPanel.getTemplateListTaskCase().getDefaultTableModel().fireTableDataChanged();

    List<TaskInfo> taskList = profileManager.getTaskInfoList();
    List<Task> taskListTemplate = templateManager.getConfigList(Task.class);
    taskListTemplate.forEach(taskIn -> multiSelectTaskPanel.getTemplateListTaskCase().getDefaultTableModel()
        .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));
    taskList.forEach(taskIn -> multiSelectTaskPanel.getTaskListCase().getDefaultTableModel()
        .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));

    Border finalBorder = GUIHelper.getGrayBorder();
    jTextFieldDescription.setBorder(finalBorder);
    jTextFieldProfile.setBorder(finalBorder);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    gbl.row()
        .cellXRemainder(profileButtonPanel).fillX();

    gbl.row()
        .cell(labelNameProfile).cell(jTextFieldProfile).fillX();
    gbl.row()
        .cell(labelDescription).cell(jTextFieldDescription).fillX();
    gbl.row()
        .cell(labelNameTask).cellXYRemainder(multiSelectTaskPanel).fillXY();

    gbl.done();
  }
}