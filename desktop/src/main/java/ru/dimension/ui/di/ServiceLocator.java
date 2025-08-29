package ru.dimension.ui.di;

import java.util.HashMap;
import java.util.Map;

public class ServiceLocator {
  private static final Map<Class<?>, Object> services = new HashMap<>();
  private static final Map<String, Object> namedServices = new HashMap<>();

  public static <T> void register(Class<T> type, T instance) {
    services.put(type, instance);
  }

  public static <T> void register(String name, T instance) {
    namedServices.put(name, instance);
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(Class<T> type) {
    return (T) services.get(type);
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(String name) {
    return (T) namedServices.get(name);
  }

  public static void clear() {
    services.clear();
    namedServices.clear();
  }
}