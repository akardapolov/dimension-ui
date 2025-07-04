package ru.dimension.ui.collector.http;

import ru.dimension.ui.collector.collect.common.HttpProtocol;

public interface HttpResponseFetcher {

  String fetchResponse(HttpProtocol httpProtocol) throws Exception;
}

