package ext.egantt.drawing.module;

import com.egantt.awt.graphics.GraphicsContext;
import com.egantt.awt.paint.VerticalGradientPaint;
import com.egantt.model.drawing.ContextResources;
import com.egantt.model.drawing.DrawingContext;
import ext.egantt.drawing.DrawingModule;
import ext.egantt.drawing.context.GradientColorContext;
import java.awt.Color;
import java.awt.Paint;
import java.util.Map;

public class GradientColorModule implements DrawingModule {

  public static final class LocalColorContext implements GraphicsContext {

    public Object get(Object key,
                      Object type) {
      return "Paint".equals(type) ? color : null;
    }

    public Paint getPaint() {
      return color;
    }

    private final Paint color;

    public LocalColorContext(Color color) {
      this.color = new VerticalGradientPaint(color, color);
    }
  }

  public GradientColorModule() {}

  public void initialise(DrawingContext attributes) {
    attributes.put("GradientColorContext.BLACK", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.BLACK);
    attributes.put("GradientColorContext.BLUE", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.BLUE);
    attributes.put("GradientColorContext.CYAN", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.CYAN);
    attributes.put("GradientColorContext.DARK_GRAY", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.DARK_GRAY);
    attributes.put("GradientColorContext.GRAY", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.GRAY);
    attributes.put("GradientColorContext.GREEN", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.GREEN);
    attributes.put("GradientColorContext.LIGHT_GRAY", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.LIGHT_GRAY);
    attributes.put("GradientColorContext.MAGENTA", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.MAGENTA);
    attributes.put("GradientColorContext.ORANGE", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.ORANGE);
    attributes.put("GradientColorContext.PINK", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.PINK);
    attributes.put("GradientColorContext.RED", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.RED);
    attributes.put("GradientColorContext.WHITE", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.WHITE);
    attributes.put("GradientColorContext.YELLOW", ContextResources.GRAPHICS_CONTEXT, GradientColorContext.YELLOW);
  }

  public void initialise(DrawingContext attributes,
                         Map<String, Color> seriesColorMap) {
    this.initialise(attributes);

    if (seriesColorMap != null) {
      seriesColorMap.forEach((key, color) -> attributes.put(key, ContextResources.GRAPHICS_CONTEXT, new LocalColorContext(color)));
    }
  }

  public void terminate(DrawingContext attributes) {
    attributes.put("GradientColorContext.BLACK", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.BLUE", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.CYAN", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.DARK_GRAY", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.GRAY", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.GREEN", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.LIGHT_GRAY", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.MAGENTA", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.ORANGE", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.PINK", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.RED", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.WHITE", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.YELLOW", ContextResources.GRAPHICS_CONTEXT, null);

    attributes.put("GradientColorContext.OTHER0", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.CLUSTER11", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.QUEUEING12", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.NETWORK7", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.ADMINISTRATIVE3", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.CONFIGURATION2", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.COMMIT5", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.APPLICATION1", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.CONCURRENCY4", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.SYSTEMIO9", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.USERIO8", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.SCHEDULER10", ContextResources.GRAPHICS_CONTEXT, null);
    attributes.put("GradientColorContext.CPU", ContextResources.GRAPHICS_CONTEXT, null);
  }

  public static final String BLACK_GRADIENT_CONTEXT = "GradientColorContext.BLACK";
  public static final String BLUE_GRADIENT_CONTEXT = "GradientColorContext.BLUE";
  public static final String GREEN_GRADIENT_CONTEXT = "GradientColorContext.GREEN";
  public static final String ORANGE_GRADIENT_CONTEXT = "GradientColorContext.ORANGE";
  public static final String RED_GRADIENT_CONTEXT = "GradientColorContext.RED";
}
