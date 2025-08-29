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
package ru.dimension.ui.component.module.analyze.timeseries.popup;

import com.github.lgooddatepicker.zinternaltools.InternalUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

public class CustomPopup extends Popup implements WindowFocusListener, ComponentListener {

  private Window topWindow;
  private JWindow displayWindow;
  private CustomPopupCloseListener optionalCustomPopupCloseListener;
  private boolean enableHideWhenFocusIsLost = false;

  public CustomPopup(Component contentsComponent,
                     Window topWindow,
                     CustomPopupCloseListener optionalCustomPopupCloseListener) {
    super();
    this.topWindow = topWindow;
    this.optionalCustomPopupCloseListener = optionalCustomPopupCloseListener;
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    mainPanel.add(contentsComponent, BorderLayout.CENTER);

    Border outsideBorder = new LineBorder(new Color(99, 130, 191));
    Border insideBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.white);
    Border compoundBorder = BorderFactory.createCompoundBorder(outsideBorder, insideBorder);
    mainPanel.setBorder(compoundBorder);
    displayWindow = new JWindow(topWindow);

    // This is part of the bug fix for blank popup windows in linux.
    displayWindow.addWindowListener(new WindowAdapter() {
      @Override
      public void windowOpened(WindowEvent e) {
        enableHideWhenFocusIsLost = true;
      }
    });

    // Add mouse listener to detect when mouse leaves the popup area
    displayWindow.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        // Check if the mouse is still within the popup area
        if (!isMouseInPopupArea()) {
          hide();
        }
      }
    });

    displayWindow.getContentPane().add(mainPanel);
    displayWindow.setFocusable(true);

    displayWindow.setAlwaysOnTop(true);

    displayWindow.pack();
    displayWindow.validate();
    String cancelName = "cancel";
    InputMap inputMap = mainPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
    ActionMap actionMap = mainPanel.getActionMap();
    actionMap.put(cancelName,
                  new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                      hide();
                    }
                  });
    registerListeners();
  }

  private boolean isMouseInPopupArea() {
    if (displayWindow == null) return false;

    Point mousePos = displayWindow.getMousePosition();
    if (mousePos == null) return false;

    return displayWindow.getBounds().contains(
        displayWindow.getLocationOnScreen().x + mousePos.x,
        displayWindow.getLocationOnScreen().y + mousePos.y
    );
  }

  @Override
  public void componentHidden(ComponentEvent e) {
    hide();
  }

  @Override
  public void componentMoved(ComponentEvent e) {
    hide();
  }

  @Override
  public void componentResized(ComponentEvent e) {
    hide();
  }

  @Override
  public void componentShown(ComponentEvent e) {
  }

  public Rectangle getBounds() {
    return displayWindow.getBounds();
  }

  @Override
  public void hide() {
    if (displayWindow != null) {
      displayWindow.setVisible(false);
      displayWindow.removeWindowFocusListener(this);
      displayWindow = null;
    }
    if (topWindow != null) {
      topWindow.removeComponentListener(this);
      topWindow = null;
    }
    if (optionalCustomPopupCloseListener != null) {
      optionalCustomPopupCloseListener.zEventCustomPopupWasClosed(this);
      optionalCustomPopupCloseListener = null;
    }
  }

  private void registerListeners() {
    // Register this class as a focus listener with the display window.
    displayWindow.addWindowFocusListener(this);
    // Register this class as a window movement listener with the top window.
    topWindow.addComponentListener(this);
  }

  public void setLocation(int popupX,
                          int popupY) {
    displayWindow.setLocation(popupX, popupY);
  }

  @Override
  public void show() {
    displayWindow.setVisible(true);
  }

  @Override
  public void windowGainedFocus(WindowEvent e) {
  }

  @Override
  public void windowLostFocus(WindowEvent e) {
    // This section is part of the bug fix for blank popup windows in linux.
    if (!enableHideWhenFocusIsLost) {
      e.getWindow().requestFocus();
      return;
    }
    // This fixes a linux-specific behavior where the focus can be "lost" by clicking a child
    // component (inside the same panel!).
    if (InternalUtilities.isMouseWithinComponent(displayWindow)) {
      return;
    }
    hide();
  }

  public void setMinimumSize(Dimension minimumSize) {
    displayWindow.setMinimumSize(minimumSize);
  }

  public Point getLocationOnScreen() {
    if (displayWindow != null) {
      return displayWindow.getLocationOnScreen();
    }
    return null;
  }

  public static interface CustomPopupCloseListener {

    public void zEventCustomPopupWasClosed(CustomPopup popup);
  }
}