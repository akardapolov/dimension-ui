package ru.dimension.ui.component.panel.popup;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import com.github.lgooddatepicker.zinternaltools.InternalUtilities;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.function.Supplier;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.view.analyze.timeseries.popup.CustomPopup;
import ru.dimension.ui.view.analyze.timeseries.popup.CustomPopup.CustomPopupCloseListener;

public class ConfigPopupPanel extends JPanel implements CustomPopupCloseListener {
  private CustomPopup popup = null;
  private JPanel contentPanel;
  private Instant lastPopupCloseTime = Instant.now();
  private final JButton button;
  private Supplier<JPanel> contentSupplier;

  private final String buttonTextClosed;
  private final String buttonTextOpen;

  public ConfigPopupPanel() {
    this(() -> {
      JPanel panel = new JPanel();
      panel.add(new JLabel("Module is under development"));
      panel.setPreferredSize(new Dimension(200, 200));
      return panel;
    }, " << ", " >> ");
  }

  public ConfigPopupPanel(Supplier<JPanel> contentSupplier) {
    this(contentSupplier, " << ", " >> ");
  }

  public ConfigPopupPanel(Supplier<JPanel> contentSupplier, String buttonTextClosed, String buttonTextOpen) {
    this.contentSupplier = contentSupplier;
    this.buttonTextClosed = buttonTextClosed;
    this.buttonTextOpen = buttonTextOpen;
    this.button = new JButton(buttonTextClosed);
    button.setMargin(new Insets(0, 5, 0, 5));

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    add(button);

    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        togglePopup();
      }
    });
  }

  public void updateContent(Supplier<JPanel> newSupplier) {
    this.contentSupplier = newSupplier;
  }

  private void togglePopup() {
    if ((Instant.now().toEpochMilli() - lastPopupCloseTime.toEpochMilli()) < 200) {
      return;
    }
    openPopup();
  }

  public void openPopup() {
    if (isPopupOpen()) {
      closePopup();
      return;
    }

    if (!isEnabled()) {
      return;
    }

    contentPanel = contentSupplier.get();

    popup = new CustomPopup(contentPanel,
                            SwingUtilities.getWindowAncestor(this),
                            this);

    int defaultX = button.getLocationOnScreen().x;
    int defaultY = button.getLocationOnScreen().y + button.getHeight() + 2;

    zSetPopupLocation(popup, defaultX, defaultY, this, button, 2, 6);
    popup.show();
    contentPanel.requestFocus();
    button.setText(buttonTextOpen);
  }

  private void zSetPopupLocation(CustomPopup popup,
                                 int defaultX,
                                 int defaultY,
                                 JComponent picker,
                                 JComponent verticalFlipReference,
                                 int verticalFlipDistance,
                                 int bottomOverlapAllowed) {
    Window topWindowOrNull = SwingUtilities.getWindowAncestor(picker);
    Rectangle workingArea = InternalUtilities.getScreenWorkingArea(topWindowOrNull);
    int popupWidth = popup.getBounds().width;
    int popupHeight = popup.getBounds().height;
    Rectangle popupRectangle = new Rectangle(defaultX, defaultY, popupWidth, popupHeight);

    if (popupRectangle.getMaxY() > (workingArea.getMaxY() + bottomOverlapAllowed)) {
      popupRectangle.y = verticalFlipReference.getLocationOnScreen().y - popupHeight - verticalFlipDistance;
    }
    if (popupRectangle.getMaxX() > workingArea.getMaxX()) {
      popupRectangle.x -= (popupRectangle.getMaxX() - workingArea.getMaxX());
    }
    if (popupRectangle.getMaxY() > (workingArea.getMaxY() + bottomOverlapAllowed)) {
      popupRectangle.y -= (popupRectangle.getMaxY() - workingArea.getMaxY());
    }
    if (popupRectangle.x < workingArea.x) {
      popupRectangle.x += (workingArea.x - popupRectangle.x);
    }
    if (popupRectangle.y < workingArea.y) {
      popupRectangle.y += (workingArea.y - popupRectangle.y);
    }
    popup.setLocation(popupRectangle.x, popupRectangle.y);
  }

  public boolean isPopupOpen() {
    return (popup != null);
  }

  public void closePopup() {
    if (popup != null) {
      popup.hide();
    }
  }

  @Override
  public void zEventCustomPopupWasClosed(CustomPopup popup) {
    this.popup = null;
    contentPanel = null;
    lastPopupCloseTime = Instant.now();
    button.setText(buttonTextClosed);
  }
}