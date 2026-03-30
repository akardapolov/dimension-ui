package ru.dimension.ui.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ConnectionErrorHelper {

  private static final Pattern HOST_PORT_PATTERN = Pattern.compile("(?:https?://)?([\\w.\\-]+):(\\d+)");

  private ConnectionErrorHelper() {
  }

  public static String formatConnectionError(String connectionName, Exception e) {
    String rootCause = extractRootCause(e);

    return String.format("""
        
        ###############################################################
        # CONNECTION FAILED: %s
        # Reason: %s
        ###############################################################
        """, connectionName, rootCause);
  }

  public static String extractRootCause(Throwable e) {
    Throwable root = e;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }

    String message = root.getMessage();
    if (message == null || message.isBlank()) {
      message = root.getClass().getSimpleName();
    }

    message = cleanUpMessage(message);

    return message;
  }

  private static String cleanUpMessage(String message) {
    if (message.contains("Connection refused")) {
      String hostPort = extractHostPort(message);
      return "Connection refused" + (hostPort != null ? " (" + hostPort + ")" : "");
    }

    if (message.contains("ORA-")) {
      int oraStart = message.indexOf("ORA-");
      int oraEnd = message.indexOf('\n', oraStart);
      if (oraEnd < 0) {
        oraEnd = message.length();
      }
      return message.substring(oraStart, oraEnd).trim();
    }

    if (message.contains("Login failed")) {
      return "Login failed — check username/password";
    }

    if (message.contains("Network is unreachable") || message.contains("No route to host")) {
      return "Network unreachable — check host/port";
    }

    if (message.contains("UnknownHostException") || message.contains("nodename nor servname")) {
      return "Unknown host — check hostname";
    }

    if (message.contains("Connection timed out")) {
      return "Connection timed out — check host/port/firewall";
    }

    if (message.contains("Access denied")) {
      return "Access denied — check username/password";
    }

    if (message.contains("Communications link failure")) {
      return "Communications link failure — check host/port";
    }

    if (message.contains("null password")) {
      return "Null password given — check password configuration";
    }

    if (message.length() > 120) {
      return message.substring(0, 120) + "...";
    }

    return message;
  }

  private static String extractHostPort(String message) {
    Matcher matcher = HOST_PORT_PATTERN.matcher(message);
    if (matcher.find()) {
      return matcher.group(1) + ":" + matcher.group(2);
    }
    return null;
  }

  public static void logConnectionError(String connectionName, Exception e) {
    String compact = formatConnectionError(connectionName, e);
    log.error(compact);
    log.debug("Full stack trace for connection '{}': ", connectionName, e);
  }
}