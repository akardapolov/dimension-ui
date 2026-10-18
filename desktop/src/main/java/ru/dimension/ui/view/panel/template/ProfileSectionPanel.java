package ru.dimension.ui.view.panel.template;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.jdesktop.swingx.JXTextArea;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.validation.TemplateDraft;
import ru.dimension.ui.model.validation.ValidationIssue;
import ru.dimension.ui.model.validation.ValidationSection;

public class ProfileSectionPanel extends JPanel {

  private final ValidatedField profileName;
  private final JXTextArea profileDesc;

  public ProfileSectionPanel() {
    setLayout(new BorderLayout(4, 4));

    this.profileName = new ValidatedField("Profile name...");
    this.profileName.setFieldKey("profile.name");

    this.profileDesc = new JXTextArea();
    this.profileDesc.setLineWrap(true);
    this.profileDesc.setWrapStyleWord(true);
    this.profileDesc.setRows(4);

    JPanel content = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(content, PGHelper.getPGConfig(0), false);
    gbl.row().cell(new JLabel("Name")).cellXRemainder(profileName).fillX();
    gbl.row().cell(new JLabel("Description")).fillX();
    gbl.row().cellXYRemainder(new JScrollPane(profileDesc)).fillXY();
    gbl.done();

    add(new JXTitledSeparator("Profile (will be created new)"), BorderLayout.NORTH);
    add(content, BorderLayout.CENTER);
  }

  public void setOnChange(Runnable onChange) {
    profileName.setOnChange(onChange);
    profileDesc.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
      @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
      @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    });
  }

  public void loadFrom(TemplateDraft draft) {
    profileName.setText(draft.getProfileName());
    profileDesc.setText(draft.getProfileDescription());
  }

  public void applyTo(TemplateDraft draft) {
    draft.setProfileName(profileName.getText());
    draft.setProfileDescription(profileDesc.getText());
  }

  public void applyIssues(List<ValidationIssue> issues) {
    ValidationIssue nameIssue = issues.stream()
        .filter(i -> i.getSection() == ValidationSection.PROFILE && "profile.name".equals(i.getFieldKey()))
        .findFirst().orElse(null);
    profileName.setIssue(nameIssue);
  }

  public ValidatedField getProfileNameField() {
    return profileName;
  }
}