/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.dimension.ui.collector.collect.common;

import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

public class CommonHttpClient {

  private static CloseableHttpClient httpClient;

  private static PoolingHttpClientConnectionManager connectionManager;

  private static final int MAX_TOTAL_CONNECTIONS = 50000;
  private static final int MAX_PER_ROUTE_CONNECTIONS = 80;
  private static final int REQUIRE_CONNECT_TIMEOUT = 4000;
  private static final int CONNECT_TIMEOUT = 4000;
  private static final int SOCKET_TIMEOUT = 60000;
  private static final int INACTIVITY_VALIDATED_TIME = 10000;
  private static final String[] SUPPORTED_SSL = {"TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3"};

  static {
    try {
      SSLContext sslContext = SSLContexts.createDefault();
      sslContext.init(null, new TrustManager[]{new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates,
                                       String s) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates,
                                       String s) throws CertificateException {
          Date now = new Date();
          if (x509Certificates != null) {
            for (X509Certificate certificate : x509Certificates) {
              Date deadline = certificate.getNotAfter();
              if (deadline != null && now.after(deadline)) {
                throw new CertificateExpiredException();
              }
            }
          }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }
      }}, null);

      SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(
          sslContext,
          SUPPORTED_SSL,
          null,
          new NoopHostnameVerifier()
      );

      final ConnectionConfig connectionConfig = ConnectionConfig.custom()
          .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT))
          .setValidateAfterInactivity(TimeValue.ofMilliseconds(INACTIVITY_VALIDATED_TIME))
          .build();

      connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
          .setSSLSocketFactory(sslFactory)
          .setMaxConnTotal(MAX_TOTAL_CONNECTIONS)
          .setMaxConnPerRoute(MAX_PER_ROUTE_CONNECTIONS)
          .setDefaultSocketConfig(SocketConfig.custom()
                                      .setSoTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT))
                                      .build())
          .setConnectionConfigResolver(route -> connectionConfig)
          .build();

      final RequestConfig requestConfig = RequestConfig.custom()
          .setResponseTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT))
          .setConnectionRequestTimeout(Timeout.ofMilliseconds(REQUIRE_CONNECT_TIMEOUT))
          .setRedirectsEnabled(true)
          .build();

      httpClient = HttpClients.custom()
          .setConnectionManager(connectionManager)
          .setDefaultRequestConfig(requestConfig)
          // clean up unavailable expired connections
          .evictExpiredConnections()
          // clean up available but idle connections
          .evictIdleConnections(TimeValue.ofSeconds(100))
          .build();

      class CustomThreadFactory implements ThreadFactory {

        private final String namePrefix;
        private final boolean daemon;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public CustomThreadFactory(String namePrefix,
                                   boolean daemon) {
          this.namePrefix = namePrefix;
          this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
          Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
          thread.setDaemon(daemon);
          return thread;
        }
      }

      ThreadFactory threadFactory = new CustomThreadFactory("http-connection-pool-cleaner-", true);

      ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1, threadFactory);
      scheduledExecutor.scheduleWithFixedDelay(() -> {
        connectionManager.closeExpired();
        connectionManager.closeIdle(TimeValue.ofSeconds(100));
      }, 40L, 40L, TimeUnit.SECONDS);

    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  public static CloseableHttpClient getHttpClient() {
    return httpClient;
  }
}

