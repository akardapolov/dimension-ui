package ext.egantt.swing;

import com.egantt.drawing.view.ViewManager;
import com.egantt.model.component.ComponentList;
import com.egantt.model.drawing.ContextResources;
import com.egantt.model.drawing.DrawingContext;
import com.egantt.model.drawing.DrawingPart;
import com.egantt.model.drawing.axis.interval.LongInterval;
import com.egantt.model.drawing.state.BasicDrawingState;
import com.egantt.swing.table.model.column.ColumnManager;
import com.egantt.swing.table.model.column.manager.BasicColumnManager;
import ext.egantt.actions.DrawingTool;
import ext.egantt.component.holder.SplitLayeredHolder;
import ext.egantt.drawing.GanttDynamicComponentUtilities;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JPopupMenu;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jdesktop.swingx.JXTable;

public class GanttTable extends SplitLayeredHolder {

	private static final long serialVersionUID = 198427067966485637L;

	public final static String axises[] = new String[] {"xAxis", "yAxis", "percentageAxis"};

	public final static String X_AXIS = axises[0];
	public final static String Y_AXIS = axises[1];
	public final static String TIME_AXIS = axises[0];
	public final static String HEIGHT_AXIS = axises[1];
	public final static String PERCENTAGE_AXIS = axises[2];

	protected ColumnManager columnManager;
	protected ComponentList componentList;
	protected GanttDynamicComponentUtilities ganttDynamicComponentUtilities;
	protected LocalPopupMouseListener mouseListener;

	protected DrawingTool drawingTool;

	private JPopupMenu popup;

	public GanttTable(Object rowData[][],
										Object columnNames[][],
										ComponentList componentList,
										Map<String, Color> seriesColorMap) {
		this(JTableHelper.createTableModel(rowData, columnNames), componentList, columnNames, seriesColorMap, true);
	}

	public GanttTable(Object rowData[][],
										Object columnNames[][],
										ComponentList componentList,
										Map<String, Color> seriesColorMap,
										Boolean isColumnSortable) {
		this(JTableHelper.createTableModel(rowData, columnNames), componentList, columnNames, seriesColorMap, isColumnSortable);
	}

	public GanttTable(TableModel model,
										ComponentList componentList,
										Object columnNames[][],
										Map<String, Color> seriesColorMap,
										Boolean isColumnSortable) {
		this.componentList = componentList;
		ganttDynamicComponentUtilities = new GanttDynamicComponentUtilities(axises, seriesColorMap);
		columnManager = new BasicColumnManager(model, columnNames);
		this.componentList.setColumnManager(columnManager);
		this.componentList.setComponentManager(ganttDynamicComponentUtilities.getManager());

		setComponentList(componentList);
		setRangeModel(ganttDynamicComponentUtilities.getScrollManager(0));
		setComparatorActivityAndOthers(isColumnSortable);
	}

	private void setComparatorActivityAndOthers(Boolean isColumnSortable) {
		TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(getJXTable().getModel());
		getJXTable().setRowSorter(rowSorter);

		for (int i = 0; i < getJXTable().getModel().getColumnCount(); i++) {
			if (i == 0) {
				rowSorter.setSortable(i, isColumnSortable);
			} else {
				rowSorter.setSortable(i, true);
			}
		}

		rowSorter.setComparator(0, (Comparator<BasicDrawingState>) (o1, o2) -> {
			try {
				Double value1 = extractPercentageFromState(o1);
				Double value2 = extractPercentageFromState(o2);
				return Double.compare(value1, value2);
			} catch (Exception e) {
				return 0;
			}
		});

		for (int colId = 1; colId < getJXTable().getModel().getColumnCount(); colId++) {
			rowSorter.setComparator(colId, (o1, o2) -> {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.toString().compareToIgnoreCase(o2.toString());
			});
		}
	}

	private Double extractPercentageFromState(BasicDrawingState state) {
		for (Iterator<DrawingPart> it = state.parts(); it.hasNext();) {
			DrawingPart part = it.next();
			if (part.getInterval() != null) {
				for (Iterator<?> valueIt = part.values(part.getInterval()); valueIt.hasNext();) {
					Object value = valueIt.next();
					if (value instanceof String) {
						String strValue = (String) value;
						if (strValue.contains("%")) {
							String[] tokens = strValue.split("\\s+");
							for (String token : tokens) {
								if (token.contains("%")) {
									String clean = token.replaceAll("[^\\d.]", "");
									if (!clean.isEmpty()) {
										return Double.parseDouble(clean);
									}
								}
							}
						}
					}
				}
			}
		}
		return 0.0;
	}

	private void setComparatorForColumn(TableRowSorter tableRowSorter, int column, Boolean isColumnSortable){
		tableRowSorter.setComparator(column, (Comparator<String>) (o1, o2) -> {
			if (!isNumeric(o1) || !isNumeric(o2)){
				return o1.toString().compareToIgnoreCase(o2.toString());
			} else {
				return Long.compare(Long.parseLong(o1.toString()), Long.parseLong(o2.toString()));
			}
		});
		tableRowSorter.setSortable(column, isColumnSortable);
	}

	//	________________________________________________________________________

	public void paint(Graphics g) {
		super.paint(g);

		if (drawingTool != null)
			drawingTool.paintComponent(g);
	}
	//	________________________________________________________________________

	public void addMouseListener(MouseListener adapter) {
		if (componentList == null)
			return;

		for (int i=0; i < componentList.size(); i++) {
			JXTable table = (JXTable) componentList.get(i);
			table.addMouseListener(adapter);
		}
	}

	public void removeMouseListener(MouseListener adapter) {
		if (componentList == null)
			return;

		for (int i=0; i < componentList.size(); i++) {
			JXTable table = (JXTable) componentList.get(i);
			table.removeMouseListener(adapter);
		}
	}

	//	________________________________________________________________________

	public void addMouseMotionListener(MouseMotionListener adapter) {
		if (componentList == null)
			return;

		for (int i=0; i < componentList.size(); i++) {
			JXTable table = (JXTable) componentList.get(i);
			table.addMouseMotionListener(adapter);
		}
	}

	public void removeMouseMotionListener(MouseMotionListener adapter) {
		if (componentList == null)
			return;

		for (int i=0; i < componentList.size(); i++) {
			JXTable table = (JXTable) componentList.get(i);
			table.removeMouseMotionListener(adapter);
		}
	}

	public void setComponentPopupMenu(JPopupMenu popup) {
		if (componentList == null)
			return;

		if (mouseListener != null)
			removeMouseListener(mouseListener);

		this.popup = popup;

		if (popup != null) {
			this.mouseListener = new LocalPopupMouseListener();
			addMouseListener(mouseListener);
		}
	}

	//	________________________________________________________________________

	public DrawingTool getDrawingTool() {
		return drawingTool;
	}

	public void setDrawingTool(DrawingTool drawingTool) {
		if (this.drawingTool != null)
			this.drawingTool.terminate();

		this.drawingTool = drawingTool;

		if (this.drawingTool != null)
			drawingTool.intialize(this);
	}

	public int getRowCount() {
		return getModel().getRowCount();
	}

	public int getColumnCount() {
		return getModel().getColumnCount();
	}

	public TableModel getModel() {
		return columnManager.getModel();
	}

	public JXTable getTableComponent(int index) {
		return (JXTable) componentList.get(index);
	}

	public TableColumnModel getColumnModel(int index) {
		return columnManager.get(index);
	}

	public TableCellEditor getDefaultEditor(int index, Class columnClass) {
		JXTable table = getTableComponent(index);
		return table != null ? table.getDefaultEditor(columnClass) : null;
	}

	public TableCellRenderer getDefaultRenderer(int index, Class columnClass) {
		JXTable table = getTableComponent(index);
		return table != null ? table.getDefaultRenderer(columnClass) : null;
	}

	public int getColumnModelCount() {
		return columnManager.size();
	}

	public JTableHeader getTableHeader(int index) {
		JXTable table = getTableComponent(index);
		return table != null ? table.getTableHeader() : null;
	}

	public DrawingContext getDrawingContext() {
		return ganttDynamicComponentUtilities.getContext();
	}

	/**
	 public DrawingComponent getDefaultDrawingComponent(int index) {
	 JXTable table = getTableComponent(index);

	 TableCellRenderer renderer = table.getDefaultRenderer(AbstractDrawingState.class);
	 if (renderer instanceof JTableRendererAdapter) {

	 }
	 return c != null ? SwingUtilities.test() : null;

	 }*/

	public ViewManager getViewManager(String axis) {
		DrawingContext context = getDrawingContext();
		return (ViewManager) context.get(axis, ContextResources.VIEW_MANAGER);
	}

	public JXTable getJXTable(){
		return (JXTable)componentList.get(0);
	}

	//	________________________________________________________________________

	/**
	 * This code is used to set the start and end points of the time line
	 */
	public boolean setTimeRange(Date start, Date finish) {
		if (start == null || finish == null)
			return false;

		if (start.equals(finish) || start.after(finish))
			return false;

		LongInterval interval = new LongInterval(start.getTime(), finish.getTime());
        ViewManager viewManager = getViewManager(GanttTable.TIME_AXIS);
        viewManager.getAxis().setInterval(interval);
        return true;
	}

	public void cancelEditing() {

		if (componentList == null)
			return;

		for (int i=0; i < componentList.size(); i++) {
			JXTable table = (JXTable) componentList.get(i);

			if (table.isEditing()) {
				// try to stop cell editing, and failing that, cancel it
				if (!table.getCellEditor().stopCellEditing()) {
					table.getCellEditor().cancelCellEditing();
				}
			}
		}
	}

	public void setRowHeightForJTable(int rowHeightForJTable) {
		if(componentList == null)
			return;
		for(int i = 0; i < componentList.size(); i++) {
			JXTable table = (JXTable)componentList.get(i);
			table.setRowHeight(rowHeightForJTable);
		}
	}

	public void setVisibleRowCount(int visibleRowCount) {
		if(componentList == null)
			return;
		for(int i = 0; i < componentList.size(); i++) {
			JXTable table = (JXTable)componentList.get(i);
			table.setVisibleRowCount(visibleRowCount);
		}
	}

/*
	public void setLongComparatorByColumn(int columnIndex){
		TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(getJXTable().getModel());
		getJXTable().setRowSorter(rowSorter);

		rowSorter.setComparator(columnIndex, (Comparator<String>) (o1, o2) -> {
			System.out.println(Long.parseLong(o1.toString()));
			System.out.println(Long.parseLong(o2.toString()));

			return Long.compare(Long.parseLong(o1.toString()), Long.parseLong(o2.toString()));
		});
	}*/

	//	________________________________________________________________________

	protected class LocalPopupMouseListener extends MouseAdapter {

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				popup.show(e.getComponent(),
						e.getX(), e.getY());
			}
		}
	}


	public static boolean isNumeric(String str) {
		if (str == null) {
			return false;
		}
		int sz = str.length();
		for (int i = 0; i < sz; i++) {
			if (Character.isDigit(str.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}


}