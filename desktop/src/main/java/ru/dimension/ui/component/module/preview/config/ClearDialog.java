package ru.dimension.ui.component.module.preview.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.helper.ColorHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;

@Log4j2
public class ClearDialog extends JDialog {

  private static final Color PROFILE_COLOR = new Color(0x3B82F6);
  private static final Color TASK_COLOR = new Color(0xEAB308);
  private static final Color QUERY_COLOR = new Color(0xEF4444);

  private static final Color STANDARD_BORDER_COLOR = Color.LIGHT_GRAY;
  private static final Color TEXT_COLOR = Color.BLACK;
  private static final Color TAG_BACKGROUND = new Color(0xDBDBDB);
  private static final Color TAG_HOVER_BACKGROUND = new Color(0x858585);

  private static final int TAG_HEIGHT = 26;
  private static final int CROSS_SIZE = 10;
  private static final int BORDER_RADIUS = 12;
  private static final int HORIZONTAL_GAP = 6;
  private static final int VERTICAL_GAP = 6;
  private static final int NESTING_INDENT = 20;
  private static final int MAX_COLUMNS = 3;

  private final ConcurrentMap<ProfileTaskQueryKey, ? extends ConcurrentMap<CProfile, ?>> chartPanes;
  private final ProfileManager profileManager;
  private final ColorHelper colorHelper;
  private final Consumer<RemoveRequest> removeCallback;

  private JPanel contentPanel;

  public ClearDialog(JFrame owner,
                     ConcurrentMap<ProfileTaskQueryKey, ? extends ConcurrentMap<CProfile, ?>> chartPanes,
                     ProfileManager profileManager,
                     ColorHelper colorHelper,
                     Consumer<RemoveRequest> removeCallback) {
    super(owner, "Clear Charts", true);
    this.chartPanes = chartPanes;
    this.profileManager = profileManager;
    this.colorHelper = colorHelper;
    this.removeCallback = removeCallback;

    initUI();
    buildContent();

    setMinimumSize(new Dimension(500, 400));
    setSize(new Dimension(650, 500));
    setLocationRelativeTo(owner);
  }

  private void initUI() {
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

    JScrollPane scrollPane = new JScrollPane(contentPanel);
    GUIHelper.setScrolling(scrollPane);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());

    mainPanel.add(scrollPane, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(e -> dispose());
    buttonPanel.add(closeButton);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    setContentPane(mainPanel);
  }

  private void buildContent() {
    contentPanel.removeAll();

    Map<Integer, Map<Integer, Map<Integer, List<CProfileEntry>>>> hierarchy = buildHierarchy();

    if (hierarchy.isEmpty()) {
      contentPanel.add(createEmptyLabel());
      contentPanel.revalidate();
      contentPanel.repaint();
      return;
    }

    for (Map.Entry<Integer, Map<Integer, Map<Integer, List<CProfileEntry>>>> profileEntry : hierarchy.entrySet()) {
      int profileId = profileEntry.getKey();
      String profileName = getProfileName(profileId);

      JPanel profilePanel = createNestingPanel(PROFILE_COLOR, "Profile", profileName, () -> {
        removeProfile(profileId);
        buildContent();
      });

      JPanel profileContent = getContentPanel(profilePanel);

      for (Map.Entry<Integer, Map<Integer, List<CProfileEntry>>> taskEntry : profileEntry.getValue().entrySet()) {
        int taskId = taskEntry.getKey();
        String taskName = getTaskName(taskId);

        JPanel taskPanel = createNestingPanel(TASK_COLOR, "Task", taskName, () -> {
          removeTask(profileId, taskId);
          buildContent();
        });

        JPanel taskContent = getContentPanel(taskPanel);

        for (Map.Entry<Integer, List<CProfileEntry>> queryEntry : taskEntry.getValue().entrySet()) {
          int queryId = queryEntry.getKey();
          String queryName = getQueryName(queryId);

          JPanel queryPanel = createNestingPanel(QUERY_COLOR, "Query", queryName, () -> {
            removeQuery(profileId, taskId, queryId);
            buildContent();
          });

          JPanel queryContent = getContentPanel(queryPanel);

          List<CProfileEntry> entries = queryEntry.getValue();
          JPanel cprofileGrid = createColumnMajorGrid(entries);
          queryContent.add(cprofileGrid);

          taskContent.add(queryPanel);
          taskContent.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        }

        profileContent.add(taskPanel);
        profileContent.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
      }

      contentPanel.add(profilePanel);
      contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    contentPanel.revalidate();
    contentPanel.repaint();
  }

  private JPanel createColumnMajorGrid(List<CProfileEntry> entries) {
    JPanel grid = new JPanel(new GridBagLayout());
    grid.setOpaque(false);

    int total = entries.size();
    int cols = Math.min(MAX_COLUMNS, total);
    int rows = (int) Math.ceil((double) total / cols);

    int index = 0;
    for (int col = 0; col < cols; col++) {
      for (int row = 0; row < rows; row++) {
        if (index >= total) {
          break;
        }
        CProfileEntry cpEntry = entries.get(index);
        Color cpColor = colorHelper.getColor(
            cpEntry.key.getColorProfileName(),
            cpEntry.cProfile.getColName()
        );
        JPanel tag = createTagPanel(cpColor, cpEntry.cProfile.getColName(), () -> {
          removeCProfile(cpEntry.key, cpEntry.cProfile);
          buildContent();
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(VERTICAL_GAP / 2, HORIZONTAL_GAP / 2, VERTICAL_GAP / 2, HORIZONTAL_GAP / 2);
        grid.add(tag, gbc);
        index++;
      }
    }

    GridBagConstraints filler = new GridBagConstraints();
    filler.gridx = cols;
    filler.gridy = 0;
    filler.weightx = 1.0;
    filler.fill = GridBagConstraints.HORIZONTAL;
    grid.add(Box.createHorizontalGlue(), filler);

    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setOpaque(false);
    wrapper.add(grid, BorderLayout.NORTH);
    return wrapper;
  }

  private Map<Integer, Map<Integer, Map<Integer, List<CProfileEntry>>>> buildHierarchy() {
    Map<Integer, Map<Integer, Map<Integer, List<CProfileEntry>>>> result = new LinkedHashMap<>();

    for (Map.Entry<ProfileTaskQueryKey, ? extends ConcurrentMap<CProfile, ?>> entry : chartPanes.entrySet()) {
      ProfileTaskQueryKey key = entry.getKey();
      ConcurrentMap<CProfile, ?> cprofiles = entry.getValue();

      if (cprofiles == null || cprofiles.isEmpty()) {
        continue;
      }

      int profileId = key.getProfileId();
      int taskId = key.getTaskId();
      int queryId = key.getQueryId();

      Map<Integer, Map<Integer, List<CProfileEntry>>> tasks =
          result.computeIfAbsent(profileId, k -> new LinkedHashMap<>());
      Map<Integer, List<CProfileEntry>> queries =
          tasks.computeIfAbsent(taskId, k -> new LinkedHashMap<>());
      List<CProfileEntry> cpList =
          queries.computeIfAbsent(queryId, k -> new ArrayList<>());

      for (CProfile cp : cprofiles.keySet()) {
        cpList.add(new CProfileEntry(key, cp));
      }
    }

    return result;
  }

  private String getProfileName(int profileId) {
    try {
      return profileManager.getProfileInfoById(profileId).getName();
    } catch (Exception e) {
      return "Profile #" + profileId;
    }
  }

  private String getTaskName(int taskId) {
    try {
      return profileManager.getTaskInfoById(taskId).getName();
    } catch (Exception e) {
      return "Task #" + taskId;
    }
  }

  private String getQueryName(int queryId) {
    try {
      return profileManager.getQueryInfoById(queryId).getName();
    } catch (Exception e) {
      return "Query #" + queryId;
    }
  }

  private void removeProfile(int profileId) {
    List<ProfileTaskQueryKey> keysToRemove = chartPanes.keySet().stream()
        .filter(k -> k.getProfileId() == profileId)
        .toList();

    for (ProfileTaskQueryKey key : keysToRemove) {
      ConcurrentMap<CProfile, ?> cpMap = chartPanes.get(key);
      if (cpMap != null) {
        for (CProfile cp : new ArrayList<>(cpMap.keySet())) {
          fireRemove(key, cp);
        }
      }
    }
  }

  private void removeTask(int profileId, int taskId) {
    List<ProfileTaskQueryKey> keysToRemove = chartPanes.keySet().stream()
        .filter(k -> k.getProfileId() == profileId && k.getTaskId() == taskId)
        .toList();

    for (ProfileTaskQueryKey key : keysToRemove) {
      ConcurrentMap<CProfile, ?> cpMap = chartPanes.get(key);
      if (cpMap != null) {
        for (CProfile cp : new ArrayList<>(cpMap.keySet())) {
          fireRemove(key, cp);
        }
      }
    }
  }

  private void removeQuery(int profileId, int taskId, int queryId) {
    List<ProfileTaskQueryKey> keysToRemove = chartPanes.keySet().stream()
        .filter(k -> k.getProfileId() == profileId && k.getTaskId() == taskId && k.getQueryId() == queryId)
        .toList();

    for (ProfileTaskQueryKey key : keysToRemove) {
      ConcurrentMap<CProfile, ?> cpMap = chartPanes.get(key);
      if (cpMap != null) {
        for (CProfile cp : new ArrayList<>(cpMap.keySet())) {
          fireRemove(key, cp);
        }
      }
    }
  }

  private void removeCProfile(ProfileTaskQueryKey key, CProfile cProfile) {
    fireRemove(key, cProfile);
  }

  private void fireRemove(ProfileTaskQueryKey key, CProfile cProfile) {
    removeCallback.accept(new RemoveRequest(key, cProfile));
  }

  private Color deriveHoverColor(Color base) {
    float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
    float newBrightness = hsb[2] > 0.5f ? hsb[2] * 0.65f : Math.min(1.0f, hsb[2] * 1.5f);
    float newSaturation = Math.min(1.0f, hsb[1] * 1.2f);
    return Color.getHSBColor(hsb[0], newSaturation, newBrightness);
  }

  private JPanel createNestingPanel(Color buttonColor, String type, String title, Runnable onRemove) {
    JPanel outer = new JPanel(new BorderLayout());
    outer.setOpaque(false);

    TitledBorder titledBorder = BorderFactory.createTitledBorder(
        new LineBorder(STANDARD_BORDER_COLOR, 1, true),
        " " + type + " "
    );
    titledBorder.setTitleFont(outer.getFont().deriveFont(Font.BOLD, 11f));
    titledBorder.setTitleColor(buttonColor);
    titledBorder.setTitlePosition(TitledBorder.TOP);
    titledBorder.setTitleJustification(TitledBorder.LEFT);

    outer.setBorder(new CompoundBorder(
        titledBorder,
        new EmptyBorder(4, 8, 8, 8)
    ));

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setOpaque(false);

    JPanel tag = createTagPanel(buttonColor, title, onRemove);
    headerPanel.add(tag, BorderLayout.WEST);

    JPanel innerContent = new JPanel();
    innerContent.setLayout(new BoxLayout(innerContent, BoxLayout.Y_AXIS));
    innerContent.setOpaque(false);
    innerContent.setBorder(new EmptyBorder(VERTICAL_GAP, NESTING_INDENT, 0, 0));

    outer.add(headerPanel, BorderLayout.NORTH);
    outer.add(innerContent, BorderLayout.CENTER);

    return outer;
  }

  private JPanel getContentPanel(JPanel nestingPanel) {
    return (JPanel) ((BorderLayout) nestingPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
  }

  private JPanel createTagPanel(Color buttonColor, String text, Runnable onRemove) {
    Color hoverColor = deriveHoverColor(buttonColor);

    JPanel tag = new JPanel(new BorderLayout()) {
      private boolean hovered = false;

      {
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
          @Override
          public void mouseEntered(MouseEvent e) {
            hovered = true;
            repaint();
          }

          @Override
          public void mouseExited(MouseEvent e) {
            hovered = false;
            repaint();
          }
        });
      }

      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(hovered ? TAG_HOVER_BACKGROUND : TAG_BACKGROUND);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), BORDER_RADIUS, BORDER_RADIUS);
        g2.setColor(STANDARD_BORDER_COLOR);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, BORDER_RADIUS, BORDER_RADIUS);
        g2.dispose();
      }
    };

    tag.setOpaque(false);
    tag.setBorder(new EmptyBorder(3, 8, 3, 4));

    JLabel label = new JLabel(text);
    label.setForeground(TEXT_COLOR);
    label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));

    JPanel crossPanel = new JPanel() {
      private boolean hovered = false;

      {
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
          @Override
          public void mouseEntered(MouseEvent e) {
            hovered = true;
            repaint();
          }

          @Override
          public void mouseExited(MouseEvent e) {
            hovered = false;
            repaint();
          }

          @Override
          public void mouseClicked(MouseEvent e) {
            onRemove.run();
          }
        });
      }

      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (hovered) {
          g2.setColor(new Color(hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue(), 30));
          g2.fillOval(0, 0, getWidth(), getHeight());
        }

        g2.setColor(hovered ? hoverColor : buttonColor);
        g2.setStroke(new java.awt.BasicStroke(hovered ? 2.0f : 1.5f));
        int pad = 3;
        g2.drawLine(pad, pad, getWidth() - pad - 1, getHeight() - pad - 1);
        g2.drawLine(getWidth() - pad - 1, pad, pad, getHeight() - pad - 1);
        g2.dispose();
      }

      @Override
      public Dimension getPreferredSize() {
        return new Dimension(CROSS_SIZE + 6, CROSS_SIZE + 6);
      }

      @Override
      public Dimension getMinimumSize() {
        return getPreferredSize();
      }

      @Override
      public Dimension getMaximumSize() {
        return getPreferredSize();
      }
    };

    tag.add(label, BorderLayout.CENTER);
    tag.add(crossPanel, BorderLayout.EAST);

    Dimension pref = tag.getPreferredSize();
    Dimension fixedSize = new Dimension(pref.width, TAG_HEIGHT);
    tag.setPreferredSize(fixedSize);
    tag.setMinimumSize(fixedSize);
    tag.setMaximumSize(fixedSize);

    return tag;
  }

  private JLabel createEmptyLabel() {
    JLabel lbl = new JLabel("No charts to display");
    lbl.setHorizontalAlignment(SwingConstants.CENTER);
    lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
    lbl.setBorder(new EmptyBorder(20, 0, 0, 0));
    return lbl;
  }

  public record RemoveRequest(ProfileTaskQueryKey key, CProfile cProfile) {}

  private record CProfileEntry(ProfileTaskQueryKey key, CProfile cProfile) {}
}