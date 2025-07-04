package ru.dimension.ui.helper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AsyncHelper {

  public static <T> CompletableFuture<T> executeAsync(Supplier<T> operation,
                                                      Consumer<Exception> errorHandler,
                                                      Runnable onSuccess) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
        try {
          return operation.get();
        } catch (Exception ex) {
          errorHandler.accept(ex);
          throw new RuntimeException(ex);
        }
      }, executor).exceptionally(exception -> {
        Throwable cause = exception.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        } else if (cause instanceof TimeoutException) {
          log.info("Task execution timed out");
          throw new RuntimeException(cause);
        } else {
          throw new RuntimeException(cause);
        }
      }).orTimeout(5, TimeUnit.SECONDS);

      future.thenRun(onSuccess);

      return future;
    }
  }
}
