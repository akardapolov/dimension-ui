package ext.egantt.chart.scheme;

import java.awt.Color;

import com.egantt.model.drawing.axis.AxisInterval;

import com.egantt.model.drawing.painter.PainterResources;

import ext.egantt.drawing.painter.context.BasicPainterContext;

public class BasicPaintScheme extends BasicPainterContext
{
	public Object get(Object key, String type)
	{
		if (type != PainterResources.PAINT || !(key instanceof AxisInterval interval))
			return super.get(key, type);

		int value  = ((Long)interval.getFinish()).intValue() * 150;

		return new Color(value);
	}
}