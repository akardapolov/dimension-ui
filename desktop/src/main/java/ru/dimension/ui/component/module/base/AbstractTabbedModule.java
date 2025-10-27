package ru.dimension.ui.component.module.base;

import java.awt.Component;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.api.ModuleModel;
import ru.dimension.ui.helper.PGHelper;

public abstract class AbstractTabbedModule<M extends ModuleModel> extends JXTaskPane {

  protected final TabbedUnitsView view = new TabbedUnitsView();
  protected final M model;

  protected AbstractTabbedModule(M model) {
    this.model = model;
    this.setAnimated(false);

    ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder());
  }

  @AllArgsConstructor
  @Getter
  public static class TabDef {
    private MessageBroker.Panel panel;
    private Component rootComponent;
  }

  protected abstract List<TabDef> buildUnits();

  public Runnable initializeUI() {
    var tabs = buildUnits();
    tabs.forEach(t -> view.addTab(t.getPanel(), t.getRootComponent()));

    return () -> PGHelper.cellXYRemainder(this, view.getTabbedPane(), 1, false);
  }

  public JTabbedPane getTabbedPane() {
    return view.getTabbedPane();
  }

  public void setActiveTab(MessageBroker.Panel panel) {
    getTabbedPane().setSelectedIndex(panel.ordinal());
  }
}