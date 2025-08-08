package ru.dimension.ui.filter;

import static org.mockito.Mockito.when;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.jdesktop.swingx.JXTable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.panel.popup.GanttPopupPanel;

@RunWith(MockitoJUnitRunner.class)
public class GanttPopupPanelTest {

  private GanttPopupPanel ganttPopupPanel;
  @Mock
  private JXTable mockTable;
  private DefaultTableModel tableModel;
  private TableRowSorter<DefaultTableModel> sorter;

  @Before
  public void setUp() {
    ganttPopupPanel = new GanttPopupPanel(Component.DASHBOARD);
    ganttPopupPanel.setCurrentTable(mockTable);

    // Setup table model with sample data
    tableModel = new DefaultTableModel(
        new Object[][]{
            {"Activity1", "Емельяново", false},
            {"Activity2", "Москва", true},
            {"Activity3", "Санкт-Петербург", false}
        },
        new String[]{"Activity", "Name", "Selected"}
    );

    sorter = new TableRowSorter<>(tableModel);

    // Fix the mock setup for getRowSorter()
    when(mockTable.getRowSorter()).thenAnswer(invocation -> sorter);
  }

  @Test
  public void testFilter_ExactCyrillicMatch() {
    ganttPopupPanel.filter("Емел");
    verifyRowVisibility(sorter, 0, true);  // Should match "Емельяново"
    verifyRowVisibility(sorter, 1, false);
    verifyRowVisibility(sorter, 2, false);
  }

  @Test
  public void testFilter_CaseInsensitiveCyrillic() {
    ganttPopupPanel.filter("мос");
    verifyRowVisibility(sorter, 0, false);
    verifyRowVisibility(sorter, 1, true);  // Should match "Москва"
    verifyRowVisibility(sorter, 2, false);
  }

  @Test
  public void testFilter_NoMatch() {
    ganttPopupPanel.filter("xyz");
    verifyRowVisibility(sorter, 0, false);
    verifyRowVisibility(sorter, 1, false);
    verifyRowVisibility(sorter, 2, false);
  }

  @Test
  public void testFilter_EmptyPattern() {
    ganttPopupPanel.filter("");
    verifyRowVisibility(sorter, 0, true);
    verifyRowVisibility(sorter, 1, true);
    verifyRowVisibility(sorter, 2, true);
  }

  @Test
  public void testFilter_NullPattern() {
    ganttPopupPanel.filter(null);
    verifyRowVisibility(sorter, 0, true);
    verifyRowVisibility(sorter, 1, true);
    verifyRowVisibility(sorter, 2, true);
  }

  private void verifyRowVisibility(TableRowSorter<?> sorter, int modelRow, boolean shouldBeVisible) {
    boolean found = false;
    for (int viewRow = 0; viewRow < sorter.getViewRowCount(); viewRow++) {
      if (sorter.convertRowIndexToModel(viewRow) == modelRow) {
        found = true;
        break;
      }
    }
    if (shouldBeVisible) {
      assert found : "Row " + modelRow + " should be visible but isn't";
    } else {
      assert !found : "Row " + modelRow + " should not be visible but is";
    }
  }
}