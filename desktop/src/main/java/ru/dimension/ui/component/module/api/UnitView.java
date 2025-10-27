package ru.dimension.ui.component.module.api;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public interface UnitView {
  Component getRootComponent();
  JPanel getConfigPanel();
  JPanel getChartPanel();
  Optional<JPanel> getDetailPanel();
  void setDetailVisible(boolean visible);
}