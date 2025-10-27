package ru.dimension.ui.component.block.base;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import lombok.Builder;
import lombok.Getter;
import ru.dimension.ui.laf.LaF;

public abstract class AbstractConfigBlock extends JPanel {

  @Getter
  protected final JPanel extraRightContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));

  @Builder
  public static class Item {
    public Component component;
    public double weightx;
  }

  @Builder
  public static class Spec {
    public List<Item> leftItems;
    public List<Component> rightItems;
    public Component trailing;
  }

  protected AbstractConfigBlock(Spec spec) {
    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);
    setLayout(new GridBagLayout());
    build(spec);
  }

  private void build(Spec spec) {
    LaF.setBackgroundConfigPanel(CHART_PANEL, extraRightContainer);
    if (spec.rightItems != null) {
      spec.rightItems.forEach(extraRightContainer::add);
    }

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(2, 4, 2, 4);

    int gridx = 0;

    if (spec.leftItems != null) {
      for (Item item : spec.leftItems) {
        gbc.gridx = gridx++;
        gbc.weightx = item.weightx;
        add(item.component, gbc);
      }
    }

    gbc.gridx = gridx++;
    gbc.weightx = 0;
    add(extraRightContainer, gbc);

    if (spec.trailing != null) {
      gbc.gridx = gridx++;
      gbc.weightx = 0;
      add(spec.trailing, gbc);
    }

    gbc.gridx = gridx;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    add(new JLabel(), gbc);
  }
}