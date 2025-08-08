package ru.dimension.ui.component.chart.holder;

import ru.dimension.ui.component.module.analyze.CustomAction;
import ru.dimension.ui.component.module.analyze.DetailAction;

public record DetailAndAnalyzeHolder(DetailAction detailAction, CustomAction customAction) {}