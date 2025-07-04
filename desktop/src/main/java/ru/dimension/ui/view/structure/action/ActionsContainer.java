package ru.dimension.ui.view.structure.action;

import java.util.HashMap;
import java.util.Map;
import javax.swing.KeyStroke;
import lombok.Data;

@Data
public class ActionsContainer {

  private Map<String, KeyStroke> keyStrokeMap = new HashMap<>();
}
