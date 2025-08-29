package ext.egantt.actions;

import java.awt.Graphics;

import ext.egantt.swing.GanttTable;

public interface DrawingTool {
	
	void intialize(GanttTable table);
	
	void terminate();

	//	________________________________________________________________________
	
	void paintComponent(Graphics g);
}