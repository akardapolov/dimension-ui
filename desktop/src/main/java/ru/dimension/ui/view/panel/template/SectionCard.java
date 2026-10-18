package ru.dimension.ui.view.panel.template;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;

public class SectionCard extends JPanel {

  public static final Color STRIPE_COLOR = new Color(0x42, 0x85, 0xF4);

  public SectionCard(String title, JComponent content) {
    this(title, content, STRIPE_COLOR);
  }

  public SectionCard(String title, JComponent content, Color stripeColor) {
    super(new BorderLayout(8, 4));

    Color outlineColor = LaF.getColorBorder(LafColorGroup.BORDER);

    Border outline = BorderFactory.createLineBorder(outlineColor, 1);
    Border stripe = BorderFactory.createMatteBorder(0, 4, 0, 0, stripeColor);
    Border padding = BorderFactory.createEmptyBorder(8, 10, 8, 10);

    Border outlineWithStripe = BorderFactory.createCompoundBorder(outline, stripe);
    Border fullBorder = BorderFactory.createCompoundBorder(outlineWithStripe, padding);
    setBorder(fullBorder);

    LaF.setBackgroundColor(LafColorGroup.TEMPLATE_CARD, this);

    JLabel titleLabel = new JLabel(title.toUpperCase());
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
    titleLabel.setForeground(LaF.getBackgroundColor(LafColorGroup.TEMPLATE_CARD_TITLE, LaF.getLafType()));

    add(titleLabel, BorderLayout.NORTH);
    add(content, BorderLayout.CENTER);
  }
}