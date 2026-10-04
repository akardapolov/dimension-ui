package ru.dimension.ui.view.panel.template;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ResourceBundle;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.Border;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdesktop.swingx.JXTextField;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.prompt.Internationalization;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class TemplateJmxConnPanel extends JPanel {

  private final JLabel labelConnectionName;
  private final JLabel labelConnectionURL;
  private final JLabel labelConnectionUserName;
  private final JLabel labelConnectionPassword;

  private final JXTextField connectionName;
  private final JXTextField connectionURL;
  private final JXTextField connectionUserName;
  private final JPasswordField connectionPassword;

  @Inject
  public TemplateJmxConnPanel() {
    ResourceBundle bundleDefault = Internationalization.getInternationalizationBundle();

    this.labelConnectionName = new JLabel("Name");
    this.labelConnectionURL = new JLabel("URL");
    this.labelConnectionUserName = new JLabel("User name");
    this.labelConnectionPassword = new JLabel("Password");

    this.connectionName = new JXTextField();
    this.connectionName.setPrompt(bundleDefault.getString("cName"));
    this.connectionName.setEditable(false);

    this.connectionURL = new JXTextField();
    this.connectionURL.setPrompt(bundleDefault.getString("cJmxURL"));
    this.connectionURL.setEditable(false);

    this.connectionUserName = new JXTextField();
    this.connectionUserName.setPrompt(bundleDefault.getString("cUserName"));
    this.connectionUserName.setEditable(false);

    this.connectionPassword = new JPasswordField();
    this.connectionPassword.setEditable(false);

    Border finalBorder = GUIHelper.getGrayBorder();
    this.connectionName.setBorder(finalBorder);
    this.connectionURL.setBorder(finalBorder);
    this.connectionUserName.setBorder(finalBorder);
    this.connectionPassword.setBorder(finalBorder);

    PainlessGridBag gblCon = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    gblCon.row()
        .cell(labelConnectionName).cellXRemainder(connectionName).fillX();
    gblCon.row()
        .cell(labelConnectionURL).cellXRemainder(connectionURL).fillX();
    gblCon.row()
        .cell(labelConnectionUserName).cell(connectionUserName).fillX()
        .cell(labelConnectionPassword).cell(connectionPassword).fillX();
    gblCon.row()
        .cellXRemainder(new JLabel(bundleDefault.getString("cJmxHint"))).fillX();
    gblCon.row()
        .cellXYRemainder(new JLabel()).fillXY();

    gblCon.done();
  }

  public void setEmpty() {
    this.connectionName.setText("");
    this.connectionURL.setText("");
    this.connectionUserName.setText("");
    this.connectionPassword.setText("");
  }
}
