package ru.dimension.ui.component.broker;

import java.util.HashMap;
import java.util.Map;

public class ParameterStore {
  private final Map<String, Object> params = new HashMap<>();

  public <T> void put(String key, T value) {
    params.put(key, value);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    return (T) params.get(key);
  }

  public <T> T get(String key, Class<T> type) {
    Object value = params.get(key);
    return type.isInstance(value) ? type.cast(value) : null;
  }

  public Map<String, Object> toMap() {
    return new HashMap<>(params);
  }
}
