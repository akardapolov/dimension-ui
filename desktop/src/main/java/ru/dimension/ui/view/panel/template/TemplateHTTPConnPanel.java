package ru.dimension.ui.view.panel.template;

import javax.inject.Inject;
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
import ru.dimension.ui.view.panel.config.connection.MethodRadioButtonPanel;
import ru.dimension.ui.view.panel.config.connection.ParseRadioButtonPanel;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class TemplateHTTPConnPanel extends JPanel {

  private final JLabel labelConnectionName;
  private final JLabel labelConnectionURL;
  private final JLabel labelConnectionMethod;
  private final JLabel labelConnectionParse;

  private final JXTextField connectionName;
  private final JXTextField connectionURL;

  private final MethodRadioButtonPanel methodRadioButtonPanel;
  private final ParseRadioButtonPanel parseRadioButtonPanel;

  @Inject
  public TemplateHTTPConnPanel() {
    this.labelConnectionName = new JLabel("Name");
    this.labelConnectionURL = new JLabel("URL");
    this.labelConnectionMethod = new JLabel("Method");
    this.labelConnectionParse = new JLabel("Parse");

    this.connectionName = new JXTextField();
    this.connectionName.setPrompt("Connection name...");
    this.connectionName.setEditable(false);

    this.connectionURL = new JXTextField();
    this.connectionURL.setPrompt("URL...");
    this.connectionURL.setEditable(false);

    this.methodRadioButtonPanel = new MethodRadioButtonPanel();
    this.methodRadioButtonPanel.setButtonNotView();

    this.parseRadioButtonPanel = new ParseRadioButtonPanel();
    this.parseRadioButtonPanel.setButtonNotView();

    Border finalBorder = GUIHelper.getGrayBorder();
    this.connectionName.setBorder(finalBorder);
    this.connectionURL.setBorder(finalBorder);

    PainlessGridBag gblCon = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    gblCon.row()
        .cell(labelConnectionURL).cellXRemainder(connectionURL).fillX();
    gblCon.row()
        .cell(new JLabel("Method"))
        .cell(methodRadioButtonPanel.getGetMethod())
        .cellXRemainder(new JLabel()).fillX();
    gblCon.row()
        .cell(new JLabel("Parse"))
        .cell(parseRadioButtonPanel.getParsePrometheus())
        .cell(parseRadioButtonPanel.getParseJson())
        .cellXRemainder(new JLabel())
        .fillX();
    gblCon.row()
        .cellXYRemainder(new JLabel()).fillXY();

    gblCon.done();
  }

  public void setEmpty() {
    this.connectionName.setText("");
    this.connectionURL.setText("");
    this.methodRadioButtonPanel.setButtonNotView();
    this.parseRadioButtonPanel.setButtonNotView();
  }
}
