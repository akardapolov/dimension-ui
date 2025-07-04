package ru.dimension.ui.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HttpServerLocal {

  private final String host;
  private final Integer port;
  private final String context;
  private final String response;

  @Getter
  private final String url;

  public HttpServerLocal(Integer port,
                         String context,
                         String response) {
    this.host = "localhost";
    this.port = port;
    this.context = context;
    this.response = response;

    this.url = "http://" + host + ":" + port + context;

    startHttpServer();
  }

  public void startHttpServer() {
    HttpServer server = null;
    try {
      server = HttpServer.create(new InetSocketAddress(host, port), 0);
      server.createContext(context, new MyHandler());
      server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
      server.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  class MyHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      long threadId = Thread.currentThread().threadId();
      log.info("Handle request: " + threadId);

      httpExchange.sendResponseHeaders(200, response.length());
      OutputStream os = httpExchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }
}
