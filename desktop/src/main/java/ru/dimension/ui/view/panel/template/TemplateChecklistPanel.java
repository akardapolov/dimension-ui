package ru.dimension.ui.view.panel.template;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import ru.dimension.ui.helper.FieldHighlighter;
import ru.dimension.ui.model.validation.ValidationIssue;
import ru.dimension.ui.model.validation.ValidationSection;
import ru.dimension.ui.model.validation.ValidationSeverity;

/**
 * Вариант C: чеклист секций слева от формы редактирования.
 * Клик по пункту переключает видимую секцию, иконка показывает её статус.
 */
public class TemplateChecklistPanel extends JPanel {

  private static final Color OK_TEXT = new Color(0x2E, 0x7D, 0x32);

  private final Map<ValidationSection, JButton> buttons = new LinkedHashMap<>();
  private Consumer<ValidationSection> onSectionSelected;

  public TemplateChecklistPanel() {
    setLayout(new GridLayout(ValidationSection.values().length, 1, 2, 2));
    setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

    for (ValidationSection section : ValidationSection.values()) {
      JButton button = new JButton(section.getTitle());
      button.setHorizontalAlignment(SwingConstants.LEFT);
      button.setFocusPainted(false);
      button.addActionListener(e -> selectSection(section));
      buttons.put(section, button);
      add(button);
    }
  }

  public void setOnSectionSelected(Consumer<ValidationSection> listener) {
    this.onSectionSelected = listener;
  }

  private void selectSection(ValidationSection section) {
    setSelectedSection(section);
    if (onSectionSelected != null) {
      onSectionSelected.accept(section);
    }
  }

  public void updateStatuses(List<ValidationIssue> issues) {
    Map<ValidationSection, ValidationSeverity> worst = new EnumMap<>(ValidationSection.class);
    Map<ValidationSection, Integer> counts = new EnumMap<>(ValidationSection.class);
    for (ValidationSection section : ValidationSection.values()) {
      worst.put(section, ValidationSeverity.OK);
      counts.put(section, 0);
    }
    for (ValidationIssue issue : issues) {
      if (issue.getSeverity().worseThan(worst.get(issue.getSection()))) {
        worst.put(issue.getSection(), issue.getSeverity());
      }
      counts.merge(issue.getSection(), 1, Integer::sum);
    }

    for (ValidationSection section : ValidationSection.values()) {
      JButton button = buttons.get(section);
      ValidationSeverity severity = worst.get(section);
      int count = counts.get(section);
      String countSuffix = count > 0 ? " (" + count + ")" : "";
      button.setText(FieldHighlighter.symbol(severity) + "  " + section.getTitle() + countSuffix);
      button.setForeground(switch (severity) {
        case ERROR -> FieldHighlighter.ERROR_COLOR;
        case WARNING -> FieldHighlighter.WARNING_COLOR;
        default -> OK_TEXT;
      });
    }
  }

  public void setSelectedSection(ValidationSection section) {
    for (Map.Entry<ValidationSection, JButton> entry : buttons.entrySet()) {
      entry.getValue().setFont(entry.getValue().getFont()
                                   .deriveFont(entry.getKey() == section ? Font.BOLD : Font.PLAIN));
    }
  }
}