package ru.dimension.ui.view.action;

import ru.dimension.ui.model.function.MetricFunction;

@FunctionalInterface
public interface MetricFunctionAction {

  void apply(MetricFunction metricFunction);
}

