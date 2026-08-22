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

  private JWindow controlWindow;
  private JWindow contentWindow;

  private CustomPopupCloseListener optionalCustomPopupCloseListener;
  private boolean enableHideWhenFocusIsLost = false;

  private Component controlComponent;
  private Component contentsComponent;

  private float contentOpacity = 1.0f;

  public CustomPopup(Component contentsComponent,
                     Window topWindow,
                     CustomPopupCloseListener optionalCustomPopupCloseListener) {
    this(contentsComponent, null, topWindow, optionalCustomPopupCloseListener);
  }

  public CustomPopup(Component contentsComponent,
                     Component controlComponent,
                     Window topWindow,
                     CustomPopupCloseListener optionalCustomPopupCloseListener) {
    super();
    this.topWindow = topWindow;
    this.optionalCustomPopupCloseListener = optionalCustomPopupCloseListener;
    this.contentsComponent = contentsComponent;
    this.controlComponent = controlComponent;

    buildContentWindow(topWindow, contentsComponent);

    if (controlComponent != null) {
      buildControlWindow(topWindow, controlComponent);
    }

    registerListeners();
  }

  private void buildContentWindow(Window topWindow, Component contentsComponent) {
    contentWindow = new JWindow(topWindow);

    JPanel mainPanel = new JPanel(new BorderLayout());
    Border outsideBorder = new LineBorder(new Color(99, 130, 191));
    Border insideBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.white);
    mainPanel.setBorder(BorderFactory.createCompoundBorder(outsideBorder, insideBorder));
    mainPanel.add(contentsComponent, BorderLayout.CENTER);

    contentWindow.getContentPane().add(mainPanel);
    contentWindow.setFocusable(true);
    contentWindow.setAlwaysOnTop(true);
    contentWindow.pack();
    contentWindow.validate();

    String cancelName = "cancel";
    InputMap inputMap = mainPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
    ActionMap actionMap = mainPanel.getActionMap();
    actionMap.put(cancelName, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        hide();
      }
    });

    contentWindow.addWindowListener(new WindowAdapter() {
      @Override
      public void windowOpened(WindowEvent e) {
        enableHideWhenFocusIsLost = true;
      }
    });

    contentWindow.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        if (!isMouseInAnyWindow()) {
          hide();
        }
      }
    });
  }

  private void buildControlWindow(Window topWindow, Component controlComponent) {
    controlWindow = new JWindow(topWindow);

    JPanel controlPanel = new JPanel(new BorderLayout());
    Border outsideBorder = new LineBorder(new Color(99, 130, 191));
    controlPanel.setBorder(outsideBorder);
    controlPanel.add(controlComponent, BorderLayout.CENTER);

    controlWindow.getContentPane().add(controlPanel);
    controlWindow.setFocusable(true);
    controlWindow.setAlwaysOnTop(true);
    controlWindow.pack();
    controlWindow.validate();

    controlWindow.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        if (!isMouseInAnyWindow()) {
          hide();
        }
      }
    });
  }

  private boolean isMouseInAnyWindow() {
    return isMouseInWindow(contentWindow) || isMouseInWindow(controlWindow);
  }

  private boolean isMouseInWindow(JWindow window) {
    if (window == null) return false;
    Point mousePos = window.getMousePosition();
    if (mousePos == null) return false;
    Rectangle bounds = window.getBounds();
    return bounds.contains(
        window.getLocationOnScreen().x + mousePos.x,
        window.getLocationOnScreen().y + mousePos.y
    );
  }

  public void setLocation(int x, int y) {
    if (controlWindow != null) {
      int controlHeight = controlWindow.getHeight();
      controlWindow.setLocation(x, y);
      contentWindow.setLocation(x, y + controlHeight);
    } else {
      contentWindow.setLocation(x, y);
    }
  }

  public Rectangle getBounds() {
    if (controlWindow != null) {
      Rectangle cb = contentWindow.getBounds();
      Rectangle ctrlB = controlWindow.getBounds();
      return new Rectangle(
          ctrlB.x,
          ctrlB.y,
          Math.max(cb.width, ctrlB.width),
          ctrlB.height + cb.height
      );
    }
    return contentWindow.getBounds();
  }

  public void setOpacity(float opacity) {
    this.contentOpacity = Math.max(0.1f, Math.min(1.0f, opacity));
    if (contentWindow != null) {
      contentWindow.setOpacity(this.contentOpacity);
    }
  }

  public float getOpacity() {
    return contentOpacity;
  }

  @Override
  public void show() {
    if (controlWindow != null) {
      controlWindow.setVisible(true);
    }
    contentWindow.setVisible(true);
  }

  @Override
  public void hide() {
    if (contentWindow != null) {
      contentWindow.removeWindowFocusListener(this);
      contentWindow.setVisible(false);
      contentWindow = null;
    }
    if (controlWindow != null) {
      controlWindow.setVisible(false);
      controlWindow = null;
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
    contentWindow.addWindowFocusListener(this);
    topWindow.addComponentListener(this);
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
    if (controlWindow != null && InternalUtilities.isMouseWithinComponent(controlWindow)) {
      return;
    }
    if (InternalUtilities.isMouseWithinComponent(contentWindow)) {
      return;
    }
    hide();
  }

  @Override
  public void componentHidden(ComponentEvent e) { hide(); }

  @Override
  public void componentMoved(ComponentEvent e) { hide(); }

  @Override
  public void componentResized(ComponentEvent e) { hide(); }

  @Override
  public void componentShown(ComponentEvent e) {}

  public void setMinimumSize(Dimension minimumSize) {
    contentWindow.setMinimumSize(minimumSize);
  }

  public Point getLocationOnScreen() {
    if (contentWindow != null) {
      return contentWindow.getLocationOnScreen();
    }
    return null;
  }

  public interface CustomPopupCloseListener {
    void zEventCustomPopupWasClosed(CustomPopup popup);
  }
}