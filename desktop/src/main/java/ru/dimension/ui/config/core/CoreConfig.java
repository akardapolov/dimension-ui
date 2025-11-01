package ru.dimension.ui.config.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import ru.dimension.db.core.DStore;
import ru.dimension.di.DimensionDI;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.cache.AppCache;
import ru.dimension.ui.cache.impl.AppCacheImpl;
import ru.dimension.ui.collector.Collector;
import ru.dimension.ui.collector.CollectorImpl;
import ru.dimension.ui.collector.collect.prometheus.ExporterParser;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.collector.http.HttpResponseFetcherImpl;
import ru.dimension.ui.helper.ColorHelper;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.helper.ReportHelper;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.warehouse.LocalDB;

public final class CoreConfig {

  private CoreConfig() {
  }

  public static void configure(DimensionDI.Builder builder) {
    builder
        // Cache
        .bindNamed(AppCache.class, "appCache", AppCacheImpl.class)

        // Collector
        .bindNamed(Collector.class, "collector", CollectorImpl.class)

        // Executors
        .provideNamed(ScheduledExecutorService.class, "executorService", ServiceLocator.singleton(
            () -> Executors.newScheduledThreadPool(10)
        ))
        .provideNamed(ru.dimension.ui.executor.TaskExecutorPool.class, "taskExecutorPool",
                      ServiceLocator.singleton(ru.dimension.ui.executor.TaskExecutorPool::new))

        // File/Gson/Report helpers
        .provide(FilesHelper.class, ServiceLocator.singleton(
            () -> new FilesHelper(Paths.get(".").toAbsolutePath().normalize().toString())
        ))
        .provide(Gson.class, ServiceLocator.singleton(
            () -> new GsonBuilder().setPrettyPrinting().create()
        ))
        .provide(ReportHelper.class, ServiceLocator.singleton(ReportHelper::new))

        // Color helper (uses FilesHelper and ConfigurationManager)
        .provide(ColorHelper.class, ServiceLocator.singleton(
            () -> new ColorHelper(
                ServiceLocator.get(FilesHelper.class),
                ServiceLocator.get(ConfigurationManager.class, "configurationManager"))
        ))

        // Local DB
        .bindNamed(DStore.class, "localDB", LocalDB.class)

        // HTTP / Parser
        .provideNamed(ExporterParser.class, "exporterParser", () -> ServiceLocator.get(ExporterParser.class))
        .bindNamed(HttpResponseFetcher.class, "httpResponseFetcher", HttpResponseFetcherImpl.class);
  }
}