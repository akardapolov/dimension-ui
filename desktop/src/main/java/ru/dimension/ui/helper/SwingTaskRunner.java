package ru.dimension.ui.helper;

import javax.swing.*;
import java.awt.Container;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SwingTaskRunner {
  public static void runWithProgress(
      Container container,
      Executor executor,
      Callable<Runnable> backgroundTask,
      Consumer<Exception> errorHandler,
      Supplier<JComponent> progressComponentSupplier) {

    runWithProgress(
        container,
        executor,
        backgroundTask,
        errorHandler,
        progressComponentSupplier,
        null
    );
  }

  public static void runWithProgress(
      Container container,
      Executor executor,
      Callable<Runnable> backgroundTask,
      Consumer<Exception> errorHandler,
      Supplier<JComponent> progressComponentSupplier,
      Runnable onComplete) {

    SwingUtilities.invokeLater(() -> showProgress(container, progressComponentSupplier));

    executor.execute(() -> executeBackgroundTask(
        container, backgroundTask, errorHandler, onComplete
    ));
  }

  private static void showProgress(
      Container container,
      Supplier<JComponent> progressComponentSupplier) {
    container.removeAll();
    container.add(progressComponentSupplier.get());
    container.revalidate();
    container.repaint();
  }

  private static void executeBackgroundTask(
      Container container,
      Callable<Runnable> backgroundTask,
      Consumer<Exception> errorHandler,
      Runnable onComplete) {
    try {
      Runnable uiUpdate = backgroundTask.call();

      SwingUtilities.invokeLater(() -> {
        updateUI(container, uiUpdate);
        if (onComplete != null) {
          onComplete.run();
        }
      });
    } catch (Exception e) {
      errorHandler.accept(e);
      if (onComplete != null) {
        SwingUtilities.invokeLater(onComplete);
      }
    }
  }

  private static void updateUI(Container container, Runnable uiUpdate) {
    container.removeAll();
    uiUpdate.run();
    container.revalidate();
    container.repaint();
  }
}