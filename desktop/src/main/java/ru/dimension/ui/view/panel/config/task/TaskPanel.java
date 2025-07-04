package ru.dimension.ui.view.panel.config.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.view.custom.DetailedComboBox;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.prompt.Internationalization;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class TaskPanel extends JPanel {

  private final ButtonPanel taskButtonPanel;
  private final JLabel labelNameTask;
  private final JLabel labelNameConnection;
  private final JLabel labelSql;
  private final DetailedComboBox taskConnectionComboBox;
  private final MultiSelectQueryPanel multiSelectQueryPanel;
  private final JLabel labelDescription;
  private final JXTextField jTextFieldDescription;
  private final JXTextField jTextFieldTask;
  private final JLabel pullTimeoutJLabel;
  private final RadioButtonPanel radioButtonPanel;
  private final ProfileManager profileManager;
  private final ResourceBundle bundleDefault;

  @Inject
  public TaskPanel(@Named("taskButtonPanel") ButtonPanel taskButtonPanel,
                   @Named("taskConnectionComboBox") DetailedComboBox taskConnectionComboBox,
                   @Named("multiSelectQueryPanel") MultiSelectQueryPanel multiSelectQueryPanel,
                   @Named("profileManager") ProfileManager profileManager) {
    this.taskButtonPanel = taskButtonPanel;
    this.bundleDefault = Internationalization.getInternationalizationBundle();
    this.labelNameTask = new JLabel("Name");
    this.labelNameConnection = new JLabel("Connection");
    this.labelSql = new JLabel("Query");
    this.labelDescription = new JLabel("Description");
    this.jTextFieldTask = new JXTextField();
    this.jTextFieldTask.setEditable(false);
    this.jTextFieldTask.setPrompt(bundleDefault.getString("tName"));
    this.jTextFieldDescription = new JXTextField();
    this.jTextFieldDescription.setEditable(false);
    this.jTextFieldDescription.setPrompt(bundleDefault.getString("tDesc"));
    this.profileManager = profileManager;
    this.taskConnectionComboBox = taskConnectionComboBox;
    List<ConnectionInfo> connectionAll = profileManager.getConnectionInfoList();
    List<List<?>> connectionDataList;
    connectionDataList = new LinkedList<>();
    connectionAll.forEach(connection -> connectionDataList.add(
        new ArrayList<>(Arrays.asList(connection.getName(), connection.getUserName(),
                                      connection.getUrl(), connection.getJar(), connection.getDriver(), connection.getType()))));
    this.taskConnectionComboBox.setTableData(connectionDataList);
    this.taskConnectionComboBox.setEnabled(false);

    this.multiSelectQueryPanel = multiSelectQueryPanel;
    this.pullTimeoutJLabel = new JLabel("Pull timeout");
    this.radioButtonPanel = new RadioButtonPanel();

    Border finalBorder = GUIHelper.getBorder();
    this.jTextFieldDescription.setBorder(finalBorder);
    this.jTextFieldTask.setBorder(finalBorder);
    this.taskConnectionComboBox.setBorder(finalBorder);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row()
        .cellXRemainder(taskButtonPanel).fillX();
    gbl.row()
        .cell(labelNameTask).cell(jTextFieldTask).fillX();
    gbl.row()
        .cell(labelDescription).cell(jTextFieldDescription).fillX();
    gbl.row()
        .cell(pullTimeoutJLabel).cell(radioButtonPanel).fillX();
    gbl.row()
        .cell(labelNameConnection).cell(taskConnectionComboBox).fillX();
    gbl.row()
        .cell(labelSql).cellXYRemainder(multiSelectQueryPanel).fillXY();

    gbl.done();
  }
}


