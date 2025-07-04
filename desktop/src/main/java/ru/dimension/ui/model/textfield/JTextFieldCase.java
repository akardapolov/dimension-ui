package ru.dimension.ui.model.textfield;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JTextFieldCase {

  private JPanel jPanel;
  private JLabel jLabel;
  private JTextField jTextField;
}
