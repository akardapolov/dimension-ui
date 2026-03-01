package ru.dimension.ui.component.panel.popup.internal;

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

    displayWindow.addWindowListener(new WindowAdapter() {
      @Override
      public void windowOpened(WindowEvent e) {
        enableHideWhenFocusIsLost = true;
      }
    });

    displayWindow.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
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
    displayWindow.addWindowFocusListener(this);
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

  public void setOpacity(float opacity) {
    if (displayWindow != null) {
      float clamped = Math.max(0.1f, Math.min(1.0f, opacity));
      displayWindow.setOpacity(clamped);
    }
  }

  public float getOpacity() {
    if (displayWindow != null) {
      return displayWindow.getOpacity();
    }
    return 1.0f;
  }

  @Override
  public void windowGainedFocus(WindowEvent e) {
  }

  @Override
  public void windowLostFocus(WindowEvent e) {
    if (!enableHideWhenFocusIsLost) {
      e.getWindow().requestFocus();
      return;
    }
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