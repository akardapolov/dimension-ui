/*
 * The MIT License
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.dimension.ui.view.analyze.timeseries.popup;

import com.github.lgooddatepicker.zinternaltools.InternalUtilities;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.EventObject;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.analyze.timeseries.popup.CustomPopup.CustomPopupCloseListener;

@Getter
@Setter
public class PopupPanel extends JPanel implements CustomPopupCloseListener {

  private JXTableCase table;
  private JTextField textField;
  private JButton button;

  private CustomPopup popup = null;
  private JPanel contentPanel;
  private Instant lastPopupCloseTime = Instant.now();

  public PopupPanel() {
    this.textField = new JTextField(10);
    this.textField.setEditable(false);
    this.textField.setBackground(Color.white);

    this.button = new JButton("Settings");
    this.button.setMargin(new Insets(1, 2, 1, 2));

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.add(textField);
    this.add(button);

    this.table = GUIHelper.getJXTableCase(3, new String[]{"Key", "Value"});
    this.table.getJxTable().setEditable(true);
    this.table.getJxTable().getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JTextField()) {
      @Override
      public boolean isCellEditable(EventObject e) {
        return false;
      }
    });

    contentPanel = new JPanel();
    contentPanel.setSize(textField.getWidth() + button.getWidth(), 100);
    contentPanel.setBorder(GUIHelper.getBorder());
    PainlessGridBag gbl = new PainlessGridBag(contentPanel, PGHelper.getPGConfig(0), false);

    gbl.row().cellXYRemainder(table.getJScrollPane()).fillXY();
    gbl.done();

    button.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            zEventToggleCalendarButtonMousePressed(e);
          }
        });

    this.setVisible(true);
  }

  private void zEventToggleCalendarButtonMousePressed(MouseEvent event) {
    togglePopup();
  }

  public void togglePopup() {
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

    if (!textField.hasFocus()) {
      textField.requestFocusInWindow();
    }

    // Create a new custom popup.
    contentPanel = null;
    contentPanel = new JPanel();
    contentPanel.setSize(textField.getWidth() + button.getWidth(), 100);
    contentPanel.setBorder(GUIHelper.getBorder());

    PainlessGridBag gbl = new PainlessGridBag(contentPanel, PGHelper.getPGConfig(0), false);

    gbl.row().cellXYRemainder(table.getJScrollPane()).fillXY();
    gbl.done();

    popup = new CustomPopup(contentPanel, SwingUtilities.getWindowAncestor(this), this);
    // Calculate the default origin for the popup.
    int defaultX =
        button.getLocationOnScreen().x
            + button.getBounds().width
            - popup.getBounds().width
            - 2;
    int defaultY =
        button.getLocationOnScreen().y + button.getBounds().height + 2;
    // Determine which component to use as the vertical flip reference component.
    JComponent verticalFlipReference = (true) ? textField : button;
    // Set the popup location.
    zSetPopupLocation(popup, defaultX, defaultY, this, verticalFlipReference, 2, 6);
    // Show the popup and focus the calendar.
    popup.show();
    contentPanel.requestFocus();
  }

  public boolean isPopupOpen() {
    return (popup != null);
  }

  public void closePopup() {
    if (popup != null) {
      popup.hide();
    }
  }

  static void zSetPopupLocation(CustomPopup popup,
                                int defaultX,
                                int defaultY,
                                JComponent picker,
                                JComponent verticalFlipReference,
                                int verticalFlipDistance,
                                int bottomOverlapAllowed) {
    // Gather some variables that we will need.
    Window topWindowOrNull = SwingUtilities.getWindowAncestor(picker);
    Rectangle workingArea = InternalUtilities.getScreenWorkingArea(topWindowOrNull);
    int popupWidth = popup.getBounds().width;
    int popupHeight = popup.getBounds().height;
    // Calculate the default rectangle for the popup.
    Rectangle popupRectangle = new Rectangle(defaultX, defaultY, popupWidth, popupHeight);
    // If the popup rectangle is below the bottom of the working area, then move it upwards by
    // the minimum amount which will ensure that it will never cover the picker component.
    if (popupRectangle.getMaxY() > (workingArea.getMaxY() + bottomOverlapAllowed)) {
      popupRectangle.y =
          verticalFlipReference.getLocationOnScreen().y - popupHeight - verticalFlipDistance;
    }
    // Confine the popup to be within the working area.
    if (popupRectangle.getMaxX() > (workingArea.getMaxX())) {
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
    // Set the location of the popup.
    popup.setLocation(popupRectangle.x, popupRectangle.y);
  }

  @Override
  public void zEventCustomPopupWasClosed(CustomPopup popup) {
    this.popup = null;
    contentPanel = null;
    lastPopupCloseTime = Instant.now();
  }
}

