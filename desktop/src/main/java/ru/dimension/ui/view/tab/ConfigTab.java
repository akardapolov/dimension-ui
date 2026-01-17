package ru.dimension.ui.view.tab;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.tab.ConfigEditTabPane;

@Log4j2
@Singleton
public class ConfigTab extends JTabbedPane implements ChangeListener {

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;

  @Inject
  public ConfigTab(@Named("profileConfigCase") JXTableCase profileCase,
                   @Named("taskConfigCase") JXTableCase taskCase,
                   @Named("connectionConfigCase") JXTableCase connectionCase,
                   @Named("queryConfigCase") JXTableCase queryCase) {
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;

    log.info("ConfigTab created with all table cases");

    this.addChangeListener(this);

    SwingUtilities.invokeLater(this::initTableListeners);
  }

  private void initTableListeners() {
    log.info("Initializing table listeners...");

    addTableListeners(profileCase, ConfigEditTabPane.PROFILE);
    addTableListeners(taskCase, ConfigEditTabPane.TASK);
    addTableListeners(connectionCase, ConfigEditTabPane.CONNECTION);
    addTableListeners(queryCase, ConfigEditTabPane.QUERY);

    log.info("Table listeners initialized successfully");
  }

  private void addTableListeners(JXTableCase tableCase, ConfigEditTabPane tab) {
    if (tableCase == null) {
      log.warn("TableCase is null for tab: {}", tab);
      return;
    }

    JXTable table = tableCase.getJxTable();
    if (table == null) {
      log.warn("JXTable is null for tab: {}", tab);
      return;
    }

    log.debug("Adding listeners for tab: {}", tab);

    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        log.debug("Mouse pressed in table for tab: {}", tab);
        switchToTabIfEnabled(tab);
      }
    });

    table.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN ||
            code == KeyEvent.VK_PAGE_UP || code == KeyEvent.VK_PAGE_DOWN ||
            code == KeyEvent.VK_HOME || code == KeyEvent.VK_END ||
            code == KeyEvent.VK_ENTER) {
          log.debug("Key pressed in table for tab: {}", tab);
          switchToTabIfEnabled(tab);
        }
      }
    });
  }

  private void switchToTabIfEnabled(ConfigEditTabPane tab) {
    int targetIndex = getIndexByTab(tab);
    if (this.isEnabledAt(targetIndex) && this.getSelectedIndex() != targetIndex) {
      log.debug("Switching to tab: {} (index: {})", tab, targetIndex);
      this.setSelectedIndex(targetIndex);
    }
  }

  public void setSelectedTab(ConfigEditTabPane tab) {
    int targetIndex = getIndexByTab(tab);
    if (this.getSelectedIndex() != targetIndex) {
      this.setSelectedIndex(targetIndex);
    }
  }

  public void setSelectedTabWithFocus(ConfigEditTabPane tab) {
    setSelectedTab(tab);
    focusOnTable(tab);
  }

  private int getIndexByTab(ConfigEditTabPane tab) {
    return switch (tab) {
      case PROFILE -> 0;
      case TASK -> 1;
      case CONNECTION -> 2;
      case QUERY -> 3;
    };
  }

  private ConfigEditTabPane getTabByIndex(int index) {
    return switch (index) {
      case 0 -> ConfigEditTabPane.PROFILE;
      case 1 -> ConfigEditTabPane.TASK;
      case 2 -> ConfigEditTabPane.CONNECTION;
      case 3 -> ConfigEditTabPane.QUERY;
      default -> null;
    };
  }

  private JXTableCase getTableCaseByTab(ConfigEditTabPane tab) {
    if (tab == null) return null;
    return switch (tab) {
      case PROFILE -> profileCase;
      case TASK -> taskCase;
      case CONNECTION -> connectionCase;
      case QUERY -> queryCase;
    };
  }

  private void focusOnTable(ConfigEditTabPane tab) {
    JXTableCase tableCase = getTableCaseByTab(tab);
    if (tableCase == null || tableCase.getJxTable() == null) {
      log.debug("Cannot focus - tableCase or table is null for tab: {}", tab);
      return;
    }

    JXTable table = tableCase.getJxTable();

    SwingUtilities.invokeLater(() -> {
      if (table.getSelectedRow() == -1 && table.getRowCount() > 0) {
        log.debug("Selecting first row in table for tab: {}", tab);
        table.setRowSelectionInterval(0, 0);
      }
      table.requestFocusInWindow();
    });
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    ConfigEditTabPane tab = getTabByIndex(this.getSelectedIndex());
    log.debug("Tab changed to: {}", tab);
    if (tab != null) {
      focusOnTable(tab);
    }
  }
}