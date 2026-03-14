/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2013, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.]
 *
 * ---------------
 * ChartPanel.java
 * ---------------
 * (C) Copyright 2000-2013, by Object Refinery Limited and Contributors.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   Andrzej Porebski;
 *                   Soren Caspersen;
 *                   Jonathan Nash;
 *                   Hans-Jurgen Greiner;
 *                   Andreas Schneider;
 *                   Daniel van Enckevort;
 *                   David M O'Donnell;
 *                   Arnaud Lelievre;
 *                   Matthias Rose;
 *                   Onno vd Akker;
 *                   Sergei Ivanov;
 *                   Ulrich Voigt - patch 2686040;
 *                   Alessandro Borges - patch 1460845;
 *                   Martin Hoeller;
 *                   Michael Zinsmaier;
 *
 * Changes (from 28-Jun-2001)
 * --------------------------
 * 28-Jun-2001 : Integrated buffering code contributed by S???ren
 *               Caspersen (DG);
 * 18-Sep-2001 : Updated header and fixed DOS encoding problem (DG);
 * 22-Nov-2001 : Added scaling to improve display of charts in small sizes (DG);
 * 26-Nov-2001 : Added property editing, saving and printing (DG);
 * 11-Dec-2001 : Transferred saveChartAsPNG method to new ChartUtilities
 *               class (DG);
 * 13-Dec-2001 : Added tooltips (DG);
 * 16-Jan-2002 : Added an optional crosshair, based on the implementation by
 *               Jonathan Nash. Renamed the tooltips class (DG);
 * 23-Jan-2002 : Implemented zooming based on code by Hans-Jurgen Greiner (DG);
 * 05-Feb-2002 : Improved tooltips setup.  Renamed method attemptSaveAs()
 *               --> doSaveAs() and made it public rather than private (DG);
 * 28-Mar-2002 : Added a new constructor (DG);
 * 09-Apr-2002 : Changed initialisation of tooltip generation, as suggested by
 *               Hans-Jurgen Greiner (DG);
 * 27-May-2002 : New interactive zooming methods based on code by Hans-Jurgen
 *               Greiner. Renamed JFreeChartPanel --> ChartPanel, moved
 *               constants to ChartPanelConstants interface (DG);
 * 31-May-2002 : Fixed a bug with interactive zooming and added a way to
 *               control if the zoom rectangle is filled in or drawn as an
 *               outline. A mouse drag gesture towards the top left now causes
 *               an autoRangeBoth() and is a way to undo zooms (AS);
 * 11-Jun-2002 : Reinstated handleClick method call in mouseClicked() to get
 *               crosshairs working again (DG);
 * 13-Jun-2002 : Added check for null popup menu in mouseDragged method (DG);
 * 18-Jun-2002 : Added get/set methods for minimum and maximum chart
 *               dimensions (DG);
 * 25-Jun-2002 : Removed redundant code (DG);
 * 27-Aug-2002 : Added get/set methods for popup menu (DG);
 * 26-Sep-2002 : Fixed errors reported by Checkstyle (DG);
 * 22-Oct-2002 : Added translation methods for screen <--> Java2D, contributed
 *               by Daniel van Enckevort (DG);
 * 05-Nov-2002 : Added a chart reference to the ChartMouseEvent class (DG);
 * 22-Nov-2002 : Added test in zoom method for inverted axes, supplied by
 *               David M O'Donnell (DG);
 * 14-Jan-2003 : Implemented ChartProgressListener interface (DG);
 * 14-Feb-2003 : Removed deprecated setGenerateTooltips method (DG);
 * 12-Mar-2003 : Added option to enforce filename extension (see bug id
 *               643173) (DG);
 * 08-Sep-2003 : Added internationalization via use of properties
 *               resourceBundle (RFE 690236) (AL);
 * 18-Sep-2003 : Added getScaleX() and getScaleY() methods (protected) as
 *               requested by Irv Thomae (DG);
 * 12-Nov-2003 : Added zooming support for the FastScatterPlot class (DG);
 * 24-Nov-2003 : Minor Javadoc updates (DG);
 * 04-Dec-2003 : Added anchor point for crosshair calculation (DG);
 * 17-Jan-2004 : Added new methods to set tooltip delays to be used in this
 *               chart panel. Refer to patch 877565 (MR);
 * 02-Feb-2004 : Fixed bug in zooming trigger and added zoomTriggerDistance
 *               attribute (DG);
 * 08-Apr-2004 : Changed getScaleX() and getScaleY() from protected to
 *               public (DG);
 * 15-Apr-2004 : Added zoomOutFactor and zoomInFactor (DG);
 * 21-Apr-2004 : Fixed zooming bug in mouseReleased() method (DG);
 * 13-Jul-2004 : Added check for null chart (DG);
 * 04-Oct-2004 : Renamed ShapeUtils --> ShapeUtilities (DG);
 * 11-Nov-2004 : Moved constants back in from ChartPanelConstants (DG);
 * 12-Nov-2004 : Modified zooming mechanism to support zooming within
 *               subplots (DG);
 * 26-Jan-2005 : Fixed mouse zooming for horizontal category plots (DG);
 * 11-Apr-2005 : Added getFillZoomRectangle() method, renamed
 *               setHorizontalZoom() --> setDomainZoomable(),
 *               setVerticalZoom() --> setRangeZoomable(), added
 *               isDomainZoomable() and isRangeZoomable(), added
 *               getHorizontalAxisTrace() and getVerticalAxisTrace(),
 *               renamed autoRangeBoth() --> restoreAutoBounds(),
 *               autoRangeHorizontal() --> restoreAutoDomainBounds(),
 *               autoRangeVertical() --> restoreAutoRangeBounds() (DG);
 * 12-Apr-2005 : Removed working areas, added getAnchorPoint() method,
 *               added protected accessors for tracelines (DG);
 * 18-Apr-2005 : Made constants final (DG);
 * 26-Apr-2005 : Removed LOGGER (DG);
 * 01-Jun-2005 : Fixed zooming for combined plots - see bug report
 *               1212039, fix thanks to Onno vd Akker (DG);
 * 25-Nov-2005 : Reworked event listener mechanism (DG);
 * ------------- JFREECHART 1.0.x ---------------------------------------------
 * 01-Aug-2006 : Fixed minor bug in restoreAutoRangeBounds() (DG);
 * 04-Sep-2006 : Renamed attemptEditChartProperties() -->
 *               doEditChartProperties() and made public (DG);
 * 13-Sep-2006 : Don't generate ChartMouseEvents if the panel's chart is null
 *               (fixes bug 1556951) (DG);
 * 05-Mar-2007 : Applied patch 1672561 by Sergei Ivanov, to fix zoom rectangle
 *               drawing for dynamic charts (DG);
 * 17-Apr-2007 : Fix NullPointerExceptions in zooming for combined plots (DG);
 * 24-May-2007 : When the look-and-feel changes, update the popup menu if there
 *               is one (DG);
 * 06-Jun-2007 : Fixed coordinates for drawing buffer image (DG);
 * 24-Sep-2007 : Added zoomAroundAnchor flag, and handle clearing of chart
 *               buffer (DG);
 * 25-Oct-2007 : Added default directory attribute (DG);
 * 07-Nov-2007 : Fixed (rare) bug in refreshing off-screen image (DG);
 * 07-May-2008 : Fixed bug in zooming that triggered zoom for a rectangle
 *               outside of the data area (DG);
 * 08-May-2008 : Fixed serialization bug (DG);
 * 15-Aug-2008 : Increased default maxDrawWidth/Height (DG);
 * 18-Sep-2008 : Modified creation of chart buffer (DG);
 * 18-Dec-2008 : Use ResourceBundleWrapper - see patch 1607918 by
 *               Jess Thrysoee (DG);
 * 13-Jan-2009 : Fixed zooming methods to trigger only one plot
 *               change event (DG);
 * 16-Jan-2009 : Use XOR for zoom rectangle only if useBuffer is false (DG);
 * 18-Mar-2009 : Added mouse wheel support (DG);
 * 19-Mar-2009 : Added panning on mouse drag support - based on Ulrich
 *               Voigt's patch 2686040 (DG);
 * 26-Mar-2009 : Changed fillZoomRectangle default to true, and only change
 *               cursor for CTRL-mouse-click if panning is enabled (DG);
 * 01-Apr-2009 : Fixed panning, and added different mouse event mask for
 *               MacOSX (DG);
 * 08-Apr-2009 : Added copy to clipboard support, based on patch 1460845
 *               by Alessandro Borges (DG);
 * 09-Apr-2009 : Added overlay support (DG);
 * 10-Apr-2009 : Set chartBuffer background to match ChartPanel (DG);
 * 05-May-2009 : Match scaling (and insets) in doCopy() (DG);
 * 01-Jun-2009 : Check for null chart in mousePressed() method (DG);
 * 08-Jun-2009 : Fixed bug in setMouseWheelEnabled() (DG);
 * 06-Jul-2009 : Clear off-screen buffer to fully transparent (DG);
 * 10-Oct-2011 : localization fix: bug #3353913 (MH);
 * 15-Jun-2012 : Removed JCommon dependencies (DG);
 *
 */

package org.jfree.chart;

import org.jfree.chart.editor.ChartEditor;
import org.jfree.chart.editor.ChartEditorManager;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.*;
import org.jfree.chart.panel.AbstractMouseHandler;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.panel.ZoomHandler;
import org.jfree.chart.panel.selectionhandler.RectangularHeightRegionSelectionHandler;
import org.jfree.chart.panel.selectionhandler.SelectionManager;
import org.jfree.chart.plot.*;
import org.jfree.chart.util.*;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/**
 * A Swing GUI component for displaying a {@link JFreeChart} object.
 * <p>
 * The panel registers with the chart to receive notification of changes to any component of the chart.  The chart is
 * redrawn automatically whenever this notification is received.
 */
public class ChartPanel extends JPanel implements ChartChangeListener,
    ChartProgressListener, ActionListener, MouseListener,
    MouseMotionListener, OverlayChangeListener, Printable, Serializable {

  private static final long serialVersionUID = 6046366297214274674L;

  public static final boolean DEFAULT_BUFFER_USED = true;
  public static final int DEFAULT_WIDTH = 680;
  public static final int DEFAULT_HEIGHT = 420;
  public static final int DEFAULT_MINIMUM_DRAW_WIDTH = 300;
  public static final int DEFAULT_MINIMUM_DRAW_HEIGHT = 200;
  public static final int DEFAULT_MAXIMUM_DRAW_WIDTH = 1024;
  public static final int DEFAULT_MAXIMUM_DRAW_HEIGHT = 768;
  public static final int DEFAULT_ZOOM_TRIGGER_DISTANCE = 10;

  public static final String PROPERTIES_COMMAND = "PROPERTIES";
  public static final String COPY_COMMAND = "COPY";
  public static final String SAVE_COMMAND = "SAVE";
  private static final String SAVE_AS_PNG_COMMAND = "SAVE_AS_PNG";
  private static final String SAVE_AS_SVG_COMMAND = "SAVE_AS_SVG";
  private static final String SAVE_AS_PDF_COMMAND = "SAVE_AS_PDF";
  public static final String PRINT_COMMAND = "PRINT";
  public static final String ZOOM_IN_BOTH_COMMAND = "ZOOM_IN_BOTH";
  public static final String ZOOM_IN_DOMAIN_COMMAND = "ZOOM_IN_DOMAIN";
  public static final String ZOOM_IN_RANGE_COMMAND = "ZOOM_IN_RANGE";
  public static final String ZOOM_OUT_BOTH_COMMAND = "ZOOM_OUT_BOTH";
  public static final String ZOOM_OUT_DOMAIN_COMMAND = "ZOOM_DOMAIN_BOTH";
  public static final String ZOOM_OUT_RANGE_COMMAND = "ZOOM_RANGE_BOTH";
  public static final String ZOOM_RESET_BOTH_COMMAND = "ZOOM_RESET_BOTH";
  public static final String ZOOM_RESET_DOMAIN_COMMAND = "ZOOM_RESET_DOMAIN";
  public static final String ZOOM_RESET_RANGE_COMMAND = "ZOOM_RESET_RANGE";

  private JFreeChart chart;
  private transient EventListenerList chartMouseListeners;
  private boolean useBuffer;
  private boolean refreshBuffer;
  private transient Image chartBuffer;
  private int chartBufferHeight;
  private int chartBufferWidth;
  private int minimumDrawWidth;
  private int minimumDrawHeight;
  private int maximumDrawWidth;
  private int maximumDrawHeight;
  private JPopupMenu popup;
  private ChartRenderingInfo info;
  private Point2D anchor;
  private double scaleX;
  private double scaleY;
  private PlotOrientation orientation = PlotOrientation.VERTICAL;
  private boolean domainZoomable = false;
  private boolean rangeZoomable = false;
  private Point2D zoomPoint = null;
  private transient Rectangle2D zoomRectangle = null;
  private boolean fillZoomRectangle = true;
  private int zoomTriggerDistance;
  private boolean horizontalAxisTrace = false;
  private boolean verticalAxisTrace = false;
  private transient Line2D verticalTraceLine;
  private transient Line2D horizontalTraceLine;
  private JMenuItem zoomInBothMenuItem;
  private JMenuItem zoomInDomainMenuItem;
  private JMenuItem zoomInRangeMenuItem;
  private JMenuItem zoomOutBothMenuItem;
  private JMenuItem zoomOutDomainMenuItem;
  private JMenuItem zoomOutRangeMenuItem;
  private JMenuItem zoomResetBothMenuItem;
  private JMenuItem zoomResetDomainMenuItem;
  private JMenuItem zoomResetRangeMenuItem;
  private File defaultDirectoryForSaveAs;
  private boolean enforceFileExtensions;
  private boolean ownToolTipDelaysActive;
  private int originalToolTipInitialDelay;
  private int originalToolTipReshowDelay;
  private int originalToolTipDismissDelay;
  private int ownToolTipInitialDelay;
  private int ownToolTipReshowDelay;
  private int ownToolTipDismissDelay;
  private double zoomInFactor = 0.5;
  private double zoomOutFactor = 2.0;
  private boolean zoomAroundAnchor;
  private transient Paint zoomOutlinePaint;
  private transient Paint zoomFillPaint;

  protected static ResourceBundle localizationResources
      = ResourceBundleWrapper.getBundle(
      "org.jfree.chart.LocalizationBundle");

  private List<Overlay> overlays;
  private SelectionManager selectionManager;
  private List<AbstractMouseHandler> availableLiveMouseHandlers;
  private AbstractMouseHandler liveMouseHandler;
  private List<AbstractMouseHandler> auxiliaryMouseHandlers;
  private ZoomHandler zoomHandler;
  private Shape selectionShape;
  private Paint selectionFillPaint;
  private Paint selectionOutlinePaint = Color.darkGray;
  private transient Stroke selectionOutlineStroke = new BasicStroke(1.0f,
                                                                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 4.0f, new float[]{
      3.0f, 3.0f}, 0.0f);

  private double dLeft;
  private double dRight;

  private static final Paint HANDLE_FILL_PAINT = new Color(255, 165, 0, 120);
  private static final Paint HANDLE_OUTLINE_PAINT = new Color(200, 120, 0);
  private static final Stroke HANDLE_OUTLINE_STROKE = new BasicStroke(1.0f);

  public ChartPanel(JFreeChart chart) {
    this(
        chart,
        DEFAULT_WIDTH,
        DEFAULT_HEIGHT,
        DEFAULT_MINIMUM_DRAW_WIDTH,
        DEFAULT_MINIMUM_DRAW_HEIGHT,
        DEFAULT_MAXIMUM_DRAW_WIDTH,
        DEFAULT_MAXIMUM_DRAW_HEIGHT,
        DEFAULT_BUFFER_USED,
        true,
        true,
        true,
        true,
        true
    );
  }

  public ChartPanel(JFreeChart chart, boolean useBuffer) {
    this(chart, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH,
         DEFAULT_MINIMUM_DRAW_HEIGHT, DEFAULT_MAXIMUM_DRAW_WIDTH,
         DEFAULT_MAXIMUM_DRAW_HEIGHT, useBuffer,
         true, true, true, true, true
    );
  }

  public ChartPanel(JFreeChart chart,
                    boolean properties,
                    boolean save,
                    boolean print,
                    boolean zoom,
                    boolean tooltips) {
    this(chart,
         DEFAULT_WIDTH,
         DEFAULT_HEIGHT,
         DEFAULT_MINIMUM_DRAW_WIDTH,
         DEFAULT_MINIMUM_DRAW_HEIGHT,
         DEFAULT_MAXIMUM_DRAW_WIDTH,
         DEFAULT_MAXIMUM_DRAW_HEIGHT,
         DEFAULT_BUFFER_USED,
         properties,
         save,
         print,
         zoom,
         tooltips
    );
  }

  public ChartPanel(JFreeChart chart,
                    int width,
                    int height,
                    int minimumDrawWidth,
                    int minimumDrawHeight,
                    int maximumDrawWidth,
                    int maximumDrawHeight,
                    boolean useBuffer,
                    boolean properties,
                    boolean save,
                    boolean print,
                    boolean zoom,
                    boolean tooltips) {
    this(chart, width, height, minimumDrawWidth, minimumDrawHeight,
         maximumDrawWidth, maximumDrawHeight, useBuffer, properties,
         true, save, print, zoom, tooltips);
  }

  public ChartPanel(JFreeChart chart,
                    int width,
                    int height,
                    int minimumDrawWidth,
                    int minimumDrawHeight,
                    int maximumDrawWidth,
                    int maximumDrawHeight,
                    boolean useBuffer,
                    boolean properties,
                    boolean copy,
                    boolean save,
                    boolean print,
                    boolean zoom,
                    boolean tooltips) {
    setChart(chart);
    this.chartMouseListeners = new EventListenerList();
    this.info = new ChartRenderingInfo();
    setPreferredSize(new Dimension(width, height));
    this.useBuffer = useBuffer;
    this.refreshBuffer = false;
    this.minimumDrawWidth = minimumDrawWidth;
    this.minimumDrawHeight = minimumDrawHeight;
    this.maximumDrawWidth = maximumDrawWidth;
    this.maximumDrawHeight = maximumDrawHeight;
    this.zoomTriggerDistance = DEFAULT_ZOOM_TRIGGER_DISTANCE;

    this.popup = null;
    if (properties || copy || save || print || zoom) {
      this.popup = createPopupMenu(properties, copy, save, print, zoom);
    }

    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
    setDisplayToolTips(tooltips);
    addMouseListener(this);
    addMouseMotionListener(this);

    this.defaultDirectoryForSaveAs = null;
    this.enforceFileExtensions = true;

    ToolTipManager ttm = ToolTipManager.sharedInstance();
    this.ownToolTipInitialDelay = ttm.getInitialDelay();
    this.ownToolTipDismissDelay = ttm.getDismissDelay();
    this.ownToolTipReshowDelay = ttm.getReshowDelay();

    this.zoomAroundAnchor = false;
    this.zoomOutlinePaint = Color.BLUE;
    this.zoomFillPaint = new Color(0, 0, 255, 63);

    this.overlays = new ArrayList<Overlay>();

    this.availableLiveMouseHandlers = new ArrayList<AbstractMouseHandler>();

    this.zoomHandler = new ZoomHandler();
    this.availableLiveMouseHandlers.add(zoomHandler);

    this.auxiliaryMouseHandlers = new ArrayList<AbstractMouseHandler>();
  }

  public JFreeChart getChart() {
    return this.chart;
  }

  public void setChart(JFreeChart chart) {
    if (this.chart != null) {
      this.chart.removeChangeListener(this);
      this.chart.removeProgressListener(this);
    }
    this.chart = chart;
    if (chart != null) {
      this.chart.addChangeListener(this);
      this.chart.addProgressListener(this);
      Plot plot = chart.getPlot();
      this.domainZoomable = false;
      this.rangeZoomable = false;
      if (plot instanceof Zoomable) {
        Zoomable z = (Zoomable) plot;
        this.domainZoomable = z.isDomainZoomable();
        this.rangeZoomable = z.isRangeZoomable();
        this.orientation = z.getOrientation();
      }
    } else {
      this.domainZoomable = false;
      this.rangeZoomable = false;
    }
    if (this.useBuffer) {
      this.refreshBuffer = true;
    }
    repaint();
  }

  public int getMinimumDrawWidth() {
    return this.minimumDrawWidth;
  }

  public void setMinimumDrawWidth(int width) {
    this.minimumDrawWidth = width;
  }

  public int getMaximumDrawWidth() {
    return this.maximumDrawWidth;
  }

  public void setMaximumDrawWidth(int width) {
    this.maximumDrawWidth = width;
  }

  public int getMinimumDrawHeight() {
    return this.minimumDrawHeight;
  }

  public void setMinimumDrawHeight(int height) {
    this.minimumDrawHeight = height;
  }

  public int getMaximumDrawHeight() {
    return this.maximumDrawHeight;
  }

  public void setMaximumDrawHeight(int height) {
    this.maximumDrawHeight = height;
  }

  public double getScaleX() {
    return this.scaleX;
  }

  public double getScaleY() {
    return this.scaleY;
  }

  public Point2D getAnchor() {
    return this.anchor;
  }

  protected void setAnchor(Point2D anchor) {
    this.anchor = anchor;
  }

  public JPopupMenu getPopupMenu() {
    return this.popup;
  }

  public void setPopupMenu(JPopupMenu popup) {
    this.popup = popup;
  }

  public ChartRenderingInfo getChartRenderingInfo() {
    return this.info;
  }

  public void setMouseZoomable(boolean flag) {
    setMouseZoomable(flag, true);
  }

  public void setMouseZoomable(boolean flag, boolean fillRectangle) {
    setDomainZoomable(flag);
    setRangeZoomable(flag);
    setFillZoomRectangle(fillRectangle);
  }

  public boolean isDomainZoomable() {
    return this.domainZoomable;
  }

  public void setDomainZoomable(boolean flag) {
    if (flag) {
      Plot plot = this.chart.getPlot();
      if (plot instanceof Zoomable) {
        Zoomable z = (Zoomable) plot;
        this.domainZoomable = z.isDomainZoomable();
      }
    } else {
      this.domainZoomable = false;
    }
  }

  public boolean isRangeZoomable() {
    return this.rangeZoomable;
  }

  public void setRangeZoomable(boolean flag) {
    if (flag) {
      Plot plot = this.chart.getPlot();
      if (plot instanceof Zoomable) {
        Zoomable z = (Zoomable) plot;
        this.rangeZoomable = z.isRangeZoomable();
      }
    } else {
      this.rangeZoomable = false;
    }
  }

  public boolean getFillZoomRectangle() {
    return this.fillZoomRectangle;
  }

  public void setFillZoomRectangle(boolean flag) {
    this.fillZoomRectangle = flag;
  }

  public int getZoomTriggerDistance() {
    return this.zoomTriggerDistance;
  }

  public void setZoomTriggerDistance(int distance) {
    this.zoomTriggerDistance = distance;
  }

  public boolean getHorizontalAxisTrace() {
    return this.horizontalAxisTrace;
  }

  public void setHorizontalAxisTrace(boolean flag) {
    this.horizontalAxisTrace = flag;
  }

  protected Line2D getHorizontalTraceLine() {
    return this.horizontalTraceLine;
  }

  protected void setHorizontalTraceLine(Line2D line) {
    this.horizontalTraceLine = line;
  }

  public boolean getVerticalAxisTrace() {
    return this.verticalAxisTrace;
  }

  public void setVerticalAxisTrace(boolean flag) {
    this.verticalAxisTrace = flag;
  }

  protected Line2D getVerticalTraceLine() {
    return this.verticalTraceLine;
  }

  protected void setVerticalTraceLine(Line2D line) {
    this.verticalTraceLine = line;
  }

  public File getDefaultDirectoryForSaveAs() {
    return this.defaultDirectoryForSaveAs;
  }

  public void setDefaultDirectoryForSaveAs(File directory) {
    if (directory != null) {
      if (!directory.isDirectory()) {
        throw new IllegalArgumentException(
            "The 'directory' argument is not a directory.");
      }
    }
    this.defaultDirectoryForSaveAs = directory;
  }

  public boolean isEnforceFileExtensions() {
    return this.enforceFileExtensions;
  }

  public void setEnforceFileExtensions(boolean enforce) {
    this.enforceFileExtensions = enforce;
  }

  public boolean getZoomAroundAnchor() {
    return this.zoomAroundAnchor;
  }

  public void setZoomAroundAnchor(boolean zoomAroundAnchor) {
    this.zoomAroundAnchor = zoomAroundAnchor;
  }

  public Paint getZoomFillPaint() {
    return this.zoomFillPaint;
  }

  public void setZoomFillPaint(Paint paint) {
    ParamChecks.nullNotPermitted(paint, "paint");
    this.zoomFillPaint = paint;
  }

  public Paint getZoomOutlinePaint() {
    return this.zoomOutlinePaint;
  }

  public void setZoomOutlinePaint(Paint paint) {
    this.zoomOutlinePaint = paint;
  }

  private MouseWheelHandler mouseWheelHandler;

  public boolean isMouseWheelEnabled() {
    return this.mouseWheelHandler != null;
  }

  public void setMouseWheelEnabled(boolean flag) {
    if (flag && this.mouseWheelHandler == null) {
      this.mouseWheelHandler = new MouseWheelHandler(this);
    } else if (!flag && this.mouseWheelHandler != null) {
      this.removeMouseWheelListener(this.mouseWheelHandler);
      this.mouseWheelHandler = null;
    }
  }

  public void addOverlay(Overlay overlay) {
    ParamChecks.nullNotPermitted(overlay, "overlay");
    this.overlays.add(overlay);
    overlay.addChangeListener(this);
    repaint();
  }

  public void removeOverlay(Overlay overlay) {
    ParamChecks.nullNotPermitted(overlay, "overlay");
    boolean removed = this.overlays.remove(overlay);
    if (removed) {
      overlay.removeChangeListener(this);
      repaint();
    }
  }

  @Override
  public void overlayChanged(OverlayChangeEvent event) {
    repaint();
  }

  public void setDisplayToolTips(boolean flag) {
    if (flag) {
      ToolTipManager.sharedInstance().registerComponent(this);
    } else {
      ToolTipManager.sharedInstance().unregisterComponent(this);
    }
  }

  @Override
  public String getToolTipText(MouseEvent e) {
    String result = null;
    if (this.info != null) {
      EntityCollection entities = this.info.getEntityCollection();
      if (entities != null) {
        Insets insets = getInsets();
        ChartEntity entity = entities.getEntity(
            (int) ((e.getX() - insets.left) / this.scaleX),
            (int) ((e.getY() - insets.top) / this.scaleY));
        if (entity != null) {
          result = entity.getToolTipText();
        }
      }
    }
    return result;
  }

  public Point translateJava2DToScreen(Point2D java2DPoint) {
    Insets insets = getInsets();
    int x = (int) (java2DPoint.getX() * this.scaleX + insets.left);
    int y = (int) (java2DPoint.getY() * this.scaleY + insets.top);
    return new Point(x, y);
  }

  public Point2D translateScreenToJava2D(Point screenPoint) {
    Insets insets = getInsets();
    double x = (screenPoint.getX() - insets.left) / this.scaleX;
    double y = (screenPoint.getY() - insets.top) / this.scaleY;
    return new Point2D.Double(x, y);
  }

  public Rectangle2D scale(Rectangle2D rect) {
    Insets insets = getInsets();
    double x = rect.getX() * getScaleX() + insets.left;
    double y = rect.getY() * getScaleY() + insets.top;
    double w = rect.getWidth() * getScaleX();
    double h = rect.getHeight() * getScaleY();
    return new Rectangle2D.Double(x, y, w, h);
  }

  public ChartEntity getEntityForPoint(int viewX, int viewY) {
    ChartEntity result = null;
    if (this.info != null) {
      Insets insets = getInsets();
      double x = (viewX - insets.left) / this.scaleX;
      double y = (viewY - insets.top) / this.scaleY;
      EntityCollection entities = this.info.getEntityCollection();
      result = entities != null ? entities.getEntity(x, y) : null;
    }
    return result;
  }

  public boolean getRefreshBuffer() {
    return this.refreshBuffer;
  }

  public void setRefreshBuffer(boolean flag) {
    this.refreshBuffer = flag;
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (this.chart == null) {
      return;
    }
    Graphics2D g2 = (Graphics2D) g.create();

    Dimension size = getSize();
    Insets insets = getInsets();
    Rectangle2D available = new Rectangle2D.Double(insets.left, insets.top,
                                                   size.getWidth() - insets.left - insets.right,
                                                   size.getHeight() - insets.top - insets.bottom);

    boolean scale = false;
    double drawWidth = available.getWidth();
    double drawHeight = available.getHeight();
    this.scaleX = 1.0;
    this.scaleY = 1.0;

    if (drawWidth < this.minimumDrawWidth) {
      this.scaleX = drawWidth / this.minimumDrawWidth;
      drawWidth = this.minimumDrawWidth;
      scale = true;
    } else if (drawWidth > this.maximumDrawWidth) {
      this.scaleX = drawWidth / this.maximumDrawWidth;
      drawWidth = this.maximumDrawWidth;
      scale = true;
    }

    if (drawHeight < this.minimumDrawHeight) {
      this.scaleY = drawHeight / this.minimumDrawHeight;
      drawHeight = this.minimumDrawHeight;
      scale = true;
    } else if (drawHeight > this.maximumDrawHeight) {
      this.scaleY = drawHeight / this.maximumDrawHeight;
      drawHeight = this.maximumDrawHeight;
      scale = true;
    }

    Rectangle2D chartArea = new Rectangle2D.Double(0.0, 0.0, drawWidth, drawHeight);

    if (this.useBuffer) {
      if ((this.chartBuffer == null)
          || (this.chartBufferWidth != available.getWidth())
          || (this.chartBufferHeight != available.getHeight())) {
        this.chartBufferWidth = (int) available.getWidth();
        this.chartBufferHeight = (int) available.getHeight();
        GraphicsConfiguration gc = g2.getDeviceConfiguration();
        this.chartBuffer = gc.createCompatibleImage(
            this.chartBufferWidth, this.chartBufferHeight,
            Transparency.TRANSLUCENT);
        this.refreshBuffer = true;
      }

      if (this.refreshBuffer) {
        this.refreshBuffer = false;

        Rectangle2D bufferArea = new Rectangle2D.Double(
            0, 0, this.chartBufferWidth, this.chartBufferHeight);

        Graphics2D bufferG2 = (Graphics2D) this.chartBuffer.getGraphics();
        Composite savedComposite = bufferG2.getComposite();
        bufferG2.setComposite(AlphaComposite.getInstance(
            AlphaComposite.CLEAR, 0.0f));
        Rectangle r = new Rectangle(0, 0, this.chartBufferWidth,
                                    this.chartBufferHeight);
        bufferG2.fill(r);
        bufferG2.setComposite(savedComposite);

        if (scale) {
          AffineTransform saved = bufferG2.getTransform();
          AffineTransform st = AffineTransform.getScaleInstance(
              this.scaleX, this.scaleY);
          bufferG2.transform(st);
          try {
            this.chart.draw(bufferG2, chartArea, this.anchor, this.info);
          } catch (IndexOutOfBoundsException e) {
            System.out.println("IndexOutOfBoundsException in ChartPanel:paintComponent " + e.getMessage());
            e.printStackTrace();
          }
          bufferG2.setTransform(saved);
        } else {
          this.chart.draw(bufferG2, bufferArea, this.anchor, this.info);
        }
      }

      g2.drawImage(this.chartBuffer, insets.left, insets.top, this);
    } else {
      AffineTransform saved = g2.getTransform();
      g2.translate(insets.left, insets.top);
      if (scale) {
        AffineTransform st = AffineTransform.getScaleInstance(
            this.scaleX, this.scaleY);
        g2.transform(st);
      }
      this.chart.draw(g2, chartArea, this.anchor, this.info);
      g2.setTransform(saved);
    }

    for (Overlay overlay : this.overlays) {
      overlay.paintOverlay(g2, this);
    }

    drawZoomRectangle(g2, !this.useBuffer);
    drawSelectionShape(g2, !this.useBuffer);
    g2.dispose();

    this.anchor = null;
    this.verticalTraceLine = null;
    this.horizontalTraceLine = null;
  }

  @Override
  public void chartChanged(ChartChangeEvent event) {
    this.refreshBuffer = true;
    Plot plot = this.chart.getPlot();
    if (plot instanceof Zoomable) {
      Zoomable z = (Zoomable) plot;
      this.orientation = z.getOrientation();
    }
    repaint();
  }

  @Override
  public void chartProgress(ChartProgressEvent event) {
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    String command = event.getActionCommand();

    double screenX = -1.0;
    double screenY = -1.0;
    if (this.zoomPoint != null) {
      screenX = this.zoomPoint.getX();
      screenY = this.zoomPoint.getY();
    }

    if (command.equals(PROPERTIES_COMMAND)) {
      doEditChartProperties();
    } else if (command.equals(COPY_COMMAND)) {
      doCopy();
    } else if (command.equals(SAVE_AS_PNG_COMMAND)) {
      try {
        doSaveAs();
      } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "I/O error occurred.",
                                      "Save As PNG", JOptionPane.WARNING_MESSAGE);
      }
    } else if (command.equals(SAVE_AS_SVG_COMMAND)) {
      try {
        saveAsSVG(null);
      } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "I/O error occurred.",
                                      "Save As SVG", JOptionPane.WARNING_MESSAGE);
      }
    } else if (command.equals(SAVE_AS_PDF_COMMAND)) {
      saveAsPDF(null);
    } else if (command.equals(PRINT_COMMAND)) {
      createChartPrintJob();
    } else if (command.equals(ZOOM_IN_BOTH_COMMAND)) {
      zoomInBoth(screenX, screenY);
    } else if (command.equals(ZOOM_IN_DOMAIN_COMMAND)) {
      zoomInDomain(screenX, screenY);
    } else if (command.equals(ZOOM_IN_RANGE_COMMAND)) {
      zoomInRange(screenX, screenY);
    } else if (command.equals(ZOOM_OUT_BOTH_COMMAND)) {
      zoomOutBoth(screenX, screenY);
    } else if (command.equals(ZOOM_OUT_DOMAIN_COMMAND)) {
      zoomOutDomain(screenX, screenY);
    } else if (command.equals(ZOOM_OUT_RANGE_COMMAND)) {
      zoomOutRange(screenX, screenY);
    } else if (command.equals(ZOOM_RESET_BOTH_COMMAND)) {
      restoreAutoBounds();
    } else if (command.equals(ZOOM_RESET_DOMAIN_COMMAND)) {
      restoreAutoDomainBounds();
    } else if (command.equals(ZOOM_RESET_RANGE_COMMAND)) {
      restoreAutoRangeBounds();
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    if (!this.ownToolTipDelaysActive) {
      ToolTipManager ttm = ToolTipManager.sharedInstance();
      this.originalToolTipInitialDelay = ttm.getInitialDelay();
      ttm.setInitialDelay(this.ownToolTipInitialDelay);
      this.originalToolTipReshowDelay = ttm.getReshowDelay();
      ttm.setReshowDelay(this.ownToolTipReshowDelay);
      this.originalToolTipDismissDelay = ttm.getDismissDelay();
      ttm.setDismissDelay(this.ownToolTipDismissDelay);
      this.ownToolTipDelaysActive = true;
    }

    if (this.liveMouseHandler != null) {
      this.liveMouseHandler.mouseEntered(e);
    }

    int mods = e.getModifiers();
    for (AbstractMouseHandler handler : auxiliaryMouseHandlers) {
      if (handler.getModifier() == 0 ||
          (mods & handler.getModifier()) == handler.getModifier()) {
        handler.mouseEntered(e);
      }
    }
  }

  @Override
  public void mouseExited(MouseEvent e) {
    if (this.ownToolTipDelaysActive) {
      ToolTipManager ttm = ToolTipManager.sharedInstance();
      ttm.setInitialDelay(this.originalToolTipInitialDelay);
      ttm.setReshowDelay(this.originalToolTipReshowDelay);
      ttm.setDismissDelay(this.originalToolTipDismissDelay);
      this.ownToolTipDelaysActive = false;
    }

    if (this.liveMouseHandler != null) {
      this.liveMouseHandler.mouseExited(e);
    }

    int mods = e.getModifiers();
    for (AbstractMouseHandler handler : auxiliaryMouseHandlers) {
      if (handler.getModifier() == 0 ||
          (mods & handler.getModifier()) == handler.getModifier()) {
        handler.mouseExited(e);
      }
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    int mods = e.getModifiers();
    if (e.isPopupTrigger()) {
      if (this.popup != null) {
        displayPopupMenu(e.getX(), e.getY());
      }
      return;
    }

    if (this.liveMouseHandler != null) {
      this.liveMouseHandler.mousePressed(e);
    } else {
      AbstractMouseHandler h = null;
      boolean found = false;
      Iterator<AbstractMouseHandler> iterator
          = this.availableLiveMouseHandlers.iterator();
      AbstractMouseHandler nomod = null;
      while (iterator.hasNext() && !found) {
        h = (AbstractMouseHandler) iterator.next();
        if (h.getModifier() == 0 && nomod == null) {
          nomod = h;
        } else {
          found = (mods & h.getModifier()) == h.getModifier();
        }
      }
      if (!found && nomod != null) {
        h = nomod;
        found = true;
      }
      if (found) {
        this.liveMouseHandler = h;
        this.liveMouseHandler.mousePressed(e);
      }
    }

    for (AbstractMouseHandler handler : auxiliaryMouseHandlers) {
      if (handler.getModifier() == 0 ||
          (mods & handler.getModifier()) == handler.getModifier()) {
        handler.mousePressed(e);
      }
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (this.popup != null && this.popup.isShowing()) {
      return;
    }

    if (this.liveMouseHandler != null) {
      this.liveMouseHandler.mouseDragged(e);
    }

    int mods = e.getModifiers();
    for (AbstractMouseHandler handler : auxiliaryMouseHandlers) {
      if (handler.getModifier() == 0 ||
          (mods & handler.getModifier()) == handler.getModifier()) {
        handler.mouseDragged(e);
      }
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (e.isPopupTrigger()) {
      if (this.popup != null) {
        displayPopupMenu(e.getX(), e.getY());
      }
      return;
    }

    if (this.liveMouseHandler != null) {
      this.liveMouseHandler.mouseReleased(e);
    }

    int mods = e.getModifiers();
    for (AbstractMouseHandler handler : auxiliaryMouseHandlers) {
      if (handler.getModifier() == 0 ||
          (mods & handler.getModifier()) == handler.getModifier()) {
        handler.mouseReleased(e);
      }
    }

    fireReleaseMouse();
  }

  public void zoomMReleased(Rectangle2D selection) {
    PlotRenderingInfo plotInfo = this.info.getPlotInfo();
    Rectangle2D scaledDataArea = getScreenDataArea();
    plotInfo.setDataArea(scaledDataArea);

    if ((selection.getHeight() > 0) && (selection.getWidth() > 0)) {
      Plot p = this.chart.getPlot();
      if (p instanceof XYPlot) {
        XYPlot z = (XYPlot) p;
        this.dLeft = z.getJava2dToValue(selection, plotInfo, 0);
        this.dRight = z.getJava2dToValue(selection, plotInfo, 1);
      }
    }
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    Insets insets = getInsets();
    int x = (int) ((event.getX() - insets.left) / this.scaleX);
    int y = (int) ((event.getY() - insets.top) / this.scaleY);

    this.anchor = new Point2D.Double(x, y);
    if (this.chart == null) {
      return;
    }
    this.chart.setNotify(true);

    Object[] listeners = this.chartMouseListeners.getListeners(
        ChartMouseListener.class);
    if (listeners.length > 0) {
      ChartEntity entity = null;
      if (this.info != null) {
        EntityCollection entities = this.info.getEntityCollection();
        if (entities != null) {
          entity = entities.getEntity(x, y);
        }
      }
      ChartMouseEvent chartEvent = new ChartMouseEvent(getChart(), event, entity);
      for (int i = listeners.length - 1; i >= 0; i -= 1) {
        ((ChartMouseListener) listeners[i]).chartMouseClicked(chartEvent);
      }
    }

    if (this.liveMouseHandler != null) {
      this.liveMouseHandler.mouseClicked(event);
    }

    int mods = event.getModifiers();
    for (AbstractMouseHandler handler : auxiliaryMouseHandlers) {
      if (handler.getModifier() == 0 ||
          (mods & handler.getModifier()) == handler.getModifier()) {
        handler.mouseClicked(event);
      }
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    Graphics2D g2 = (Graphics2D) getGraphics();
    if (this.horizontalAxisTrace) {
      drawHorizontalAxisTrace(g2, e.getX());
    }
    if (this.verticalAxisTrace) {
      drawVerticalAxisTrace(g2, e.getY());
    }
    g2.dispose();

    Object[] listeners = this.chartMouseListeners.getListeners(
        ChartMouseListener.class);
    if (listeners.length > 0) {
      Insets insets = getInsets();
      int x = (int) ((e.getX() - insets.left) / this.scaleX);
      int y = (int) ((e.getY() - insets.top) / this.scaleY);

      ChartEntity entity = null;
      if (this.info != null) {
        EntityCollection entities = this.info.getEntityCollection();
        if (entities != null) {
          entity = entities.getEntity(x, y);
        }
      }

      if (this.chart != null) {
        ChartMouseEvent event = new ChartMouseEvent(getChart(), e, entity);
        for (int i = listeners.length - 1; i >= 0; i -= 1) {
          ((ChartMouseListener) listeners[i]).chartMouseMoved(event);
        }
      }
    }

    updateSelectionCursor(e);

    if (this.chart != null) {
      if (this.liveMouseHandler != null) {
        this.liveMouseHandler.mouseMoved(e);
      }
      int mods = e.getModifiers();
      for (AbstractMouseHandler handler : auxiliaryMouseHandlers) {
        if (handler.getModifier() == 0 ||
            (mods & handler.getModifier()) == handler.getModifier()) {
          handler.mouseMoved(e);
        }
      }
    }
  }

  private void updateSelectionCursor(MouseEvent e) {
    if (this.selectionShape == null) {
      setCursor(Cursor.getDefaultCursor());
      return;
    }

    Point pt = e.getPoint();
    Rectangle2D bounds = this.selectionShape.getBounds2D();

    Rectangle2D leftHandle = RectangularHeightRegionSelectionHandler.getLeftHandleRect(bounds);
    Rectangle2D rightHandle = RectangularHeightRegionSelectionHandler.getRightHandleRect(bounds);

    if (leftHandle.contains(pt)) {
      setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
    } else if (rightHandle.contains(pt)) {
      setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
    } else if (bounds.contains(pt)) {
      setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    } else {
      setCursor(Cursor.getDefaultCursor());
    }
  }

  public void zoomInBoth(double x, double y) {
    Plot plot = this.chart.getPlot();
    if (plot == null) {
      return;
    }
    boolean savedNotify = plot.isNotify();
    plot.setNotify(false);
    zoomInDomain(x, y);
    zoomInRange(x, y);
    plot.setNotify(savedNotify);
  }

  public void zoomInDomain(double x, double y) {
    Plot plot = this.chart.getPlot();
    if (plot instanceof Zoomable) {
      boolean savedNotify = plot.isNotify();
      plot.setNotify(false);
      Zoomable z = (Zoomable) plot;
      z.zoomDomainAxes(this.zoomInFactor, this.info.getPlotInfo(),
                       translateScreenToJava2D(new Point((int) x, (int) y)),
                       this.zoomAroundAnchor);
      plot.setNotify(savedNotify);
    }
  }

  public void zoomInRange(double x, double y) {
    Plot plot = this.chart.getPlot();
    if (plot instanceof Zoomable) {
      boolean savedNotify = plot.isNotify();
      plot.setNotify(false);
      Zoomable z = (Zoomable) plot;
      z.zoomRangeAxes(this.zoomInFactor, this.info.getPlotInfo(),
                      translateScreenToJava2D(new Point((int) x, (int) y)),
                      this.zoomAroundAnchor);
      plot.setNotify(savedNotify);
    }
  }

  public void zoomOutBoth(double x, double y) {
    Plot plot = this.chart.getPlot();
    if (plot == null) {
      return;
    }
    boolean savedNotify = plot.isNotify();
    plot.setNotify(false);
    zoomOutDomain(x, y);
    zoomOutRange(x, y);
    plot.setNotify(savedNotify);
  }

  public void zoomOutDomain(double x, double y) {
    Plot plot = this.chart.getPlot();
    if (plot instanceof Zoomable) {
      boolean savedNotify = plot.isNotify();
      plot.setNotify(false);
      Zoomable z = (Zoomable) plot;
      z.zoomDomainAxes(this.zoomOutFactor, this.info.getPlotInfo(),
                       translateScreenToJava2D(new Point((int) x, (int) y)),
                       this.zoomAroundAnchor);
      plot.setNotify(savedNotify);
    }
  }

  public void zoomOutRange(double x, double y) {
    Plot plot = this.chart.getPlot();
    if (plot instanceof Zoomable) {
      boolean savedNotify = plot.isNotify();
      plot.setNotify(false);
      Zoomable z = (Zoomable) plot;
      z.zoomRangeAxes(this.zoomOutFactor, this.info.getPlotInfo(),
                      translateScreenToJava2D(new Point((int) x, (int) y)),
                      this.zoomAroundAnchor);
      plot.setNotify(savedNotify);
    }
  }

  public void zoom(Rectangle2D selection) {
    Point2D selectOrigin = translateScreenToJava2D(new Point(
        (int) Math.ceil(selection.getX()),
        (int) Math.ceil(selection.getY())));
    PlotRenderingInfo plotInfo = this.info.getPlotInfo();
    Rectangle2D scaledDataArea = getScreenDataArea(
        (int) selection.getCenterX(), (int) selection.getCenterY());
    if ((selection.getHeight() > 0) && (selection.getWidth() > 0)) {
      double hLower = (selection.getMinX() - scaledDataArea.getMinX())
          / scaledDataArea.getWidth();
      double hUpper = (selection.getMaxX() - scaledDataArea.getMinX())
          / scaledDataArea.getWidth();
      double vLower = (scaledDataArea.getMaxY() - selection.getMaxY())
          / scaledDataArea.getHeight();
      double vUpper = (scaledDataArea.getMaxY() - selection.getMinY())
          / scaledDataArea.getHeight();

      Plot p = this.chart.getPlot();
      if (p instanceof Zoomable) {
        boolean savedNotify = p.isNotify();
        p.setNotify(false);
        Zoomable z = (Zoomable) p;
        if (z.getOrientation() == PlotOrientation.HORIZONTAL) {
          z.zoomDomainAxes(vLower, vUpper, plotInfo, selectOrigin);
          z.zoomRangeAxes(hLower, hUpper, plotInfo, selectOrigin);
        } else {
          z.zoomDomainAxes(hLower, hUpper, plotInfo, selectOrigin);
          z.zoomRangeAxes(vLower, vUpper, plotInfo, selectOrigin);
        }
        p.setNotify(savedNotify);
      }
    }
  }

  public void restoreAutoBounds() {
    Plot plot = this.chart.getPlot();
    if (plot == null) {
      return;
    }
    boolean savedNotify = plot.isNotify();
    plot.setNotify(false);
    restoreAutoDomainBounds();
    restoreAutoRangeBounds();
    plot.setNotify(savedNotify);
  }

  public void restoreAutoDomainBounds() {
    Plot plot = this.chart.getPlot();
    if (plot instanceof Zoomable) {
      Zoomable z = (Zoomable) plot;
      boolean savedNotify = plot.isNotify();
      plot.setNotify(false);
      Point2D zp = (this.zoomPoint != null ? this.zoomPoint : new Point());
      z.zoomDomainAxes(0.0, this.info.getPlotInfo(), zp);
      plot.setNotify(savedNotify);
    }
  }

  public void restoreAutoRangeBounds() {
    Plot plot = this.chart.getPlot();
    if (plot instanceof Zoomable) {
      Zoomable z = (Zoomable) plot;
      boolean savedNotify = plot.isNotify();
      plot.setNotify(false);
      Point2D zp = (this.zoomPoint != null ? this.zoomPoint : new Point());
      z.zoomRangeAxes(0.0, this.info.getPlotInfo(), zp);
      plot.setNotify(savedNotify);
    }
  }

  public Rectangle2D getScreenDataArea() {
    Rectangle2D dataArea = this.info.getPlotInfo().getDataArea();
    Insets insets = getInsets();
    double x = dataArea.getX() * this.scaleX + insets.left;
    double y = dataArea.getY() * this.scaleY + insets.top;
    double w = dataArea.getWidth() * this.scaleX;
    double h = dataArea.getHeight() * this.scaleY;
    return new Rectangle2D.Double(x, y, w, h);
  }

  public Rectangle2D getScreenDataArea(int x, int y) {
    PlotRenderingInfo plotInfo = this.info.getPlotInfo();
    Rectangle2D result;
    if (plotInfo.getSubplotCount() == 0) {
      result = getScreenDataArea();
    } else {
      Point2D selectOrigin = translateScreenToJava2D(new Point(x, y));
      int subplotIndex = plotInfo.getSubplotIndex(selectOrigin);
      if (subplotIndex == -1) {
        return null;
      }
      result = scale(plotInfo.getSubplotInfo(subplotIndex).getDataArea());
    }
    return result;
  }

  public int getInitialDelay() {
    return this.ownToolTipInitialDelay;
  }

  public int getReshowDelay() {
    return this.ownToolTipReshowDelay;
  }

  public int getDismissDelay() {
    return this.ownToolTipDismissDelay;
  }

  public void setInitialDelay(int delay) {
    this.ownToolTipInitialDelay = delay;
  }

  public void setReshowDelay(int delay) {
    this.ownToolTipReshowDelay = delay;
  }

  public void setDismissDelay(int delay) {
    this.ownToolTipDismissDelay = delay;
  }

  public double getZoomInFactor() {
    return this.zoomInFactor;
  }

  public void setZoomInFactor(double factor) {
    this.zoomInFactor = factor;
  }

  public double getZoomOutFactor() {
    return this.zoomOutFactor;
  }

  public void setZoomOutFactor(double factor) {
    this.zoomOutFactor = factor;
  }

  private void drawZoomRectangle(Graphics2D g2, boolean xor) {
    if (this.zoomRectangle != null) {
      if (xor) {
        g2.setXORMode(Color.GRAY);
      }
      if (this.fillZoomRectangle) {
        g2.setPaint(this.zoomFillPaint);
        g2.fill(this.zoomRectangle);
      } else {
        g2.setPaint(this.zoomOutlinePaint);
        g2.draw(this.zoomRectangle);
      }
      if (xor) {
        g2.setPaintMode();
      }
    }
  }

  private void drawSelectionShape(Graphics2D g2, boolean xor) {
    if (this.selectionShape != null) {
      if (xor) {
        g2.setXORMode(Color.gray);
      }
      if (this.selectionFillPaint != null) {
        g2.setPaint(this.selectionFillPaint);
        g2.fill(this.selectionShape);
      }
      if (this.selectionOutlinePaint != null
          && this.selectionOutlineStroke != null) {
        g2.setPaint(this.selectionOutlinePaint);
        g2.setStroke(this.selectionOutlineStroke);
        GeneralPath pp = new GeneralPath(this.selectionShape);
        pp.closePath();
        g2.draw(pp);
      }

      drawSelectionHandles(g2);

      if (xor) {
        g2.setPaintMode();
      }
    }
  }

  private void drawSelectionHandles(Graphics2D g2) {
    if (this.selectionShape == null) {
      return;
    }

    Rectangle2D bounds = this.selectionShape.getBounds2D();

    Rectangle2D leftHandle = RectangularHeightRegionSelectionHandler.getLeftHandleRect(bounds);
    Rectangle2D rightHandle = RectangularHeightRegionSelectionHandler.getRightHandleRect(bounds);

    g2.setPaint(HANDLE_FILL_PAINT);
    g2.fill(leftHandle);
    g2.fill(rightHandle);

    g2.setPaint(HANDLE_OUTLINE_PAINT);
    g2.setStroke(HANDLE_OUTLINE_STROKE);
    g2.draw(leftHandle);
    g2.draw(rightHandle);
  }

  private void drawHorizontalAxisTrace(Graphics2D g2, int x) {
    Rectangle2D dataArea = getScreenDataArea();
    g2.setXORMode(Color.ORANGE);
    if (((int) dataArea.getMinX() < x) && (x < (int) dataArea.getMaxX())) {
      if (this.verticalTraceLine != null) {
        g2.draw(this.verticalTraceLine);
        this.verticalTraceLine.setLine(x, (int) dataArea.getMinY(), x,
                                       (int) dataArea.getMaxY());
      } else {
        this.verticalTraceLine = new Line2D.Float(x,
                                                  (int) dataArea.getMinY(), x, (int) dataArea.getMaxY());
      }
      g2.draw(this.verticalTraceLine);
    }
    g2.setPaintMode();
  }

  private void drawVerticalAxisTrace(Graphics2D g2, int y) {
    Rectangle2D dataArea = getScreenDataArea();
    g2.setXORMode(Color.ORANGE);
    if (((int) dataArea.getMinY() < y) && (y < (int) dataArea.getMaxY())) {
      if (this.horizontalTraceLine != null) {
        g2.draw(this.horizontalTraceLine);
        this.horizontalTraceLine.setLine((int) dataArea.getMinX(), y,
                                         (int) dataArea.getMaxX(), y);
      } else {
        this.horizontalTraceLine = new Line2D.Float(
            (int) dataArea.getMinX(), y, (int) dataArea.getMaxX(), y);
      }
      g2.draw(this.horizontalTraceLine);
    }
    g2.setPaintMode();
  }

  public void doEditChartProperties() {
    ChartEditor editor = ChartEditorManager.getChartEditor(this.chart);
    int result = JOptionPane.showConfirmDialog(this, editor,
                                               localizationResources.getString("Chart_Properties"),
                                               JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result == JOptionPane.OK_OPTION) {
      editor.updateChart(this.chart);
    }
  }

  public void doCopy() {
    Clipboard systemClipboard
        = Toolkit.getDefaultToolkit().getSystemClipboard();
    Insets insets = getInsets();
    int w = getWidth() - insets.left - insets.right;
    int h = getHeight() - insets.top - insets.bottom;
    ChartTransferable selection = new ChartTransferable(this.chart, w, h,
                                                        getMinimumDrawWidth(), getMinimumDrawHeight(),
                                                        getMaximumDrawWidth(), getMaximumDrawHeight(), true);
    systemClipboard.setContents(selection, null);
  }

  public void doSaveAs() throws IOException {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setCurrentDirectory(this.defaultDirectoryForSaveAs);
    FileNameExtensionFilter filter = new FileNameExtensionFilter(
        localizationResources.getString("PNG_Image_Files"), "png");
    fileChooser.addChoosableFileFilter(filter);
    fileChooser.setFileFilter(filter);

    int option = fileChooser.showSaveDialog(this);
    if (option == JFileChooser.APPROVE_OPTION) {
      String filename = fileChooser.getSelectedFile().getPath();
      if (isEnforceFileExtensions()) {
        if (!filename.endsWith(".png")) {
          filename = filename + ".png";
        }
      }
      ChartUtilities.saveChartAsPNG(new File(filename), this.chart,
                                    getWidth(), getHeight());
    }
  }

  private void saveAsSVG(File f) throws IOException {
    File file = f;
    if (file == null) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setCurrentDirectory(this.defaultDirectoryForSaveAs);
      FileNameExtensionFilter filter = new FileNameExtensionFilter(
          localizationResources.getString("SVG_Files"), "svg");
      fileChooser.addChoosableFileFilter(filter);
      fileChooser.setFileFilter(filter);

      int option = fileChooser.showSaveDialog(this);
      if (option == JFileChooser.APPROVE_OPTION) {
        String filename = fileChooser.getSelectedFile().getPath();
        if (isEnforceFileExtensions()) {
          if (!filename.endsWith(".svg")) {
            filename = filename + ".svg";
          }
        }
        file = new File(filename);
        if (file.exists()) {
          String fileExists = localizationResources.getString(
              "FILE_EXISTS_CONFIRM_OVERWRITE");
          int response = JOptionPane.showConfirmDialog(this,
                                                       fileExists, "Save As SVG",
                                                       JOptionPane.OK_CANCEL_OPTION);
          if (response == JOptionPane.CANCEL_OPTION) {
            file = null;
          }
        }
      }
    }

    if (file != null) {
      String svg = generateSVG(getWidth(), getHeight());
      BufferedWriter writer = null;
      try {
        writer = new BufferedWriter(new FileWriter(file));
        writer.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
        writer.write(svg + "\n");
        writer.flush();
      } finally {
        try {
          if (writer != null) {
            writer.close();
          }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    }
  }

  private String generateSVG(int width, int height) {
    Graphics2D g2 = createSVGGraphics2D(width, height);
    if (g2 == null) {
      throw new IllegalStateException("JFreeSVG library is not present.");
    }
    g2.setRenderingHint(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, true);
    String svg = null;
    Rectangle2D drawArea = new Rectangle2D.Double(0, 0, width, height);
    this.chart.draw(g2, drawArea);
    try {
      Method m = g2.getClass().getMethod("getSVGElement");
      svg = (String) m.invoke(g2);
    } catch (NoSuchMethodException e) {
    } catch (SecurityException e) {
    } catch (IllegalAccessException e) {
    } catch (IllegalArgumentException e) {
    } catch (InvocationTargetException e) {
    }
    return svg;
  }

  private Graphics2D createSVGGraphics2D(int w, int h) {
    try {
      Class svgGraphics2d = Class.forName("org.jfree.graphics2d.svg.SVGGraphics2D");
      Constructor ctor = svgGraphics2d.getConstructor(int.class, int.class);
      return (Graphics2D) ctor.newInstance(w, h);
    } catch (ClassNotFoundException ex) {
      return null;
    } catch (NoSuchMethodException ex) {
      return null;
    } catch (SecurityException ex) {
      return null;
    } catch (InstantiationException ex) {
      return null;
    } catch (IllegalAccessException ex) {
      return null;
    } catch (IllegalArgumentException ex) {
      return null;
    } catch (InvocationTargetException ex) {
      return null;
    }
  }

  private void saveAsPDF(File f) {
    File file = f;
    if (file == null) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setCurrentDirectory(this.defaultDirectoryForSaveAs);
      FileNameExtensionFilter filter = new FileNameExtensionFilter(
          localizationResources.getString("PDF_Files"), "pdf");
      fileChooser.addChoosableFileFilter(filter);
      fileChooser.setFileFilter(filter);

      int option = fileChooser.showSaveDialog(this);
      if (option == JFileChooser.APPROVE_OPTION) {
        String filename = fileChooser.getSelectedFile().getPath();
        if (isEnforceFileExtensions()) {
          if (!filename.endsWith(".pdf")) {
            filename = filename + ".pdf";
          }
        }
        file = new File(filename);
        if (file.exists()) {
          String fileExists = localizationResources.getString(
              "FILE_EXISTS_CONFIRM_OVERWRITE");
          int response = JOptionPane.showConfirmDialog(this,
                                                       fileExists, "Save As PDF",
                                                       JOptionPane.OK_CANCEL_OPTION);
          if (response == JOptionPane.CANCEL_OPTION) {
            file = null;
          }
        }
      }
    }

    if (file != null) {
      writeAsPDF(file, getWidth(), getHeight());
    }
  }

  private boolean isOrsonPDFAvailable() {
    Class pdfDocumentClass = null;
    try {
      pdfDocumentClass = Class.forName("com.orsonpdf.PDFDocument");
    } catch (ClassNotFoundException e) {
    }
    return (pdfDocumentClass != null);
  }

  private void writeAsPDF(File file, int w, int h) {
    if (!isOrsonPDFAvailable()) {
      throw new IllegalStateException(
          "OrsonPDF is not present on the classpath.");
    }
    ParamChecks.nullNotPermitted(file, "file");
    try {
      Class pdfDocClass = Class.forName("com.orsonpdf.PDFDocument");
      Object pdfDoc = pdfDocClass.newInstance();
      Method m = pdfDocClass.getMethod("createPage", Rectangle2D.class);
      Rectangle2D rect = new Rectangle(w, h);
      Object page = m.invoke(pdfDoc, rect);
      Method m2 = page.getClass().getMethod("getGraphics2D");
      Graphics2D g2 = (Graphics2D) m2.invoke(page);
      g2.setRenderingHint(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, true);
      Rectangle2D drawArea = new Rectangle2D.Double(0, 0, w, h);
      this.chart.draw(g2, drawArea);
      Method m3 = pdfDocClass.getMethod("writeToFile", File.class);
      m3.invoke(pdfDoc, file);
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    } catch (InstantiationException ex) {
      throw new RuntimeException(ex);
    } catch (IllegalAccessException ex) {
      throw new RuntimeException(ex);
    } catch (NoSuchMethodException ex) {
      throw new RuntimeException(ex);
    } catch (SecurityException ex) {
      throw new RuntimeException(ex);
    } catch (IllegalArgumentException ex) {
      throw new RuntimeException(ex);
    } catch (InvocationTargetException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void createChartPrintJob() {
    PrinterJob job = PrinterJob.getPrinterJob();
    PageFormat pf = job.defaultPage();
    PageFormat pf2 = job.pageDialog(pf);
    if (pf2 != pf) {
      job.setPrintable(this, pf2);
      if (job.printDialog()) {
        try {
          job.print();
        } catch (PrinterException e) {
          JOptionPane.showMessageDialog(this, e);
        }
      }
    }
  }

  @Override
  public int print(Graphics g, PageFormat pf, int pageIndex) {
    if (pageIndex != 0) {
      return NO_SUCH_PAGE;
    }
    Graphics2D g2 = (Graphics2D) g;
    double x = pf.getImageableX();
    double y = pf.getImageableY();
    double w = pf.getImageableWidth();
    double h = pf.getImageableHeight();
    this.chart.draw(g2, new Rectangle2D.Double(x, y, w, h), this.anchor, null);
    return PAGE_EXISTS;
  }

  public void addChartMouseListener(ChartMouseListener listener) {
    ParamChecks.nullNotPermitted(listener, "listener");
    this.chartMouseListeners.add(ChartMouseListener.class, listener);
  }

  public void removeChartMouseListener(ChartMouseListener listener) {
    this.chartMouseListeners.remove(ChartMouseListener.class, listener);
  }

  @Override
  public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
    if (listenerType == ChartMouseListener.class) {
      return this.chartMouseListeners.getListeners(listenerType);
    }
    return super.getListeners(listenerType);
  }

  protected JPopupMenu createPopupMenu(boolean properties,
                                       boolean save,
                                       boolean print,
                                       boolean zoom) {
    return createPopupMenu(properties, false, save, print, zoom);
  }

  protected JPopupMenu createPopupMenu(boolean properties,
                                       boolean copy,
                                       boolean save,
                                       boolean print,
                                       boolean zoom) {
    JPopupMenu result = new JPopupMenu(localizationResources.getString("Chart") + ":");
    boolean separator = false;

    if (properties) {
      JMenuItem propertiesItem = new JMenuItem(
          localizationResources.getString("Properties..."));
      propertiesItem.setActionCommand(PROPERTIES_COMMAND);
      propertiesItem.addActionListener(this);
      result.add(propertiesItem);
      separator = true;
    }

    if (copy) {
      if (separator) {
        result.addSeparator();
      }
      JMenuItem copyItem = new JMenuItem(
          localizationResources.getString("Copy"));
      copyItem.setActionCommand(COPY_COMMAND);
      copyItem.addActionListener(this);
      result.add(copyItem);
      separator = !save;
    }

    if (save) {
      if (separator) {
        result.addSeparator();
      }
      JMenu saveSubMenu = new JMenu(localizationResources.getString("Save_as"));
      JMenuItem pngItem = new JMenuItem(localizationResources.getString("PNG..."));
      pngItem.setActionCommand("SAVE_AS_PNG");
      pngItem.addActionListener(this);
      saveSubMenu.add(pngItem);

      if (createSVGGraphics2D(10, 10) != null) {
        JMenuItem svgItem = new JMenuItem(localizationResources.getString("SVG..."));
        svgItem.setActionCommand("SAVE_AS_SVG");
        svgItem.addActionListener(this);
        saveSubMenu.add(svgItem);
      }

      if (isOrsonPDFAvailable()) {
        JMenuItem pdfItem = new JMenuItem(
            localizationResources.getString("PDF..."));
        pdfItem.setActionCommand("SAVE_AS_PDF");
        pdfItem.addActionListener(this);
        saveSubMenu.add(pdfItem);
      }
      result.add(saveSubMenu);
      separator = true;
    }

    if (print) {
      if (separator) {
        result.addSeparator();
      }
      JMenuItem printItem = new JMenuItem(
          localizationResources.getString("Print..."));
      printItem.setActionCommand(PRINT_COMMAND);
      printItem.addActionListener(this);
      result.add(printItem);
      separator = true;
    }

    if (zoom) {
      if (separator) {
        result.addSeparator();
      }

      JMenu zoomInMenu = new JMenu(localizationResources.getString("Zoom_In"));

      this.zoomInBothMenuItem = new JMenuItem(
          localizationResources.getString("All_Axes"));
      this.zoomInBothMenuItem.setActionCommand(ZOOM_IN_BOTH_COMMAND);
      this.zoomInBothMenuItem.addActionListener(this);
      zoomInMenu.add(this.zoomInBothMenuItem);

      zoomInMenu.addSeparator();

      this.zoomInDomainMenuItem = new JMenuItem(
          localizationResources.getString("Domain_Axis"));
      this.zoomInDomainMenuItem.setActionCommand(ZOOM_IN_DOMAIN_COMMAND);
      this.zoomInDomainMenuItem.addActionListener(this);
      zoomInMenu.add(this.zoomInDomainMenuItem);

      this.zoomInRangeMenuItem = new JMenuItem(
          localizationResources.getString("Range_Axis"));
      this.zoomInRangeMenuItem.setActionCommand(ZOOM_IN_RANGE_COMMAND);
      this.zoomInRangeMenuItem.addActionListener(this);
      zoomInMenu.add(this.zoomInRangeMenuItem);

      result.add(zoomInMenu);

      JMenu zoomOutMenu = new JMenu(localizationResources.getString("Zoom_Out"));

      this.zoomOutBothMenuItem = new JMenuItem(
          localizationResources.getString("All_Axes"));
      this.zoomOutBothMenuItem.setActionCommand(ZOOM_OUT_BOTH_COMMAND);
      this.zoomOutBothMenuItem.addActionListener(this);
      zoomOutMenu.add(this.zoomOutBothMenuItem);

      zoomOutMenu.addSeparator();

      this.zoomOutDomainMenuItem = new JMenuItem(
          localizationResources.getString("Domain_Axis"));
      this.zoomOutDomainMenuItem.setActionCommand(ZOOM_OUT_DOMAIN_COMMAND);
      this.zoomOutDomainMenuItem.addActionListener(this);
      zoomOutMenu.add(this.zoomOutDomainMenuItem);

      this.zoomOutRangeMenuItem = new JMenuItem(
          localizationResources.getString("Range_Axis"));
      this.zoomOutRangeMenuItem.setActionCommand(ZOOM_OUT_RANGE_COMMAND);
      this.zoomOutRangeMenuItem.addActionListener(this);
      zoomOutMenu.add(this.zoomOutRangeMenuItem);

      result.add(zoomOutMenu);

      JMenu autoRangeMenu = new JMenu(localizationResources.getString("Auto_Range"));

      this.zoomResetBothMenuItem = new JMenuItem(
          localizationResources.getString("All_Axes"));
      this.zoomResetBothMenuItem.setActionCommand(ZOOM_RESET_BOTH_COMMAND);
      this.zoomResetBothMenuItem.addActionListener(this);
      autoRangeMenu.add(this.zoomResetBothMenuItem);

      autoRangeMenu.addSeparator();
      this.zoomResetDomainMenuItem = new JMenuItem(
          localizationResources.getString("Domain_Axis"));
      this.zoomResetDomainMenuItem.setActionCommand(ZOOM_RESET_DOMAIN_COMMAND);
      this.zoomResetDomainMenuItem.addActionListener(this);
      autoRangeMenu.add(this.zoomResetDomainMenuItem);

      this.zoomResetRangeMenuItem = new JMenuItem(
          localizationResources.getString("Range_Axis"));
      this.zoomResetRangeMenuItem.setActionCommand(ZOOM_RESET_RANGE_COMMAND);
      this.zoomResetRangeMenuItem.addActionListener(this);
      autoRangeMenu.add(this.zoomResetRangeMenuItem);

      result.addSeparator();
      result.add(autoRangeMenu);
    }

    return result;
  }

  protected void displayPopupMenu(int x, int y) {
    if (this.popup == null) {
      return;
    }

    boolean isDomainZoomable = false;
    boolean isRangeZoomable = false;
    Plot plot = (this.chart != null ? this.chart.getPlot() : null);
    if (plot instanceof Zoomable) {
      Zoomable z = (Zoomable) plot;
      isDomainZoomable = z.isDomainZoomable();
      isRangeZoomable = z.isRangeZoomable();
    }

    if (this.zoomInDomainMenuItem != null) {
      this.zoomInDomainMenuItem.setEnabled(isDomainZoomable);
    }
    if (this.zoomOutDomainMenuItem != null) {
      this.zoomOutDomainMenuItem.setEnabled(isDomainZoomable);
    }
    if (this.zoomResetDomainMenuItem != null) {
      this.zoomResetDomainMenuItem.setEnabled(isDomainZoomable);
    }
    if (this.zoomInRangeMenuItem != null) {
      this.zoomInRangeMenuItem.setEnabled(isRangeZoomable);
    }
    if (this.zoomOutRangeMenuItem != null) {
      this.zoomOutRangeMenuItem.setEnabled(isRangeZoomable);
    }
    if (this.zoomResetRangeMenuItem != null) {
      this.zoomResetRangeMenuItem.setEnabled(isRangeZoomable);
    }
    if (this.zoomInBothMenuItem != null) {
      this.zoomInBothMenuItem.setEnabled(isDomainZoomable && isRangeZoomable);
    }
    if (this.zoomOutBothMenuItem != null) {
      this.zoomOutBothMenuItem.setEnabled(isDomainZoomable && isRangeZoomable);
    }
    if (this.zoomResetBothMenuItem != null) {
      this.zoomResetBothMenuItem.setEnabled(isDomainZoomable && isRangeZoomable);
    }

    this.popup.show(this, x, y);
  }

  @Override
  public void updateUI() {
    if (this.popup != null) {
      SwingUtilities.updateComponentTreeUI(this.popup);
    }
    super.updateUI();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    SerialUtilities.writePaint(this.zoomFillPaint, stream);
    SerialUtilities.writePaint(this.zoomOutlinePaint, stream);
    SerialUtilities.writeStroke(this.selectionOutlineStroke, stream);
  }

  private void readObject(ObjectInputStream stream)
      throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    this.zoomFillPaint = SerialUtilities.readPaint(stream);
    this.zoomOutlinePaint = SerialUtilities.readPaint(stream);
    this.selectionOutlineStroke = SerialUtilities.readStroke(stream);
    this.chartMouseListeners = new EventListenerList();
    if (this.chart != null) {
      this.chart.addChangeListener(this);
    }
  }

  public boolean getUseBuffer() {
    return this.useBuffer;
  }

  public PlotOrientation getOrientation() {
    return this.orientation;
  }

  public void addMouseHandler(AbstractMouseHandler handler) {
    if (handler.isLiveHandler()) {
      this.availableLiveMouseHandlers.add(handler);
    } else {
      this.auxiliaryMouseHandlers.add(handler);
    }
  }

  public boolean removeMouseHandler(AbstractMouseHandler handler) {
    if (handler.isLiveHandler()) {
      return this.availableLiveMouseHandlers.remove(handler);
    } else {
      return this.auxiliaryMouseHandlers.remove(handler);
    }
  }

  public void clearLiveMouseHandler() {
    this.liveMouseHandler = null;
  }

  public Shape getSelectionShape() {
    return this.selectionShape;
  }

  public void setSelectionShape(Shape shape) {
    this.selectionShape = shape;
  }

  public Paint getSelectionFillPaint() {
    return this.selectionFillPaint;
  }

  public void setSelectionFillPaint(Paint paint) {
    this.selectionFillPaint = paint;
  }

  public void setSelectionOutlinePaint(Paint paint) {
    this.selectionOutlinePaint = paint;
  }

  public void setSelectionOutlineStroke(Stroke stroke) {
    this.selectionOutlineStroke = stroke;
  }

  public Rectangle2D getZoomRectangle() {
    return this.zoomRectangle;
  }

  public void setZoomRectangle(Rectangle2D rect) {
    this.zoomRectangle = rect;
  }

  public ZoomHandler getZoomHandler() {
    return this.zoomHandler;
  }

  public SelectionManager getSelectionManager() {
    return this.selectionManager;
  }

  public void setSelectionManager(SelectionManager manager) {
    this.selectionManager = manager;
  }

  public boolean removeListenerReleaseMouse(IDetailPanel l) {
    return listenersStart.remove(l);
  }

  public void addListenerReleaseMouse(IDetailPanel l) {
    listenersStart.add(l);
  }

  protected void fireReleaseMouse() {
    PlotRenderingInfo plotInfo = this.info.getPlotInfo();

    Rectangle2D scaledDataArea = getScreenDataArea();
    plotInfo.setDataArea(scaledDataArea);

    if (Objects.isNull(getSelectionShape())) {
      System.out.println("NPE log: getSelectionShape is null..");
      return;
    }

    Rectangle2D selection = getSelectionShape().getBounds2D();

    Plot p = this.chart.getPlot();

    if (p instanceof XYPlot z) {
      this.dLeft = z.getJava2dToValue(selection, plotInfo, 0);
      this.dRight = z.getJava2dToValue(selection, plotInfo, 1);
    }

    listenersStart.forEach(currListener ->
                               currListener.loadDataToDetail((long) this.dLeft, (long) this.dRight));
  }

  private final List<IDetailPanel> listenersStart = new ArrayList();

  public void setBackgroundAndTextColor(Color backgroundColor,
                                        Color legendBackgroundColor) {
    if (backgroundColor != null) {
      chart.setBackgroundPaint(backgroundColor);
      chart.getTitle().setPaint(Color.WHITE);
      XYPlot plot = (XYPlot) chart.getPlot();
      plot.getDomainAxis().setLabelPaint(Color.WHITE);
      plot.getRangeAxis().setLabelPaint(Color.WHITE);
      plot.getDomainAxis().setTickLabelPaint(Color.WHITE);
      plot.getRangeAxis().setTickLabelPaint(Color.WHITE);
    }
  }
}