package ru.dimension.ui.prompt;

import java.util.ListResourceBundle;

public class Resource_en extends ListResourceBundle {

  private static final Object[][]
      prtText =
      {
          {"pName", "Profile name"},
          {"pDesc", "Profile description"},
          {"tName", "Task name"},
          {"tDesc", "Task description"},
          {"cName", "Connection name"},
          {"cURL", "Connection URL"},
          {"cUserName", "User name"},
          {"cPass", "Password"},
          {"cJar", "Jar-file path"},
          {"cDriver", "Driver"},
          {"cJmxURL", "JMX host:port or service:jmx url, empty for local JVM"},
          {"cJmxHint", "Empty URL monitors this application JVM (local mode). Remote credentials are sent unencrypted (no SSL)"},
          {"qName", "Query name"},
          {"qDesc", "Query description"},
          {"qSqlText", "SQL text"},
          {"mTableName", "Table name"},
          {"mTableType", "Table type"},
          {"mTableIndex", "Table index type"},
          {"mTimestamp", "Timestamp of table"},
          {"loadMeta", "Load metadata from database"},
          {"metricName", "Metric name"},
          {"metricDef", "Default"},
          {"xAxis", "X axis value"},
          {"yAxis", "Y axis value"},

          {"btnNew", "New"},
          {"btnCopy", "Copy"},
          {"btnDel", "Delete"},
          {"btnEdit", "Edit"},
          {"btnSave", "Save"},
          {"btnCancel", "Cancel"},

          {"sysErrMsg", "System error, see application message log"}
      };

  @Override
  protected Object[][] getContents() {
    return prtText;
  }
}
