package ru.dimension.ui.view.panel.config.query;

import java.util.ResourceBundle;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTextField;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.panel.config.ButtonPanel;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class MainQueryPanel extends JPanel {

  private final ButtonPanel queryButtonPanel;
  private final JLabel labelQueryName;
  private final JLabel labelQueryDescription;
  private final JLabel labelQueryText;
  private final JXTextField queryName;
  private final JXTextField queryDescription;

  private final JComboBox<?> queryGatherDataComboBox;
  private final JLabel labelGatherDataSql;
  private final RSyntaxTextArea querySqlText;
  private final Border finalBorder;
  private final ResourceBundle bundleDefault;

  @Inject
  public MainQueryPanel(@Named("queryButtonPanel") ButtonPanel queryButtonPanel,
                        @Named("queryGatherDataComboBox") JComboBox<?> queryGatherDataComboBox,
                        @Named("querySqlText") RSyntaxTextArea querySqlText) {
    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.queryButtonPanel = queryButtonPanel;
    this.labelQueryName = new JLabel("Name");
    this.labelQueryDescription = new JLabel("Description");
    this.labelQueryText = new JLabel("Text");
    this.queryName = new JXTextField();
    this.queryDescription = new JXTextField();
    this.queryDescription.setEditable(false);
    this.queryDescription.setPrompt(bundleDefault.getString("qDesc"));
    this.queryName.setEditable(false);
    this.getQueryName().setPrompt(bundleDefault.getString("qName"));

    this.querySqlText = querySqlText;

    this.queryGatherDataComboBox = queryGatherDataComboBox;
    this.queryGatherDataComboBox.setEnabled(false);
    this.labelGatherDataSql = new JLabel("Gather data SQL");

    this.finalBorder = GUIHelper.getGrayBorder();
    this.queryName.setBorder(finalBorder);
    this.queryDescription.setBorder(finalBorder);
    this.queryGatherDataComboBox.setBorder(finalBorder);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row()
        .cellXRemainder(this.queryButtonPanel).fillX();
    gbl.row()
        .cell(labelQueryName).cell(queryName).fillX();
    gbl.row()
        .cell(labelQueryDescription).cell(queryDescription).fillX();
    gbl.row()
        .cell(labelGatherDataSql).cell(queryGatherDataComboBox).fillX();
    gbl.row()
        .cell(labelQueryText).cell(new RTextScrollPane(querySqlText)).fillXY(1, 2);
    gbl.done();
  }
}
