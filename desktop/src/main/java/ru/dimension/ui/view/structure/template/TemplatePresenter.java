package ru.dimension.ui.view.structure.template;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.event.TreeSelectionListener;
import org.apache.hc.core5.http.Method;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.ConnectionAddEvent;
import ru.dimension.ui.bus.event.ProfileAddEvent;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Table;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.validation.TemplateDraft;
import ru.dimension.ui.model.validation.TemplateValidationEngine;
import ru.dimension.ui.model.validation.ValidationIssue;
import ru.dimension.ui.model.validation.ValidationSection;
import ru.dimension.ui.model.validation.ValidationSeverity;
import ru.dimension.ui.model.view.TemplateState;
import ru.dimension.ui.router.event.EventDispatcher;
import ru.dimension.ui.router.listener.TemplateListener;
import ru.dimension.ui.security.EncryptDecrypt;
import ru.dimension.ui.view.panel.template.TemplateCatalogPanel;
import ru.dimension.ui.view.panel.template.TemplateDetailsPanel;
import ru.dimension.ui.view.panel.template.TemplateEditDialog;
import ru.dimension.ui.view.panel.template.ValidatedField;
import ru.dimension.ui.view.panel.template.TemplateTreeItem;
import ru.dimension.ui.view.structure.TemplateView;
import ru.dimension.ui.view.table.row.Rows.TemplateMetricRow;
import ru.dimension.ui.view.table.row.Rows.TemplateQueryRow;

@Log4j2
@Singleton
public class TemplatePresenter extends WindowAdapter implements TemplateListener {

  /** Постфикс, который автоматически добавляется к именам, скопированным из шаблона. */
  private static final String COPY_SUFFIX = "_copied";

  private final TemplateView templateView;
  private final EventDispatcher eventDispatcher;
  private final EventBus eventBus;
  private final ProfileManager profileManager;
  private final ConfigurationManager configurationManager;
  private final TemplateManager templateManager;
  private final EncryptDecrypt encryptDecrypt;

  private final TemplateCatalogPanel catalogPanel;
  private final TemplateDetailsPanel detailsPanel;
  private final TemplateEditDialog editDialog;

  private TemplateDraft currentDraft;
  private Task sourceTask;
  private Connection sourceConnection;
  private List<ValidationIssue> lastIssues = Collections.emptyList();

  /**
   * true — идёт программная загрузка значений в панели (loadFrom подряд для
   * Profile/Task/Connection/Queries). Пока флаг включён, syncAndValidate()
   * не выполняется, чтобы панели, ещё не дождавшиеся своего loadFrom(),
   * не затёрли currentDraft своими СТАРЫМИ значениями (баг "эффект памяти"
   * между открытиями разных шаблонов).
   */
  private boolean loadingDraft = false;

  @Inject
  public TemplatePresenter(@Named("templateView") TemplateView templateView,
                           @Named("eventListener") EventDispatcher eventDispatcher,
                           @Named("eventBus") EventBus eventBus,
                           @Named("profileManager") ProfileManager profileManager,
                           @Named("configurationManager") ConfigurationManager configurationManager,
                           @Named("templateManager") TemplateManager templateManager,
                           @Named("encryptDecrypt") EncryptDecrypt encryptDecrypt,
                           @Named("templateCatalogPanel") TemplateCatalogPanel catalogPanel,
                           @Named("templateDetailsPanel") TemplateDetailsPanel detailsPanel,
                           @Named("templateEditDialog") TemplateEditDialog editDialog) {
    this.templateView = templateView;
    this.eventDispatcher = eventDispatcher;
    this.eventBus = eventBus;
    this.profileManager = profileManager;
    this.configurationManager = configurationManager;
    this.templateManager = templateManager;
    this.encryptDecrypt = encryptDecrypt;

    this.catalogPanel = catalogPanel;
    this.detailsPanel = detailsPanel;
    this.editDialog = editDialog;

    this.eventDispatcher.addTemplateStateListener(this);

    wireCatalogSelection();
    wireQueryPreviewSelection();
    wireLoadButton();
    wireEditDialog();
  }

  // ----- wiring -----

  private void wireCatalogSelection() {
    TreeSelectionListener listener = e -> catalogPanel.getSelectedItem().ifPresent(item -> {
      if (item.isGroup()) {
        detailsPanel.showEmpty();
        return;
      }
      showDetails(item);
    });
    catalogPanel.addTreeSelectionListener(listener);
  }

  private void wireQueryPreviewSelection() {
    detailsPanel.getQueryPreviewTable().table().getSelectionModel().addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return;
      }
      int row = detailsPanel.getQueryPreviewTable().table().getSelectedRow();
      if (row == -1) {
        return;
      }
      int modelRow = detailsPanel.getQueryPreviewTable().table().convertRowIndexToModel(row);
      TemplateQueryRow queryRow = detailsPanel.getQueryPreviewTable().model().itemAt(modelRow);

      Query query = findById(templateManager.getConfigList(Query.class), queryRow.getId(), Query.class);
      detailsPanel.showQueryPreview(query.getDescription(), query.getText());

      List<TemplateMetricRow> metricRows = query.getMetricList().stream()
          .map(m -> new TemplateMetricRow(
              m.getId(), m.getName(), m.getIsDefault(),
              m.getXAxis() != null ? m.getXAxis().getColName() : "",
              m.getYAxis() != null ? m.getYAxis().getColName() : "",
              m.getGroup() != null ? m.getGroup().getColName() : "",
              m.getGroupFunction() != null ? m.getGroupFunction().toString() : "",
              m.getChartType() != null ? m.getChartType().toString() : ""))
          .toList();
      detailsPanel.getMetricsPreviewTable().setItems(metricRows);
    });
  }

  private void wireLoadButton() {
    detailsPanel.getLoadButton().addActionListener(e -> catalogPanel.getSelectedItem()
        .filter(item -> !item.isGroup())
        .ifPresent(this::startEdit));
  }

  private void wireEditDialog() {
    Runnable onChange = this::syncAndValidate;
    editDialog.getProfilePanel().setOnChange(onChange);
    editDialog.getTaskPanel().setOnChange(onChange);
    editDialog.getConnectionPanel().setOnChange(onChange);
    editDialog.getQueriesPanel().setOnChange(onChange);

    // Fallback — ручное применение подсказки по клику.
    editDialog.getProfilePanel().getProfileNameField().setOnApplySuggestion(v -> syncAndValidate());
    editDialog.getTaskPanel().getTaskNameField().setOnApplySuggestion(v -> syncAndValidate());
    editDialog.getConnectionPanel().getConnNameField().setOnApplySuggestion(v -> syncAndValidate());

    // Авто-резолв дубликатов: срабатывает сам после паузы в наборе текста,
    // только если поле реально редактировал пользователь (приоритетная задача).
    editDialog.getProfilePanel().getProfileNameField()
        .setOnUserPause(() -> autoResolveFieldDuplicate(ValidationSection.PROFILE, "profile.name",
                                                        editDialog.getProfilePanel().getProfileNameField()));
    editDialog.getTaskPanel().getTaskNameField()
        .setOnUserPause(() -> autoResolveFieldDuplicate(ValidationSection.TASK, "task.name",
                                                        editDialog.getTaskPanel().getTaskNameField()));
    editDialog.getConnectionPanel().getConnNameField()
        .setOnUserPause(() -> autoResolveFieldDuplicate(ValidationSection.CONNECTION, "conn.name",
                                                        editDialog.getConnectionPanel().getConnNameField()));

    editDialog.getQueriesPanel().setOnQueryNameEdited(this::autoResolveQueryDuplicate);

    editDialog.getSaveButton().addActionListener(e -> saveDraft());
    editDialog.getCancelButton().addActionListener(e -> cancelEdit());
  }

  private void autoResolveFieldDuplicate(ValidationSection section, String fieldKey, ValidatedField field) {
    if (currentDraft == null || !field.isUserEdited()) {
      return;
    }
    syncAndValidate();
    lastIssues.stream()
        .filter(i -> i.getSection() == section && fieldKey.equals(i.getFieldKey()) && i.getSuggestedValue() != null)
        .findFirst()
        .ifPresent(issue -> {
          field.setTextSilently(issue.getSuggestedValue());
          syncAndValidate();
          editDialog.showAutoRenameHint(section, issue.getSuggestedValue());
        });
  }

  private void autoResolveQueryDuplicate(int row, String newName) {
    if (currentDraft == null || row < 0 || row >= currentDraft.getQueries().size()) {
      return;
    }
    syncAndValidate();
    String fieldKey = "query." + row + ".name";
    lastIssues.stream()
        .filter(i -> i.getSection() == ValidationSection.QUERIES
            && fieldKey.equals(i.getFieldKey()) && i.getSuggestedValue() != null)
        .findFirst()
        .ifPresent(issue -> {
          editDialog.getQueriesPanel().setQueryNameSilently(row, issue.getSuggestedValue());
          syncAndValidate();
          editDialog.showAutoRenameHint(ValidationSection.QUERIES, issue.getSuggestedValue());
        });
  }

  // ----- catalog / details -----

  public void fillModel() {
    List<Task> tasks = templateManager.getConfigList(Task.class);
    List<Connection> connections = templateManager.getConfigList(Connection.class);
    catalogPanel.setModel(tasks, connections);
    catalogPanel.selectFirstTemplate();
  }

  private void showDetails(TemplateTreeItem item) {
    detailsPanel.showTemplate(item.getTask(), item.getConnection());

    List<Query> queries = templateManager.getConfigList(Query.class).stream()
        .filter(q -> item.getTask().getQueryList().contains(q.getId()))
        .toList();

    List<TemplateQueryRow> queryRows = queries.stream()
        .map(q -> new TemplateQueryRow(q.getId(), q.getName(), q.getGatherDataMode().name()))
        .toList();
    detailsPanel.getQueryPreviewTable().setItems(queryRows);

    if (!queryRows.isEmpty()) {
      detailsPanel.getQueryPreviewTable().table().setRowSelectionInterval(0, 0);
    }
  }

  // ----- edit lifecycle (отдельный JDialog) -----

  private void startEdit(TemplateTreeItem item) {
    this.sourceTask = item.getTask();
    this.sourceConnection = item.getConnection();
    this.currentDraft = buildDraft(sourceTask, sourceConnection);

    // Исходные тексты запросов шаблона (по id исходного Query) — нужны панели
    // Queries, чтобы было что показать в детализации запроса (QueryDetailDialog)
    // при первом открытии, пока пользователь ещё ничего не редактировал.
    Map<Integer, String> queryTextBySourceId = templateManager.getConfigList(Query.class).stream()
        .filter(q -> sourceTask.getQueryList().contains(q.getId()))
        .collect(Collectors.toMap(Query::getId, Query::getText));

    // Важно: пока все панели последовательно не загружены свежим драфтом,
    // блокируем их onChange -> syncAndValidate(), иначе панели, до которых
    // loadFrom() ещё не дошёл, применят к currentDraft СТАРЫЕ значения
    // (оставшиеся в полях от прошлого открытого шаблона).
    loadingDraft = true;
    try {
      editDialog.getProfilePanel().loadFrom(currentDraft);
      editDialog.getTaskPanel().loadFrom(currentDraft);
      editDialog.getConnectionPanel().loadFrom(currentDraft, sourceConnection);
      editDialog.getQueriesPanel().loadFrom(currentDraft, queryTextBySourceId);
    } finally {
      loadingDraft = false;
    }

    syncAndValidate();

    editDialog.open(sourceTask.getName());
  }

  private void cancelEdit() {
    this.currentDraft = null;
    this.lastIssues = Collections.emptyList();
    editDialog.close();
  }

  private TemplateDraft buildDraft(Task task, Connection connection) {
    TemplateDraft draft = new TemplateDraft();
    draft.setSourceTaskId(task.getId());
    draft.setSourceConnectionId(connection.getId());

    Set<String> profileNames = lowerNames(configurationManager.getConfigList(Profile.class));
    draft.setProfileName(suggestCopiedName(task.getName(), profileNames));
    draft.setProfileDescription("");

    Set<String> taskNames = lowerNames(configurationManager.getConfigList(Task.class));
    draft.setTaskName(suggestCopiedName(task.getName(), taskNames));
    draft.setTaskDescription(task.getDescription());
    draft.setPullTimeoutSec(task.getPullTimeout());

    ConnectionType type = connection.getType() == null ? ConnectionType.JDBC : connection.getType();
    draft.setConnectionType(type);

    Set<String> connNames = lowerNames(configurationManager.getConfigList(Connection.class));
    draft.setConnName(suggestCopiedName(connection.getName(), connNames));
    draft.setConnUrl(connection.getUrl());
    draft.setConnUserName(connection.getUserName());
    draft.setConnJar(connection.getJar());
    draft.setConnDriver(connection.getDriver());

    Set<String> queryNames = lowerNames(configurationManager.getConfigList(Query.class));
    List<Query> templateQueries = templateManager.getConfigList(Query.class).stream()
        .filter(q -> task.getQueryList().contains(q.getId()))
        .toList();
    for (Query q : templateQueries) {
      String uniqueName = suggestCopiedName(q.getName(), queryNames);
      queryNames.add(uniqueName.toLowerCase());
      String gatherDataMode = q.getGatherDataMode() != null ? q.getGatherDataMode().name() : "";
      draft.getQueries().add(new TemplateDraft.QueryDraft(q.getId(), uniqueName, q.getDescription(), gatherDataMode));
    }

    return draft;
  }

  /**
   * Автоматически формирует имя-копию с постфиксом "_copied", чтобы оператору
   * не приходилось вручную придумывать Name для Profile/Task/Connection/Query.
   * При повторном конфликте добавляется числовой суффикс: "_copied2", "_copied3"...
   */
  private String suggestCopiedName(String baseName, Set<String> existingLowerNames) {
    String base = baseName == null ? "" : baseName.trim();
    String candidate = base + COPY_SUFFIX;
    if (!existingLowerNames.contains(candidate.toLowerCase())) {
      return candidate;
    }
    int counter = 2;
    while (existingLowerNames.contains((candidate + counter).toLowerCase())) {
      counter++;
    }
    return candidate + counter;
  }

  private void syncAndValidate() {
    if (currentDraft == null || loadingDraft) {
      return;
    }
    editDialog.getProfilePanel().applyTo(currentDraft);
    editDialog.getTaskPanel().applyTo(currentDraft);
    editDialog.getConnectionPanel().applyTo(currentDraft);
    editDialog.getQueriesPanel().applyTo(currentDraft);

    lastIssues = TemplateValidationEngine.validate(currentDraft, configurationManager);
    editDialog.refreshValidation(lastIssues);
    editDialog.updatePreview(currentDraft);
  }

  private void saveDraft() {
    if (currentDraft == null) {
      return;
    }
    List<ValidationIssue> issues = TemplateValidationEngine.validate(currentDraft, configurationManager);
    boolean hasErrors = issues.stream().anyMatch(i -> i.getSeverity() == ValidationSeverity.ERROR);
    if (hasErrors) {
      editDialog.refreshValidation(issues);
      return;
    }

    persist(currentDraft);

    this.currentDraft = null;
    this.lastIssues = Collections.emptyList();
    editDialog.close();

    fillModel();

    JOptionPane.showMessageDialog(editDialog, "Template successfully loaded into configuration",
                                  "Saved", JOptionPane.INFORMATION_MESSAGE);
  }

  private void persist(TemplateDraft draft) {
    int queryMaxId = configurationManager.getConfigList(Query.class).stream()
        .map(Query::getId).reduce(Integer::max).orElse(0) + 1;

    List<Integer> newQueryIds = new java.util.ArrayList<>();
    List<TemplateDraft.QueryDraft> queryDrafts = draft.getQueries();
    for (int i = 0; i < queryDrafts.size(); i++) {
      TemplateDraft.QueryDraft qd = queryDrafts.get(i);
      Query templateQuery = findById(templateManager.getConfigList(Query.class), qd.getSourceQueryId(), Query.class);

      Query newQuery = cloneQuery(templateQuery);
      newQuery.setId(queryMaxId);
      newQuery.setName(qd.getName());
      newQuery.setDescription(qd.getDescription());

      // Текст запроса мог быть отредактирован в детализации (QueryDetailDialog) —
      // берём актуальную версию из панели, а не только исходный текст шаблона.
      String editedText = editDialog.getQueriesPanel().getQueryText(i);
      if (editedText != null && !editedText.isBlank()) {
        newQuery.setText(editedText);
      }

      configurationManager.addConfig(newQuery, Query.class);

      Table table = new Table();
      table.setTableName(newQuery.getName());
      configurationManager.addConfig(table, Table.class);

      newQueryIds.add(queryMaxId);
      queryMaxId++;
    }

    int connMaxId = configurationManager.getConfigList(Connection.class).stream()
        .map(Connection::getId).reduce(Integer::max).orElse(0) + 1;

    Connection newConnection = cloneConnection(sourceConnection);
    newConnection.setId(connMaxId);
    newConnection.setName(draft.getConnName());
    newConnection.setUrl(draft.getConnUrl());
    newConnection.setUserName(draft.getConnUserName());
    newConnection.setJar(draft.getConnJar());
    newConnection.setDriver(draft.getConnDriver());

    ConnectionType type = draft.getConnectionType();
    if (type == ConnectionType.HTTP) {
      Method method = editDialog.getConnectionPanel().getSelectedHttpMethod();
      newConnection.setHttpMethod(method);
      newConnection.setParseType(editDialog.getConnectionPanel().getSelectedParseType());
      newConnection.setPassword(sourceConnection.getPassword());
    } else if (type == ConnectionType.JMX) {
      char[] jmxPwd = editDialog.getConnectionPanel().getJmxPasswordChars();
      try {
        String jmxPwdStr = String.valueOf(jmxPwd);
        newConnection.setPassword(encryptDecrypt.encrypt(jmxPwdStr));
      } finally {
        java.util.Arrays.fill(jmxPwd, '0');
      }
    } else {
      char[] jdbcPwd = editDialog.getConnectionPanel().getJdbcPasswordChars();
      try {
        String jdbcPwdStr = String.valueOf(jdbcPwd);
        if (!jdbcPwdStr.isBlank()) {
          newConnection.setPassword(encryptDecrypt.encrypt(jdbcPwdStr));
        } else {
          newConnection.setPassword(sourceConnection.getPassword());
        }
      } finally {
        java.util.Arrays.fill(jdbcPwd, '0');
      }
    }

    configurationManager.addConfig(newConnection, Connection.class);
    eventBus.publish(new ConnectionAddEvent(newConnection.getId(), newConnection.getName(), newConnection.getType()));

    int taskMaxId = configurationManager.getConfigList(Task.class).stream()
        .map(Task::getId).reduce(Integer::max).orElse(0) + 1;

    Task newTask = cloneTask(sourceTask);
    newTask.setId(taskMaxId);
    newTask.setName(draft.getTaskName());
    newTask.setDescription(draft.getTaskDescription());
    newTask.setPullTimeout(draft.getPullTimeoutSec());
    newTask.setConnectionId(connMaxId);
    newTask.setQueryList(newQueryIds);
    configurationManager.addConfig(newTask, Task.class);

    int profileMaxId = configurationManager.getConfigList(Profile.class).stream()
        .map(Profile::getId).reduce(Integer::max).orElse(0) + 1;

    Profile profile = new Profile();
    profile.setId(profileMaxId);
    profile.setName(draft.getProfileName());
    profile.setDescription(draft.getProfileDescription());
    profile.setTaskList(Collections.singletonList(taskMaxId));
    configurationManager.addConfig(profile, Profile.class);

    profileManager.updateCache();
    eventBus.publish(new ProfileAddEvent());
  }

  // ----- helpers -----

  private Query cloneQuery(Query source) {
    Query copy = new Query();
    copy.setGatherDataMode(source.getGatherDataMode());
    copy.setText(source.getText());
    copy.setMetricList(source.getMetricList());
    return copy;
  }

  /**
   * Клонирует Connection через {@link #shallowCopy(Object)}, чтобы ВСЕ поля
   * класса (включая те, что появятся в будущем) переносились автоматически,
   * а не перечислялись руками. После клонирования вызывающий код явно
   * перезаписывает нужные поля (id/name/url/...) — это нормально и ожидаемо.
   * <p>
   * Если у {@link Connection} есть Lombok {@code @Builder(toBuilder = true)},
   * предпочтительнее заменить этот метод на {@code source.toBuilder().build()}.
   */
  private Connection cloneConnection(Connection source) {
    return shallowCopy(source);
  }

  /**
   * Клонирует Task через {@link #shallowCopy(Object)} — см. комментарий к
   * {@link #cloneConnection(Connection)}.
   */
  private Task cloneTask(Task source) {
    return shallowCopy(source);
  }

  /**
   * Универсальный shallow-copy POJO через рефлексию: копирует значения ВСЕХ
   * объявленных полей (включая унаследованные от родительских классов,
   * кроме static) из {@code source} в новый экземпляр того же класса,
   * созданный через no-args конструктор.
   * <p>
   * Это защищает от "хрупкого" ручного клонирования (как было раньше в
   * cloneTask/cloneConnection), когда при добавлении нового поля в модель
   * конфигурации его забывали перенести в клон — поле тихо терялось при
   * сохранении шаблона в Configuration.
   * <p>
   * Ограничение: копирование shallow — вложенные объекты/коллекции
   * копируются по ссылке, а не глубоко. Для текущих сценариев (Task,
   * Connection) это безопасно, так как все мутируемые коллекции
   * (например, queryList у Task) сразу перезаписываются новым значением
   * после клонирования.
   */
  @SuppressWarnings("unchecked")
  private <T> T shallowCopy(T source) {
    if (source == null) {
      return null;
    }
    try {
      Class<T> clazz = (Class<T>) source.getClass();
      T copy = clazz.getDeclaredConstructor().newInstance();

      Class<?> current = clazz;
      while (current != null && current != Object.class) {
        for (Field field : current.getDeclaredFields()) {
          if (Modifier.isStatic(field.getModifiers())) {
            continue;
          }
          field.setAccessible(true);
          field.set(copy, field.get(source));
        }
        current = current.getSuperclass();
      }
      return copy;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
          "Failed to clone " + source.getClass().getSimpleName()
              + " — no-args constructor is required for shallowCopy()", e);
    }
  }

  private <T extends ru.dimension.ui.model.config.ConfigEntity> T findById(List<T> list, int id, Class<T> clazz) {
    return list.stream().filter(f -> f.getId() == id).findAny()
        .orElseThrow(() -> new NotFoundException("Not found " + clazz.getSimpleName() + " by id: " + id));
  }

  private Set<String> lowerNames(List<? extends ru.dimension.ui.model.config.ConfigEntity> entities) {
    Set<String> set = new HashSet<>();
    for (ru.dimension.ui.model.config.ConfigEntity e : entities) {
      if (e.getName() != null) {
        set.add(e.getName().trim().toLowerCase());
      }
    }
    return set;
  }

  // ----- TemplateListener / WindowAdapter -----

  @Override
  public void fireShowTemplate(TemplateState templateState) {
    if (templateState == TemplateState.SHOW) {
      fillModel();
      templateView.showTemplate();
    }
    if (templateState == TemplateState.HIDE) {
      templateView.hideTemplate();
    }
  }

  @Override
  public void windowClosing(WindowEvent e) {
    log.info("Window template closing event received");
  }
}