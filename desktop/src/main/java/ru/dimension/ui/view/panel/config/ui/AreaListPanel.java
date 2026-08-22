package ru.dimension.ui.view.panel.config.ui;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;

public class AreaListPanel extends JPanel {

  private final JList<String> areaList;
  private final DefaultListModel<String> listModel;

  public AreaListPanel() {
    setLayout(new BorderLayout());

    listModel = new DefaultListModel<>();
    listModel.addElement("Chart");

    areaList = new JList<>(listModel);
    areaList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    areaList.setSelectedIndex(0);

    add(new JScrollPane(areaList), BorderLayout.CENTER);
  }

  public void addSelectionListener(ListSelectionListener listener) {
    areaList.addListSelectionListener(listener);
  }

  public String getSelectedArea() {
    return areaList.getSelectedValue();
  }

  public void setSelectedArea(String area) {
    areaList.setSelectedValue(area, true);
  }

}
