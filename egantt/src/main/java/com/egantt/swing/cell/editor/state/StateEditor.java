package com.egantt.swing.cell.editor.state;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import com.egantt.model.drawing.DrawingContext;
import com.egantt.model.drawing.DrawingState;

public interface StateEditor {
	
	//	________________________________________________________________________
	
	void mousePressed(MouseEvent e, Rectangle bounds, DrawingState drawing, Object axisKey, DrawingContext context);
	
	void mouseReleased(MouseEvent e, Rectangle bounds, DrawingState drawing, Object axisKey, DrawingContext context);

	void mouseMoved(MouseEvent e, Rectangle bounds, DrawingState drawing, Object axisKey, DrawingContext context);

	void mouseDragged(MouseEvent e, Rectangle bounds, DrawingState drawing, Object axisKey, DrawingContext context);
}