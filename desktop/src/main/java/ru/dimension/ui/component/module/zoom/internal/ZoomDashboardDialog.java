package ru.dimension.ui.component.module.zoom.internal;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.ConcurrentMap;
import java.util.prefs.Preferences;
import javax.swing.*;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.module.chart.PRChartModule;
import ru.dimension.ui.component.module.zoom.RenderMode;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;

@Log4j2
public class ZoomDashboardDialog extends JDialog {

  private static final String PREF_DIALOG_X = "zoom.dialog.x";
  private static final String PREF_DIALOG_Y = "zoom.dialog.y";
  private static final String PREF_DIALOG_W = "zoom.dialog.width";
  private static final String PREF_DIALOG_H = "zoom.dialog.height";
  private static final Preferences prefs = Preferences.userNodeForPackage(ZoomDashboardDialog.class);

  private final ZoomCanvasPanel canvasPanel;

  public ZoomDashboardDialog(
      Window owner,
      ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, PRChartModule>> chartPanes,
      RenderMode renderMode,
      ProfileManager profileManager) {

    super(owner, "Zoom Dashboard", ModalityType.MODELESS);
    canvasPanel = new ZoomCanvasPanel(chartPanes, this::closeDialog, renderMode, profileManager);
    setContentPane(canvasPanel);
    restoreDialogBounds(owner);
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override public void windowOpened(WindowEvent e) { canvasPanel.requestFocusInWindow(); }
      @Override public void windowClosing(WindowEvent e) { saveDialogBounds(); canvasPanel.stopEngine(); }
    });
  }

  private void restoreDialogBounds(Window owner) {
    Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
    int w = Math.max(400, Math.min(prefs.getInt(PREF_DIALOG_W, ss.width-200), ss.width));
    int h = Math.max(300, Math.min(prefs.getInt(PREF_DIALOG_H, ss.height-100), ss.height));
    int sx = prefs.getInt(PREF_DIALOG_X, Integer.MIN_VALUE);
    int sy = prefs.getInt(PREF_DIALOG_Y, Integer.MIN_VALUE);
    setSize(w, h);
    if (sx != Integer.MIN_VALUE && sy != Integer.MIN_VALUE) {
      boolean onScreen = false;
      for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
        if (gd.getDefaultConfiguration().getBounds().intersects(new Rectangle(sx, sy, w, h))) { onScreen = true; break; }
      if (onScreen) setLocation(sx, sy); else setLocationRelativeTo(owner);
    } else setLocationRelativeTo(owner);
  }

  private void saveDialogBounds() {
    prefs.putInt(PREF_DIALOG_X, getX()); prefs.putInt(PREF_DIALOG_Y, getY());
    prefs.putInt(PREF_DIALOG_W, getWidth()); prefs.putInt(PREF_DIALOG_H, getHeight());
  }

  public void refreshTiles(ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, PRChartModule>> cp) { canvasPanel.rebuildTiles(cp); }
  public void markDirty(ProfileTaskQueryKey key) { canvasPanel.markDirty(key); }
  public int getTileCount() { return canvasPanel.getTileCount(); }
  private void closeDialog() { saveDialogBounds(); canvasPanel.stopEngine(); dispose(); }
}