package ru.dimension.ui.view.table.icon.adhoc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import javax.swing.Icon;
import ru.dimension.ui.model.db.DBType;

/**
 * Icon representing database types.
 * Renders vector symbols for different database vendors in DBeaver style.
 */
public class DBTypeIcon implements Icon {

  private static final int SIZE = 14;
  private final DBType dbType;
  private final Color color;

  // Database vendor colors (matching official brand colors)
  private static final Color ORACLE_COLOR = new Color(0xCF5F5F);      // Oracle Red
  private static final Color POSTGRES_COLOR = new Color(0x336791);    // PostgreSQL Blue
  private static final Color MSSQL_COLOR = new Color(0xCC2927);       // SQL Server Red
  private static final Color CLICKHOUSE_COLOR = new Color(0xFFCC00);  // ClickHouse Yellow
  private static final Color MYSQL_COLOR = new Color(0x00758F);       // MySQL Teal
  private static final Color DUCKDB_COLOR = new Color(0xFFC107);      // DuckDB Amber
  private static final Color FIREBIRD_COLOR = new Color(0xF4511E);    // Firebird Orange-Red
  private static final Color HTTP_COLOR = new Color(0x4CAF50);        // HTTP Green
  private static final Color UNKNOWN_COLOR = Color.GRAY;

  private static final Font ICON_FONT = new Font("SansSerif", Font.BOLD, 9);

  public DBTypeIcon(DBType dbType) {
    this.dbType = dbType;
    this.color = getColorForDBType(dbType);
  }

  private static Color getColorForDBType(DBType dbType) {
    if (dbType == null) return UNKNOWN_COLOR;
    return switch (dbType) {
      case ORACLE -> ORACLE_COLOR;
      case POSTGRES -> POSTGRES_COLOR;
      case MSSQL -> MSSQL_COLOR;
      case CLICKHOUSE -> CLICKHOUSE_COLOR;
      case MYSQL -> MYSQL_COLOR;
      case DUCKDB -> DUCKDB_COLOR;
      case FIREBIRD -> FIREBIRD_COLOR;
      case HTTP -> HTTP_COLOR;
      case UNKNOWN -> UNKNOWN_COLOR;
    };
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    g2.setColor(color);
    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    if (dbType == null) {
      drawUnknown(g2, x, y);
      g2.dispose();
      return;
    }

    switch (dbType) {
      case ORACLE -> drawOracle(g2, x, y);
      case POSTGRES -> drawPostgres(g2, x, y);
      case MSSQL -> drawMSSQL(g2, x, y);
      case CLICKHOUSE -> drawClickHouse(g2, x, y);
      case MYSQL -> drawMySQL(g2, x, y);
      case DUCKDB -> drawDuckDB(g2, x, y);
      case FIREBIRD -> drawFirebird(g2, x, y);
      case HTTP -> drawHTTP(g2, x, y);
      case UNKNOWN -> drawUnknown(g2, x, y);
    }

    g2.dispose();
  }

  private void drawOracle(Graphics2D g2, int x, int y) {
    // Oracle: Modified to look like a "0" rotated 90 degrees (Horizontal Ellipse)
    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(new Ellipse2D.Double(x + 1, y + 3, SIZE - 2, SIZE - 6));
  }

  private void drawPostgres(Graphics2D g2, int x, int y) {
    // PostgreSQL: Elephant head silhouette
    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Elephant head shape
    Path2D elephant = new Path2D.Double();
    elephant.moveTo(x + 3, y + 11);
    elephant.curveTo(x + 1, y + 8, x + 2, y + 4, x + 5, y + 2);
    elephant.curveTo(x + 8, y + 1, x + 11, y + 2, x + 12, y + 5);
    elephant.curveTo(x + 13, y + 8, x + 12, y + 11, x + 10, y + 12);
    g2.draw(elephant);

    // Trunk curl
    Path2D trunk = new Path2D.Double();
    trunk.moveTo(x + 3, y + 11);
    trunk.curveTo(x + 2, y + 13, x + 4, y + 13, x + 5, y + 11);
    g2.draw(trunk);

    // Eye
    g2.fillOval(x + 8, y + 5, 2, 2);

    // Ear
    g2.draw(new Arc2D.Double(x + 9, y + 2, 3, 4, 270, 180, Arc2D.OPEN));
  }

  private void drawMSSQL(Graphics2D g2, int x, int y) {
    // SQL Server: 3D Cylinder/Database shape
    g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Top ellipse (filled slightly)
    g2.draw(new Ellipse2D.Double(x + 2, y + 1, SIZE - 4, 4));

    // Sides
    g2.draw(new Line2D.Double(x + 2, y + 3, x + 2, y + 10));
    g2.draw(new Line2D.Double(x + SIZE - 2, y + 3, x + SIZE - 2, y + 10));

    // Middle ellipse hint
    g2.setStroke(new BasicStroke(0.8f));
    g2.draw(new Arc2D.Double(x + 2, y + 5, SIZE - 4, 3, 180, 180, Arc2D.OPEN));

    // Bottom ellipse (arc only)
    g2.setStroke(new BasicStroke(1.3f));
    g2.draw(new Arc2D.Double(x + 2, y + 8, SIZE - 4, 4, 180, 180, Arc2D.OPEN));
  }

  private void drawClickHouse(Graphics2D g2, int x, int y) {
    // ClickHouse: Ascending bar chart (columnar storage symbol)
    // Bars representing fast columnar data
    g2.fillRect(x + 1, y + 9, 2, 4);
    g2.fillRect(x + 4, y + 6, 2, 7);
    g2.fillRect(x + 7, y + 3, 2, 10);
    g2.fillRect(x + 10, y + 1, 2, 12);

    // Speed indicator line
    g2.setStroke(new BasicStroke(1.0f));
    g2.setColor(new Color(0xFFAA00));
    g2.draw(new Line2D.Double(x + 2, y + 8, x + 11, y + 2));
  }

  private void drawMySQL(Graphics2D g2, int x, int y) {
    // MySQL: Dolphin jumping over waves
    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Dolphin body arc
    Path2D dolphin = new Path2D.Double();
    dolphin.moveTo(x + 2, y + 9);
    dolphin.curveTo(x + 3, y + 5, x + 7, y + 2, x + 11, y + 4);
    dolphin.curveTo(x + 13, y + 6, x + 12, y + 9, x + 10, y + 10);
    g2.draw(dolphin);

    // Tail
    Path2D tail = new Path2D.Double();
    tail.moveTo(x + 2, y + 9);
    tail.curveTo(x + 1, y + 7, x + 1, y + 11, x + 3, y + 11);
    g2.draw(tail);

    // Dorsal fin
    Path2D fin = new Path2D.Double();
    fin.moveTo(x + 7, y + 4);
    fin.lineTo(x + 8, y + 1);
    fin.lineTo(x + 9, y + 4);
    g2.draw(fin);

    // Eye
    g2.fillOval(x + 9, y + 5, 1, 1);
  }

  private void drawDuckDB(Graphics2D g2, int x, int y) {
    // DuckDB: Cute duck head profile
    g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Head circle
    g2.draw(new Ellipse2D.Double(x + 4, y + 2, 8, 8));

    // Beak (orange-ish for duck)
    g2.setColor(new Color(0xFF9800));
    Path2D beak = new Path2D.Double();
    beak.moveTo(x + 1, y + 6);
    beak.lineTo(x + 4, y + 5);
    beak.lineTo(x + 4, y + 8);
    beak.closePath();
    g2.fill(beak);

    // Reset color for eye and body
    g2.setColor(color);

    // Eye
    g2.fillOval(x + 8, y + 4, 2, 2);

    // Body hint (water line)
    g2.setStroke(new BasicStroke(1.0f));
    g2.draw(new Arc2D.Double(x + 5, y + 9, 7, 4, 180, 180, Arc2D.OPEN));
  }

  private void drawFirebird(Graphics2D g2, int x, int y) {
    // Firebird: Phoenix/flame bird
    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Flame/bird body
    Path2D flame = new Path2D.Double();
    flame.moveTo(x + 7, y + 1);
    flame.curveTo(x + 10, y + 2, x + 12, y + 5, x + 11, y + 8);
    flame.curveTo(x + 13, y + 6, x + 13, y + 10, x + 10, y + 12);
    flame.lineTo(x + 7, y + 13);
    flame.lineTo(x + 4, y + 12);
    flame.curveTo(x + 1, y + 10, x + 1, y + 6, x + 3, y + 8);
    flame.curveTo(x + 2, y + 5, x + 4, y + 2, x + 7, y + 1);
    flame.closePath();
    g2.fill(flame);

    // Inner flame highlight
    g2.setColor(new Color(0xFFCC80));
    Path2D innerFlame = new Path2D.Double();
    innerFlame.moveTo(x + 7, y + 4);
    innerFlame.curveTo(x + 9, y + 5, x + 9, y + 8, x + 7, y + 10);
    innerFlame.curveTo(x + 5, y + 8, x + 5, y + 5, x + 7, y + 4);
    g2.fill(innerFlame);
  }

  private void drawHTTP(Graphics2D g2, int x, int y) {
    // HTTP: Globe with network lines
    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Outer globe circle
    g2.draw(new Ellipse2D.Double(x + 1, y + 1, SIZE - 2, SIZE - 2));

    // Vertical meridian (center ellipse)
    g2.draw(new Ellipse2D.Double(x + 4, y + 1, SIZE - 8, SIZE - 2));

    // Horizontal equator
    g2.draw(new Line2D.Double(x + 1, y + 7, x + SIZE - 1, y + 7));

    // Latitude lines
    g2.setStroke(new BasicStroke(0.8f));
    g2.draw(new Arc2D.Double(x + 1, y + 2, SIZE - 2, 5, 180, 180, Arc2D.OPEN));
    g2.draw(new Arc2D.Double(x + 1, y + 7, SIZE - 2, 5, 0, 180, Arc2D.OPEN));
  }

  private void drawUnknown(Graphics2D g2, int x, int y) {
    // Unknown: Database cylinder with question mark
    g2.setStroke(new BasicStroke(1.2f));

    // Simple cylinder
    g2.draw(new Ellipse2D.Double(x + 3, y + 1, SIZE - 6, 3));
    g2.draw(new Line2D.Double(x + 3, y + 2.5, x + 3, y + 10));
    g2.draw(new Line2D.Double(x + SIZE - 3, y + 2.5, x + SIZE - 3, y + 10));
    g2.draw(new Arc2D.Double(x + 3, y + 8.5, SIZE - 6, 3, 180, 180, Arc2D.OPEN));

    // Question mark
    g2.setFont(ICON_FONT);
    g2.drawString("?", x + 5, y + 10);
  }

  @Override
  public int getIconWidth() { return SIZE; }

  @Override
  public int getIconHeight() { return SIZE; }

  public DBType getDbType() { return dbType; }
  public Color getColor() { return color; }
}