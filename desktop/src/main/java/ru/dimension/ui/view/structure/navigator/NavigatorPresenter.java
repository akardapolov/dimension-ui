package ru.dimension.ui.view.structure.navigator;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.router.listener.ProfileAddListener;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.router.Router;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.state.NavigatorState;
import ru.dimension.ui.view.structure.NavigatorView;

@Log4j2
@Singleton
public class NavigatorPresenter extends KeyAdapter implements ListSelectionListener, ProfileAddListener {

  private final NavigatorView navigatorView;
  private final JXTableCase navigatorProfileCase;
  private final NavigatorState navigatorState;
  private final ProfileManager profileManager;
  private final EventListener eventListener;
  private final Router router;

  private final List<Integer> keyEventList = List.of(KeyEvent.VK_UP, KeyEvent.VK_DOWN);

  @Inject
  public NavigatorPresenter(@Named("navigatorView") NavigatorView navigatorView,
                            @Named("navigatorProfileCase") JXTableCase navigatorProfileCase,
                            @Named("navigatorState") NavigatorState navigatorState,
                            @Named("profileManager") ProfileManager profileManager,
                            @Named("eventListener") EventListener eventListener,
                            @Named("router") Router router) {
    this.navigatorView = navigatorView;
    this.navigatorProfileCase = navigatorProfileCase;
    this.navigatorState = navigatorState;
    this.profileManager = profileManager;
    this.eventListener = eventListener;
    this.router = router;

    this.eventListener.addProfileAddListener(this);
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel lsm = (ListSelectionModel) e.getSource();

    if (lsm.getValueIsAdjusting()) {
      int minSelectionIndex = lsm.getMinSelectionIndex() == -1 ?
          navigatorState.getSelectionIndex() : lsm.getMinSelectionIndex();
      navigatorState.setSelectionIndex(minSelectionIndex);

      Object profileId = getProfileIdOnMouseClickEvent();

      navigatorState.setSelectedProfile((Integer) profileId);

      router.fireOnSelectProfileOnNavigator((Integer) profileId);

      log.info("Current selections index on mouse click (valueChanged): " + minSelectionIndex);
      log.info("Current Profile ID on mouse click (valueChanged): " + profileId);
    }
  }

  @Override
  public void keyPressed(KeyEvent evt) {
    if (keyEventList.contains(evt.getKeyCode())) {
      log.info("Profile info: " + profileManager.getProfileInfoList().stream()
          .findAny()
          .orElseThrow(() -> new NotFoundException("Not found profiles")));

      Object profileId = getProfileIdOnUpDownActionEvent(evt);

      navigatorState.setSelectedProfile((Integer) profileId);

      router.fireOnSelectProfileOnNavigator((Integer) profileId);

      log.info("Current selections index on mouse click (keyPressed): " + navigatorState.getSelectionIndex());
      log.info("Profile ID on up/down events (keyPressed): " + profileId);
    } else {
      if (evt.getKeyCode() == KeyEvent.VK_F4) {
        Object profileId = getProfileIdOnUpDownActionEvent(evt);
        router.runConfigDialog((Integer) profileId);
      }
    }
  }

  public void fillModel() {
    navigatorProfileCase.getDefaultTableModel().getDataVector().removeAllElements();
    navigatorProfileCase.getDefaultTableModel().fireTableDataChanged();

    profileManager.getProfileInfoList().forEach(e ->
                                                    navigatorProfileCase.getDefaultTableModel()
                                                        .addRow(new Object[]{e.getId(), e.getName()}));
  }

  public Object getProfileIdOnUpDownActionEvent(KeyEvent action) {
    int index = navigatorProfileCase.getJxTable().getSelectedRow();
    navigatorProfileCase.getJxTable().setRowSelectionInterval(index, index);

    return navigatorProfileCase.getDefaultTableModel().getDataVector().elementAt(getNextRowIndex(action)).get(0);
  }

  public Object getProfileIdOnMouseClickEvent() {
    return getElementAtSelectedRow();
  }

  private Object getElementAtSelectedRow() {
    if (navigatorProfileCase.getJxTable().getSelectedRow() == -1) {
      throw new NotFoundException("Please, choose profile");
    }

    return navigatorProfileCase.getDefaultTableModel().getDataVector()
        .elementAt(navigatorProfileCase.getJxTable().getSelectedRow()).get(0);
  }

  private int getNextRowIndex(KeyEvent action) {
    if (action.getKeyCode() == KeyEvent.VK_UP) {
      if (navigatorProfileCase.getJxTable().getSelectedRow() > 0) {
        return navigatorProfileCase.getJxTable().getSelectedRow() - 1;
      } else {
        return 0;
      }
    }

    if (action.getKeyCode() == KeyEvent.VK_DOWN) {
      if (navigatorProfileCase.getJxTable().getSelectedRow() <
          (navigatorProfileCase.getJxTable().getModel().getRowCount() - 1)) {
        return navigatorProfileCase.getJxTable().getSelectedRow() + 1;
      } else {
        return navigatorProfileCase.getJxTable().getModel().getRowCount() - 1;
      }
    }

    return 0;
  }

  @Override
  public void fireProfileAdd() {
    this.fillModel();
  }
}
