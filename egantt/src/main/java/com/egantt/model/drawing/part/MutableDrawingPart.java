package com.egantt.model.drawing.part;

import com.egantt.model.drawing.DrawingPart;

public interface MutableDrawingPart extends DrawingPart {
	
	void setContext(Object key, Object value);

	void setPainter(Object key, Object value);
	void setState(Object key, Object value);

}