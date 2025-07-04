package ru.dimension.ui.view.panel.config.connection;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import lombok.Data;

@Data
public class TextFieldWithButtonPanel extends JPanel {

  private JButton jButton;
  private JTextField jTextField;
  private int height;

  public TextFieldWithButtonPanel(JButton jButton,
                                  JTextField jTextField) {

    this.jButton = jButton;
    this.jTextField = jTextField;

    this.height = Math.max(jTextField.getPreferredSize().height, jButton.getPreferredSize().height);

    GroupLayout layout = new GroupLayout(this);
    this.setLayout(layout);

    layout.setAutoCreateGaps(false);
    layout.setAutoCreateContainerGaps(false);

    layout.setHorizontalGroup(layout.createSequentialGroup()
                                  .addComponent(jTextField)
                                  .addComponent(jButton));

    layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField, height, height, height)
                                .addComponent(jButton));
  }

}
