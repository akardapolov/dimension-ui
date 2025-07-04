package ru.dimension.ui.helper;

import java.awt.Component;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public final class DialogHelper {

  private DialogHelper() {}

  public static void showErrorDialog(Component parentComponent,
                                     String errorMessage,
                                     String title,
                                     Throwable exception) {
    String fullErrorMessage = buildErrorMessage(errorMessage, exception);
    showScrollableMessageDialog(parentComponent, fullErrorMessage, title, JOptionPane.ERROR_MESSAGE);
  }

  public static void showErrorDialog(String errorMessage,
                                     String title,
                                     Throwable exception) {
    showErrorDialog(null, errorMessage, title, exception);
  }

  public static int showYesNoOptionDialog(Component parentComponent,
                                          String message,
                                          String title) {
    return JOptionPane.showOptionDialog(parentComponent, message, title,
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null,
                                        new String[]{"Yes", "No"}, "Yes");
  }

  public static void showMessageDialog(Component parentComponent,
                                       String message,
                                       String title) {
    JOptionPane.showMessageDialog(parentComponent, message, title, JOptionPane.INFORMATION_MESSAGE);
  }

  private static String buildErrorMessage(String errorMessage, Throwable exception) {
    StringWriter stringWriter = new StringWriter();
    try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
      printWriter.println(errorMessage);
      printWriter.print("Exception is: ");
      exception.printStackTrace(printWriter);
    }
    return stringWriter.toString();
  }

  private static void showScrollableMessageDialog(Component parentComponent,
                                                  String message,
                                                  String title,
                                                  int messageType) {
    JTextArea textArea = new JTextArea(message, 15, 60);
    textArea.setEditable(false);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);

    JScrollPane scrollPane = new JScrollPane(textArea);
    JOptionPane.showMessageDialog(parentComponent, scrollPane, title, messageType);
  }
}