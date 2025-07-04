package ru.dimension.ui.component.module.model;

@FunctionalInterface
public interface ModelHandler<T> {

  void handle(T item, boolean add);
}
