package ru.dimension.ui.config;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.ui.collector.collect.prometheus.ExporterParser;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.collector.http.HttpResponseFetcherImpl;

@Module
public abstract class ParserHttpConfig {

  @Binds
  @Named("exporterParser")
  public abstract ExporterParser bindExporterParser(ExporterParser parser);

  @Binds
  @Named("httpResponseFetcher")
  public abstract HttpResponseFetcher bindHttpResponseFetcher(HttpResponseFetcherImpl httpResponseFetcher);
}
