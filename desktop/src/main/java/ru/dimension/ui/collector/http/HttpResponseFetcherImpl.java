package ru.dimension.ui.collector.http;

import static javax.servlet.http.HttpServletRequest.BASIC_AUTH;
import static org.apache.hc.client5.http.auth.StandardAuthScheme.BASIC;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.DigestScheme;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import ru.dimension.ui.collector.collect.CollectorConstants;
import ru.dimension.ui.collector.collect.common.CommonHttpClient;
import ru.dimension.ui.collector.collect.common.HttpProtocol;
import ru.dimension.ui.collector.collect.utils.CollectUtil;
import ru.dimension.ui.collector.collect.utils.CommonUtil;
import ru.dimension.ui.collector.collect.utils.IpDomainUtil;

@Log4j2
@Singleton
public class HttpResponseFetcherImpl implements HttpResponseFetcher {

  static Set<Integer> defaultSuccessStatusCodes = Stream.of(HttpStatus.SC_OK,
                                                            HttpStatus.SC_CREATED,
                                                            HttpStatus.SC_ACCEPTED,
                                                            HttpStatus.SC_MULTIPLE_CHOICES,
                                                            HttpStatus.SC_MOVED_PERMANENTLY,
                                                            HttpStatus.SC_MOVED_TEMPORARILY)
      .collect(Collectors.toSet());

  @Inject
  public HttpResponseFetcherImpl() {
  }

  @Override
  public String fetchResponse(HttpProtocol httpProtocol) {
    HttpContext httpContext = createHttpContext(httpProtocol);
    ClassicHttpRequest request = createHttpRequest(httpProtocol);
    HttpHost target = new HttpHost(request.getScheme(), request.getAuthority());

    String resp = "";

    try (ClassicHttpResponse response = CommonHttpClient.getHttpClient().executeOpen(target, request, httpContext)) {
      int statusCode = response.getCode();
      log.debug("http response status: {}", statusCode);
      if (!checkSuccessInvoke(httpProtocol, statusCode)) {
        log.error("StatusCode {}", statusCode);
        return resp;
      }
      return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    } catch (ClientProtocolException e1) {
      log.error("Client Protocol Exception: {}, Message: {}", e1.getClass()
          .getSimpleName(), CommonUtil.getMessageFromThrowable(e1));
    } catch (SocketTimeoutException e2) {
      log.error("Socket Timeout: {}, Message: {}", e2.getClass()
          .getSimpleName(), CommonUtil.getMessageFromThrowable(e2));
    } catch (IOException e4) {
      log.error("IO Exception: {}, Message: {}", e4.getClass().getSimpleName(), CommonUtil.getMessageFromThrowable(e4));
    } catch (Exception e) {
      log.error("Unexpected Exception: {}, Message: {}", e.getClass()
          .getSimpleName(), CommonUtil.getMessageFromThrowable(e), e);
    }

    return resp;
  }

  private boolean checkSuccessInvoke(HttpProtocol httpProtocol,
                                     int statusCode) {
    List<String> successCodes = httpProtocol.getSuccessCodes();
    Set<Integer> successCodeSet = successCodes != null ? successCodes.stream().map(code -> {
      try {
        return Integer.valueOf(code);
      } catch (Exception ignored) {
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toSet()) : defaultSuccessStatusCodes;
    if (successCodeSet.isEmpty()) {
      successCodeSet = defaultSuccessStatusCodes;
    }
    return successCodeSet.contains(statusCode);
  }

  public HttpContext createHttpContext(HttpProtocol httpProtocol) {
    HttpProtocol.Authorization auth = httpProtocol.getAuthorization();
    if (auth != null && CollectorConstants.DIGEST_AUTH.equals(auth.getType())) {
      HttpClientContext clientContext = new HttpClientContext();
      if (CommonUtil.isNullOrBlank(auth.getDigestAuthUsername())
          && CommonUtil.isNullOrBlank(auth.getDigestAuthPassword())) {
        UsernamePasswordCredentials credentials
            = new UsernamePasswordCredentials(auth.getDigestAuthUsername(), auth.getDigestAuthPassword().toCharArray());

        // Create AuthScope
        int portNumber;
        try {
          portNumber = Integer.parseInt(httpProtocol.getPort());
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Port must be a valid number", e);
        }

        AuthScope authScope = new AuthScope(httpProtocol.getHost(), portNumber);
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(authScope, credentials);

        AuthCache authCache = new BasicAuthCache();
        authCache.put(new HttpHost(httpProtocol.getHost(), Integer.parseInt(httpProtocol.getPort())), new DigestScheme());
        clientContext.setCredentialsProvider(provider);
        clientContext.setAuthCache(authCache);
        return clientContext;
      }
    }
    return null;
  }

  public ClassicHttpRequest createHttpRequest(HttpProtocol httpProtocol) {
    // Build the URI first, as it's going to be used in any case
    String uri = CollectUtil.replaceUriSpecialChar(httpProtocol.getUrl());
    if (IpDomainUtil.isHasSchema(httpProtocol.getHost())) {
      uri = httpProtocol.getHost() + ":" + httpProtocol.getPort() + uri;
    } else {
      String ipAddressType = IpDomainUtil.checkIpAddressType(httpProtocol.getHost());
      String baseUri = CollectorConstants.IPV6.equals(ipAddressType)
          ? String.format("[%s]:%s%s", httpProtocol.getHost(), httpProtocol.getPort(), uri)
          : String.format("%s:%s%s", httpProtocol.getHost(), httpProtocol.getPort(), uri);
      boolean ssl = Boolean.parseBoolean(httpProtocol.getSsl());
      uri = ssl ? CollectorConstants.HTTPS_HEADER + baseUri : CollectorConstants.HTTP_HEADER + baseUri;
    }

    // Start building the request
    ClassicRequestBuilder requestBuilder =
        ClassicRequestBuilder.create(httpProtocol.getMethod().toUpperCase())
            .setUri(uri);

    // Parameters
    Map<String, String> params = httpProtocol.getParams();
    if (params != null) {
      params.forEach((key, val) -> {
        if (CommonUtil.isNullOrBlank(val)) {
          requestBuilder.addParameter(key, val);
        }
      });
    }

    // Default and custom headers
    requestBuilder.addHeader(HttpHeaders.CONNECTION, "keep-alive");
    requestBuilder.addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 ...");

    // Overwrite headers with httpProtocol's headers
    Map<String, String> headers = httpProtocol.getHeaders();
    if (headers != null) {
      headers.forEach((key, val) -> {
        if (CommonUtil.isNullOrBlank(val)) {
          requestBuilder.addHeader(CollectUtil.replaceUriSpecialChar(key),
                                   CollectUtil.replaceUriSpecialChar(val));
        }
      });
    }

    // Accept header based on parse type
    switch (httpProtocol.getParseType()) {
      case CollectorConstants.PARSE_DEFAULT:
      case CollectorConstants.PARSE_JSON_PATH:
        requestBuilder.addHeader(HttpHeaders.ACCEPT, "application/json");
        break;
      case CollectorConstants.PARSE_XML_PATH:
        requestBuilder.addHeader(HttpHeaders.ACCEPT, "text/xml,application/xml");
        break;
      default:
        requestBuilder.addHeader(HttpHeaders.ACCEPT, "*/*");
    }

    // Authorization
    HttpProtocol.Authorization authorization = httpProtocol.getAuthorization();
    if (authorization != null) {
      String authHeader = "";
      switch (authorization.getType()) {
        case CollectorConstants.BEARER_TOKEN:
          authHeader = CollectorConstants.BEARER + " " + authorization.getBearerTokenToken();
          break;
        case BASIC_AUTH:
          if (CommonUtil.isNullOrBlank(authorization.getBasicAuthUsername())
              && CommonUtil.isNullOrBlank(authorization.getBasicAuthPassword())) {
            String authStr = authorization.getBasicAuthUsername() + ":" + authorization.getBasicAuthPassword();
            String encodedAuth = new String(Base64.encodeBase64(authStr.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            authHeader = BASIC + " " + encodedAuth;
          }
          break;
        default:
          log.error("Unsupported authorization type: {}", authorization.getType());
          return null;
      }
      requestBuilder.addHeader(HttpHeaders.AUTHORIZATION, authHeader);
    }

    // Body
    if (!CommonUtil.isNullOrBlank(httpProtocol.getPayload())) {
      requestBuilder.setEntity(new StringEntity(httpProtocol.getPayload(), ContentType.APPLICATION_JSON));
    }

    try {
      return requestBuilder.build();
    } catch (IllegalStateException e) {
      log.error("Could not build http request", e);
      return null;
    }
  }
}
