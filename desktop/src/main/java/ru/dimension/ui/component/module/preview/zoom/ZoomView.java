package ru.dimension.ui.component.module.preview.zoom;

import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.module.chart.PRChartModule;
import ru.dimension.ui.component.module.preview.zoom.internal.ZoomDashboardDialog;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.ProfileTaskQueryKey;

@Log4j2
public class ZoomView extends JPanel {

  private final ZoomModel model;
  private final JLabel openButton;
  private final JRadioButton currentRadio;
  private final JRadioButton hybridRadio;
  @Getter private ZoomDashboardDialog currentDialog;
  private Runnable onOpenAction;

  public ZoomView(ZoomModel model) {
    this.model = model;
    setLayout(new BorderLayout(0, 2));
    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CONFIG_PANEL, this);

    openButton = new JLabel("\uD83D\uDD2D Zoom");
    openButton.setFont(new Font("SansSerif", Font.BOLD, 12));
    openButton.setHorizontalAlignment(SwingConstants.CENTER);
    openButton.setVerticalAlignment(SwingConstants.CENTER);
    openButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    openButton.setToolTipText("Open zoomable dashboard view (experimental)");
    openButton.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(100, 100, 120), 1),
        BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    openButton.addMouseListener(new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent e) { if (onOpenAction != null) onOpenAction.run(); }
      @Override public void mouseEntered(MouseEvent e) { openButton.setForeground(new Color(100, 180, 255)); }
      @Override public void mouseExited(MouseEvent e) { openButton.setForeground(null); }
    });

    currentRadio = new JRadioButton("Current");
    currentRadio.setFont(new Font("SansSerif", Font.PLAIN, 11));
    currentRadio.setForeground(new Color(180, 180, 200));
    currentRadio.setOpaque(false);
    currentRadio.setFocusPainted(false);
    currentRadio.setSelected(true);
    currentRadio.addActionListener(e -> model.setRenderMode(RenderMode.CURRENT));

    hybridRadio = new JRadioButton("Hybrid");
    hybridRadio.setFont(new Font("SansSerif", Font.PLAIN, 11));
    hybridRadio.setForeground(new Color(180, 180, 200));
    hybridRadio.setOpaque(false);
    hybridRadio.setFocusPainted(false);
    hybridRadio.addActionListener(e -> model.setRenderMode(RenderMode.HYBRID));

    ButtonGroup modeGroup = new ButtonGroup();
    modeGroup.add(currentRadio);
    modeGroup.add(hybridRadio);

    JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
    modePanel.setOpaque(false);
    modePanel.add(currentRadio);
    modePanel.add(hybridRadio);

    add(openButton, BorderLayout.CENTER);
    add(modePanel, BorderLayout.SOUTH);
  }

  public void setOnOpenAction(Runnable action) { this.onOpenAction = action; }

  public void updateDialogTitle(int tileCount) {
    SwingUtilities.invokeLater(() -> {
      if (currentDialog != null && currentDialog.isVisible()) {
        String ml = model.getRenderMode() == RenderMode.HYBRID ? " [HYBRID]" : "";
        currentDialog.setTitle("Zoom Dashboard \u2014 " + tileCount + " charts" + ml);
      }
    });
  }

  public boolean isZoomDashboardOpen() { return currentDialog != null && currentDialog.isVisible(); }

  public void markDirty(ProfileTaskQueryKey key) {
    if (currentDialog != null && currentDialog.isVisible()) currentDialog.markDirty(key);
  }

  public void openDashboard(
      ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, PRChartModule>> chartPanes) {
    if (currentDialog != null && currentDialog.isVisible()) {
      currentDialog.toFront(); currentDialog.requestFocus(); return;
    }
    currentDialog = new ZoomDashboardDialog(
        SwingUtilities.getWindowAncestor(this), chartPanes,
        model.getRenderMode(), model.getProfileManager());
    currentDialog.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override public void windowClosed(java.awt.event.WindowEvent e) { currentDialog = null; }
    });
    currentDialog.setVisible(true);
    updateDialogTitle(currentDialog.getTileCount());
  }

  public void closeDashboard() { if (currentDialog != null) currentDialog.dispose(); }
}