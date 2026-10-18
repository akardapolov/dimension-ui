// TemplateDraft.java
package ru.dimension.ui.model.validation;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import ru.dimension.ui.model.type.ConnectionType;

@Data
public class TemplateDraft {

  private Integer sourceTaskId;
  private Integer sourceConnectionId;

  private String profileName = "";
  private String profileDescription = "";

  private String taskName = "";
  private String taskDescription = "";
  private int pullTimeoutSec;

  private ConnectionType connectionType = ConnectionType.JDBC;

  private String connName = "";
  private String connUserName = "";
  private String connUrl = "";
  private String connJar = "";
  private String connDriver = "";

  private final List<QueryDraft> queries = new ArrayList<>();

  @Data
  public static class QueryDraft {

    private final int sourceQueryId;
    private String name;
    private String description;
    /**
     * Режим сбора данных исходного запроса (Query.getGatherDataMode().name()).
     * Только для отображения в детализации запроса (QueryDetailDialog) —
     * не редактируется пользователем и не участвует в валидации/сохранении,
     * при Save в Configuration копируется напрямую из исходного Query
     * (см. TemplatePresenter.cloneQuery(...)).
     */
    private String gatherDataMode;

    public QueryDraft(int sourceQueryId, String name, String description, String gatherDataMode) {
      this.sourceQueryId = sourceQueryId;
      this.name = name;
      this.description = description;
      this.gatherDataMode = gatherDataMode;
    }
  }
}