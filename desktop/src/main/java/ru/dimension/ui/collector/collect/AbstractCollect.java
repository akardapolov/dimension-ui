package ru.dimension.ui.collector.collect;

public abstract class AbstractCollect {

  protected static final int resultSetFetchSize = 10000;

  public abstract void collect();

  public abstract String getProtocol();
}
