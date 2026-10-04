/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2014, by Object Refinery Limited and Contributors.
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
 * ----------------
 * LegendTitle.java
 * ----------------
 * (C) Copyright 2002-2014, by Object Refinery Limited.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   Pierre-Marie Le Biot;
 *
 * Changes
 * -------
 * 25-Nov-2004 : First working version (DG);
 * 11-Jan-2005 : Removed deprecated code in preparation for 1.0.0 release (DG);
 * 08-Feb-2005 : Updated for changes in RectangleConstraint class (DG);
 * 11-Feb-2005 : Implemented PublicCloneable (DG);
 * 23-Feb-2005 : Replaced chart reference with LegendItemSource (DG);
 * 16-Mar-2005 : Added itemFont attribute (DG);
 * 17-Mar-2005 : Fixed missing fillShape setting (DG);
 * 20-Apr-2005 : Added new draw() method (DG);
 * 03-May-2005 : Modified equals() method to ignore sources (DG);
 * 13-May-2005 : Added settings for legend item label and graphic padding (DG);
 * 09-Jun-2005 : Fixed serialization bug (DG);
 * 01-Sep-2005 : Added itemPaint attribute (PMLB);
 * ------------- JFREECHART 1.0.x ---------------------------------------------
 * 20-Jul-2006 : Use new LegendItemBlockContainer to restore support for
 *               LegendItemEntities (DG);
 * 06-Oct-2006 : Add tooltip and URL text to legend item (DG);
 * 13-Dec-2006 : Added support for GradientPaint in legend items (DG);
 * 16-Mar-2007 : Updated border drawing for changes in AbstractBlock (DG);
 * 18-May-2007 : Pass seriesKey and dataset to legend item block (DG);
 * 15-Aug-2008 : Added getWrapper() method (DG);
 * 19-Mar-2009 : Added entity support - see patch 2603321 by Peter Kolb (DG);
 * 11-Mar-2012 : Added sort-order support - patch 3500621 by Simon Kaczor (MH);
 * 17-Jun-2012 : Removed JCommon dependencies (DG);
 * 10-Mar-2014 : Removed LegendItemCollection (DG);
 *
 */

package org.jfree.chart.title;

import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.block.*;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.entity.TitleEntity;
import org.jfree.chart.event.TitleChangeEvent;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.util.PaintUtilities;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.chart.util.SerialUtilities;
import org.jfree.chart.util.SortOrder;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

public class LegendTitle extends Title
        implements Cloneable, PublicCloneable, Serializable {

    private static final long serialVersionUID = 2644010518533854633L;

    public static final Font DEFAULT_ITEM_FONT
            = new Font("SansSerif", Font.PLAIN, 12);

    public static final Paint DEFAULT_ITEM_PAINT = Color.BLACK;

    private LegendItemSource[] sources;

    private transient Paint backgroundPaint;

    private RectangleEdge legendItemGraphicEdge;

    private RectangleAnchor legendItemGraphicAnchor;

    private RectangleAnchor legendItemGraphicLocation;

    private RectangleInsets legendItemGraphicPadding;

    private Font itemFont;

    private transient Paint itemPaint;

    private RectangleInsets itemLabelPadding;

    private BlockContainer items;

    private Arrangement hLayout;

    private Arrangement vLayout;

    private BlockContainer wrapper;

    private SortOrder sortOrder;

    public LegendTitle(LegendItemSource source) {
        this(source, new FlowArrangement(), new ColumnArrangement());
    }

    public LegendTitle(LegendItemSource source,
                       Arrangement hLayout, Arrangement vLayout) {
        this.sources = new LegendItemSource[] {source};
        this.items = new BlockContainer(hLayout);
        this.hLayout = hLayout;
        this.vLayout = vLayout;
        this.backgroundPaint = null;
        this.legendItemGraphicEdge = RectangleEdge.LEFT;
        this.legendItemGraphicAnchor = RectangleAnchor.CENTER;
        this.legendItemGraphicLocation = RectangleAnchor.CENTER;
        this.legendItemGraphicPadding = new RectangleInsets(2.0, 2.0, 2.0, 2.0);
        this.itemFont = DEFAULT_ITEM_FONT;
        this.itemPaint = DEFAULT_ITEM_PAINT;
        this.itemLabelPadding = new RectangleInsets(2.0, 2.0, 2.0, 2.0);
        this.sortOrder = SortOrder.ASCENDING;
    }

    public LegendItemSource[] getSources() {
        return this.sources;
    }

    public void setSources(LegendItemSource[] sources) {
        if (sources == null) {
            throw new IllegalArgumentException("Null 'sources' argument.");
        }
        this.sources = sources;
        notifyListeners(new TitleChangeEvent(this));
    }

    public Paint getBackgroundPaint() {
        return this.backgroundPaint;
    }

    public void setBackgroundPaint(Paint paint) {
        this.backgroundPaint = paint;
        notifyListeners(new TitleChangeEvent(this));
    }

    public RectangleEdge getLegendItemGraphicEdge() {
        return this.legendItemGraphicEdge;
    }

    public void setLegendItemGraphicEdge(RectangleEdge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("Null 'edge' argument.");
        }
        this.legendItemGraphicEdge = edge;
        notifyListeners(new TitleChangeEvent(this));
    }

    public RectangleAnchor getLegendItemGraphicAnchor() {
        return this.legendItemGraphicAnchor;
    }

    public void setLegendItemGraphicAnchor(RectangleAnchor anchor) {
        if (anchor == null) {
            throw new IllegalArgumentException("Null 'anchor' point.");
        }
        this.legendItemGraphicAnchor = anchor;
    }

    public RectangleAnchor getLegendItemGraphicLocation() {
        return this.legendItemGraphicLocation;
    }

    public void setLegendItemGraphicLocation(RectangleAnchor anchor) {
        this.legendItemGraphicLocation = anchor;
    }

    public RectangleInsets getLegendItemGraphicPadding() {
        return this.legendItemGraphicPadding;
    }

    public void setLegendItemGraphicPadding(RectangleInsets padding) {
        if (padding == null) {
            throw new IllegalArgumentException("Null 'padding' argument.");
        }
        this.legendItemGraphicPadding = padding;
        notifyListeners(new TitleChangeEvent(this));
    }

    public Font getItemFont() {
        return this.itemFont;
    }

    public void setItemFont(Font font) {
        if (font == null) {
            throw new IllegalArgumentException("Null 'font' argument.");
        }
        this.itemFont = font;
        notifyListeners(new TitleChangeEvent(this));
    }

    public Paint getItemPaint() {
        return this.itemPaint;
    }

    public void setItemPaint(Paint paint) {
        if (paint == null) {
            throw new IllegalArgumentException("Null 'paint' argument.");
        }
        this.itemPaint = paint;
        notifyListeners(new TitleChangeEvent(this));
    }

    public RectangleInsets getItemLabelPadding() {
        return this.itemLabelPadding;
    }

    public void setItemLabelPadding(RectangleInsets padding) {
        if (padding == null) {
            throw new IllegalArgumentException("Null 'padding' argument.");
        }
        this.itemLabelPadding = padding;
        notifyListeners(new TitleChangeEvent(this));
    }

    public SortOrder getSortOrder() {
        return this.sortOrder;
    }

    public void setSortOrder(SortOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Null 'order' argument.");
        }
        this.sortOrder = order;
        notifyListeners(new TitleChangeEvent(this));
    }

    protected void fetchLegendItems() {
        this.items.clear();
        RectangleEdge p = getPosition();
        if (RectangleEdge.isTopOrBottom(p)) {
            this.items.setArrangement(this.hLayout);
        }
        else {
            this.items.setArrangement(this.vLayout);
        }

        if (this.sortOrder.equals(SortOrder.ASCENDING)) {
            for (LegendItemSource source : this.sources) {
                List<LegendItem> legendItems = source.getLegendItems();
                for (LegendItem item: legendItems) {
                    addItemBlock(item);
                }
            }
        }
        else {
            for (int s = this.sources.length - 1; s >= 0; s--) {
                List<LegendItem> legendItems = this.sources[s].getLegendItems();
                for (int i = legendItems.size() - 1; i >= 0; i--) {
                    addItemBlock(legendItems.get(i));
                }
            }
        }
    }

    private void addItemBlock(LegendItem item) {
        Block block = createLegendItemBlock(item);
        this.items.add(block);
    }

    protected Block createLegendItemBlock(LegendItem item) {
        BlockContainer result;
        LegendGraphic lg = new LegendGraphic(item.getShape(),
                item.getFillPaint());
        lg.setFillPaintTransformer(item.getFillPaintTransformer());
        lg.setShapeFilled(item.isShapeFilled());
        lg.setLine(item.getLine());
        lg.setLineStroke(item.getLineStroke());
        lg.setLinePaint(item.getLinePaint());
        lg.setLineVisible(item.isLineVisible());
        lg.setShapeVisible(item.isShapeVisible());
        lg.setShapeOutlineVisible(item.isShapeOutlineVisible());
        lg.setOutlinePaint(item.getOutlinePaint());
        lg.setOutlineStroke(item.getOutlineStroke());
        lg.setPadding(this.legendItemGraphicPadding);

        LegendItemBlockContainer legendItem = new LegendItemBlockContainer(
                new BorderArrangement(), item.getDataset(),
                item.getSeriesKey());
        lg.setShapeAnchor(getLegendItemGraphicAnchor());
        lg.setShapeLocation(getLegendItemGraphicLocation());
        legendItem.add(lg, this.legendItemGraphicEdge);
        Font textFont = item.getLabelFont();
        if (textFont == null) {
            textFont = this.itemFont;
        }
        Paint textPaint = item.getLabelPaint();
        if (textPaint == null) {
            textPaint = this.itemPaint;
        }
        LabelBlock labelBlock = new LabelBlock(item.getLabel(), textFont,
                textPaint);
        labelBlock.setPadding(this.itemLabelPadding);
        legendItem.add(labelBlock);
        legendItem.setToolTipText(item.getToolTipText());
        legendItem.setURLText(item.getURLText());

        result = new BlockContainer(new CenterArrangement());
        result.add(legendItem);

        return result;
    }

    public BlockContainer getItemContainer() {
        return this.items;
    }

    @Override
    public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
        Size2D result = new Size2D();
        fetchLegendItems();
        if (this.items.isEmpty()) {
            return result;
        }
        BlockContainer container = this.wrapper;
        if (container == null) {
            container = this.items;
        }
        RectangleConstraint c = toContentConstraint(constraint);
        Size2D size = container.arrange(g2, c);
        result.height = calculateTotalHeight(size.height);
        result.width = calculateTotalWidth(size.width);
        return result;
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area) {
        draw(g2, area, null);
    }

    @Override
    public Object draw(Graphics2D g2, Rectangle2D area, Object params) {
        Rectangle2D target = (Rectangle2D) area.clone();
        Rectangle2D hotspot = (Rectangle2D) area.clone();
        StandardEntityCollection sec = null;
        if (params instanceof EntityBlockParams
                && ((EntityBlockParams) params).getGenerateEntities()) {
            sec = new StandardEntityCollection();
            sec.add(new TitleEntity(hotspot, this));
        }
        target = trimMargin(target);
        if (this.backgroundPaint != null) {
            g2.setPaint(this.backgroundPaint);
            g2.fill(target);
        }
        BlockFrame border = getFrame();
        border.draw(g2, target);
        border.getInsets().trim(target);
        BlockContainer container = this.wrapper;
        if (container == null) {
            container = this.items;
        }
        target = trimPadding(target);
        Object val = container.draw(g2, target, params);
        if (val instanceof BlockResult) {
            EntityCollection ec = ((BlockResult) val).getEntityCollection();
            if (ec != null && sec != null) {
                sec.addAll(ec);
                ((BlockResult) val).setEntityCollection(sec);
            }
        }
        return val;
    }

    public BlockContainer getWrapper() {
        return this.wrapper;
    }

    public void setWrapper(BlockContainer wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LegendTitle)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        LegendTitle that = (LegendTitle) obj;
        if (!PaintUtilities.equal(this.backgroundPaint, that.backgroundPaint)) {
            return false;
        }
        if (this.legendItemGraphicEdge != that.legendItemGraphicEdge) {
            return false;
        }
        if (this.legendItemGraphicAnchor != that.legendItemGraphicAnchor) {
            return false;
        }
        if (this.legendItemGraphicLocation != that.legendItemGraphicLocation) {
            return false;
        }
        if (!this.itemFont.equals(that.itemFont)) {
            return false;
        }
        if (!this.itemPaint.equals(that.itemPaint)) {
            return false;
        }
        if (!this.hLayout.equals(that.hLayout)) {
            return false;
        }
        if (!this.vLayout.equals(that.vLayout)) {
            return false;
        }
        if (!this.sortOrder.equals(that.sortOrder)) {
            return false;
        }
        return true;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        SerialUtilities.writePaint(this.backgroundPaint, stream);
        SerialUtilities.writePaint(this.itemPaint, stream);
    }

    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.backgroundPaint = SerialUtilities.readPaint(stream);
        this.itemPaint = SerialUtilities.readPaint(stream);
    }

}