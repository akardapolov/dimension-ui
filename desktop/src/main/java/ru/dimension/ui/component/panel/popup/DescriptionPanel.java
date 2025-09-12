package ru.dimension.ui.component.panel.popup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTextArea;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.model.ProfileTaskQueryKey;

@Log4j2
public class DescriptionPanel extends ConfigPopupPanel {
  private final MessageBroker.Component component;
  private final ProfileTaskQueryKey key;
  private final CProfile cProfile;
  private final JXTextArea description;
  private String savedDescription;

  public DescriptionPanel(MessageBroker.Component component,
                          ProfileTaskQueryKey key,
                          CProfile cProfile,
                          JXTextArea description) {
    super(() -> createPopupContent(description), "Description >>", "Description <<");
    this.component = component;
    this.key = key;
    this.cProfile = cProfile;
    this.description = description;
    this.savedDescription = description.getText();

    this.description.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent focusEvent) {
        log.info("Focus lost");
        checkAndSendSaveMessage();
      }

      @Override
      public void focusGained(FocusEvent focusEvent) {
        log.info("Focus gained");
        saveCurrentState();
      }
    });
  }

  private static JPanel createPopupContent(JXTextArea textArea) {
    JPanel panel = new JPanel(new BorderLayout());

    JLabel label = new JLabel("Comment:");
    panel.add(label, BorderLayout.NORTH);

    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setPreferredSize(new Dimension(300, 200));
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  public String getComment() {
    return description.getText();
  }

  public void setComment(String comment) {
    description.setText(comment);
    savedDescription = comment;
  }

  private void saveCurrentState() {
    savedDescription = description.getText();
    log.info("Saved current description state: {}", savedDescription);
  }

  private void checkAndSendSaveMessage() {
    String currentText = description.getText();

    if (!currentText.equals(savedDescription)) {
      log.info("Description changed from '{}' to '{}'", savedDescription, currentText);
      sendNeedSaveDesignMessage();
      savedDescription = currentText;
    } else {
      log.info("Description unchanged, no need to save");
    }
  }

  private void sendNeedSaveDesignMessage() {
    log.info("Need to save design message sending");
    Destination destination = Destination.withDefault(component);

    MessageBroker broker = MessageBroker.getInstance();
    broker.sendMessage(Message.builder()
                           .destination(destination)
                           .action(Action.NEED_TO_SAVE_DESIGN)
                           .parameter("key", key)
                           .parameter("cProfile", cProfile)
                           .parameter("comment", description.getText())
                           .build());
  }
}