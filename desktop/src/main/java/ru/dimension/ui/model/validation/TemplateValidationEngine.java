package ru.dimension.ui.model.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.model.config.ConfigEntity;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.type.ConnectionType;

public final class TemplateValidationEngine {

  private TemplateValidationEngine() {
  }

  public static List<ValidationIssue> validate(TemplateDraft draft, ConfigurationManager configurationManager) {
    List<ValidationIssue> issues = new ArrayList<>();

    validateProfile(draft, configurationManager, issues);
    validateTask(draft, configurationManager, issues);
    validateConnection(draft, configurationManager, issues);
    validateQueries(draft, configurationManager, issues);

    return issues;
  }

  private static void validateProfile(TemplateDraft draft, ConfigurationManager cm, List<ValidationIssue> issues) {
    String name = draft.getProfileName() == null ? "" : draft.getProfileName().trim();
    if (name.isEmpty()) {
      issues.add(new ValidationIssue(ValidationSection.PROFILE, "profile.name", ValidationSeverity.ERROR,
                                     "Profile name is required", null));
      return;
    }
    Set<String> existing = lowerNames(cm.getConfigList(Profile.class));
    if (existing.contains(name.toLowerCase())) {
      issues.add(new ValidationIssue(ValidationSection.PROFILE, "profile.name", ValidationSeverity.ERROR,
                                     "Profile \"" + name + "\" already exists",
                                     NameSuggester.suggestUnique(name, existing)));
    }
  }

  private static void validateTask(TemplateDraft draft, ConfigurationManager cm, List<ValidationIssue> issues) {
    String name = draft.getTaskName() == null ? "" : draft.getTaskName().trim();
    if (name.isEmpty()) {
      issues.add(new ValidationIssue(ValidationSection.TASK, "task.name", ValidationSeverity.ERROR,
                                     "Task name is required", null));
      return;
    }
    Set<String> existing = lowerNames(cm.getConfigList(Task.class));
    if (existing.contains(name.toLowerCase())) {
      issues.add(new ValidationIssue(ValidationSection.TASK, "task.name", ValidationSeverity.ERROR,
                                     "Task \"" + name + "\" already exists",
                                     NameSuggester.suggestUnique(name, existing)));
    }
    if (draft.getTaskDescription() == null || draft.getTaskDescription().trim().isEmpty()) {
      issues.add(new ValidationIssue(ValidationSection.TASK, "task.description", ValidationSeverity.WARNING,
                                     "Task description is empty", null));
    }
  }

  private static void validateConnection(TemplateDraft draft, ConfigurationManager cm, List<ValidationIssue> issues) {
    String name = draft.getConnName() == null ? "" : draft.getConnName().trim();
    if (name.isEmpty()) {
      issues.add(new ValidationIssue(ValidationSection.CONNECTION, "conn.name", ValidationSeverity.ERROR,
                                     "Connection name is required", null));
    } else {
      Set<String> existing = lowerNames(cm.getConfigList(Connection.class));
      if (existing.contains(name.toLowerCase())) {
        issues.add(new ValidationIssue(ValidationSection.CONNECTION, "conn.name", ValidationSeverity.ERROR,
                                       "Connection \"" + name + "\" already exists",
                                       NameSuggester.suggestUnique(name, existing)));
      }
    }

    if (draft.getConnUrl() == null || draft.getConnUrl().trim().isEmpty()) {
      issues.add(new ValidationIssue(ValidationSection.CONNECTION, "conn.url", ValidationSeverity.ERROR,
                                     "URL is required", null));
    }

    if (draft.getConnectionType() == ConnectionType.JDBC
        && (draft.getConnJar() == null || draft.getConnJar().trim().isEmpty())) {
      issues.add(new ValidationIssue(ValidationSection.CONNECTION, "conn.jar", ValidationSeverity.WARNING,
                                     "JDBC jar path is empty", null));
    }
  }

  private static void validateQueries(TemplateDraft draft, ConfigurationManager cm, List<ValidationIssue> issues) {
    Set<String> existingInConfig = lowerNames(cm.getConfigList(Query.class));
    Map<String, Integer> seenInDraft = new HashMap<>();

    List<TemplateDraft.QueryDraft> queries = draft.getQueries();
    for (int i = 0; i < queries.size(); i++) {
      TemplateDraft.QueryDraft q = queries.get(i);
      String fieldKey = "query." + i + ".name";
      String name = q.getName() == null ? "" : q.getName().trim();

      if (name.isEmpty()) {
        issues.add(new ValidationIssue(ValidationSection.QUERIES, fieldKey, ValidationSeverity.ERROR,
                                       "Query name is required (row " + (i + 1) + ")", null));
        continue;
      }

      String lower = name.toLowerCase();
      if (existingInConfig.contains(lower)) {
        issues.add(new ValidationIssue(ValidationSection.QUERIES, fieldKey, ValidationSeverity.ERROR,
                                       "Query \"" + name + "\" already exists",
                                       NameSuggester.suggestUnique(name, existingInConfig)));
      } else if (seenInDraft.containsKey(lower)) {
        issues.add(new ValidationIssue(ValidationSection.QUERIES, fieldKey, ValidationSeverity.ERROR,
                                       "Duplicate query name \"" + name + "\" in this task",
                                       NameSuggester.suggestUnique(name, existingInConfig)));
      }
      seenInDraft.put(lower, i);
    }
  }

  private static Set<String> lowerNames(List<? extends ConfigEntity> entities) {
    Set<String> set = new HashSet<>();
    for (ConfigEntity e : entities) {
      if (e.getName() != null) {
        set.add(e.getName().trim().toLowerCase());
      }
    }
    return set;
  }
}