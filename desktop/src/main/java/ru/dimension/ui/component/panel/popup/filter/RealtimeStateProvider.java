package ru.dimension.ui.component.panel.popup.filter;

import java.awt.Color;
import java.util.Map;

public interface RealtimeStateProvider {
  long provideCurrentBegin();
  long provideCurrentEnd();
  Map<String, Color> provideCurrentSeriesColorMap();
}