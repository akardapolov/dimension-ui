package ru.dimension.ui.view.action;

import javax.swing.JRadioButton;
import ru.dimension.ui.model.function.MetricFunction;

public class RadioButtonActionExecutor {

  public static void execute(JRadioButton button,
                             MetricFunctionAction action) {
    String buttonText = button.getText();
    MetricFunction metricFunction = MetricFunction.valueOf(buttonText.toUpperCase());
    action.apply(metricFunction);
  }
}

