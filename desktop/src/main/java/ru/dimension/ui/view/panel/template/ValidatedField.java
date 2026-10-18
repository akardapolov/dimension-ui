package ru.dimension.ui.view.panel.template;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jdesktop.swingx.JXTextField;
import ru.dimension.ui.helper.FieldHighlighter;
import ru.dimension.ui.model.validation.ValidationIssue;
import ru.dimension.ui.model.validation.ValidationSeverity;

/**
 * Текстовое поле с подсветкой валидации. Подпись об ошибке/предупреждении
 * НЕ выводится под полем (чтобы не "дёргать" высоту строки в
 * PainlessGridBag) — она доступна через tooltip поля и сводится в общую
 * строку состояния диалога. Подсветка всегда жёлтая (WARNING), независимо
 * от реальной severity issue — красный цвет не используется нигде.
 */
public class ValidatedField extends JPanel {

  private static final int USER_PAUSE_DEBOUNCE_MS = 700;

  private final JXTextField textField;
  private final JButton applyButton;
  private final Timer userPauseTimer;

  private String fieldKey;
  private String suggestion;
  private Runnable onChange;
  private Runnable onUserPause;
  private Consumer<String> onApplySuggestion;

  /** true — поле реально менял пользователь руками, а не код (loadFrom/auto-apply). */
  private boolean userEdited = false;
  /** подавляет onChange/userEdited при программной установке текста. */
  private boolean suppressEvents = false;

  public ValidatedField(String prompt) {
    super(new BorderLayout(4, 0));

    this.textField = new JXTextField();
    this.textField.setPrompt(prompt);
    FieldHighlighter.clear(textField);

    this.applyButton = new JButton();
    this.applyButton.setFont(applyButton.getFont().deriveFont(10f));
    this.applyButton.setFocusPainted(false);
    this.applyButton.setVisible(false);
    this.applyButton.addActionListener(e -> {
      if (suggestion != null) {
        applySuggestionInternal(suggestion);
      }
    });

    this.userPauseTimer = new Timer(USER_PAUSE_DEBOUNCE_MS, e -> {
      if (userEdited && onUserPause != null) {
        onUserPause.run();
      }
    });
    userPauseTimer.setRepeats(false);

    add(textField, BorderLayout.CENTER);
    add(applyButton, BorderLayout.EAST);

    textField.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { fireChange(); }
      @Override public void removeUpdate(DocumentEvent e) { fireChange(); }
      @Override public void changedUpdate(DocumentEvent e) { fireChange(); }
    });
  }

  private void fireChange() {
    if (!suppressEvents) {
      userEdited = true;
      userPauseTimer.restart();
    }
    if (onChange != null) {
      onChange.run();
    }
  }

  private void applySuggestionInternal(String value) {
    setTextSilently(value);
    if (onApplySuggestion != null) {
      onApplySuggestion.accept(value);
    }
    if (onChange != null) {
      onChange.run();
    }
  }

  public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
  public String getFieldKey() { return fieldKey; }

  public void setOnChange(Runnable onChange) { this.onChange = onChange; }

  /** Срабатывает через debounce после того, как ПОЛЬЗОВАТЕЛЬ перестал печатать. */
  public void setOnUserPause(Runnable onUserPause) { this.onUserPause = onUserPause; }

  public void setOnApplySuggestion(Consumer<String> onApplySuggestion) {
    this.onApplySuggestion = onApplySuggestion;
  }

  /**
   * Программная установка значения (загрузка драфта, авто-дополнение при
   * дубликате). НЕ считается правкой пользователя и не запускает повторную
   * авто-проверку — иначе будет дёргаться туда-сюда.
   */
  public void setTextSilently(String text) {
    suppressEvents = true;
    try {
      textField.setText(text);
    } finally {
      suppressEvents = false;
    }
    userEdited = false;
  }

  /** Оставлено для обратной совместимости — ведёт себя как silent-установка. */
  public void setText(String text) {
    setTextSilently(text);
  }

  public String getText() { return textField.getText(); }
  public JXTextField getTextField() { return textField; }

  public boolean isUserEdited() { return userEdited; }

  /** Сбросить флаг "тронуто пользователем" без изменения текста (после успешного сохранения). */
  public void clearUserEditedFlag() { userEdited = false; }

  /**
   * Применяет подсветку поля и запоминает suggestion (если есть) для кнопки
   * быстрого применения. Текст сообщения уходит только в tooltip — отдельной
   * подписи под полем больше нет. Цвет подсветки всегда "жёлтый"
   * (ValidationSeverity.WARNING), вне зависимости от реальной severity —
   * красный цвет нигде не используется.
   */
  public void setIssue(ValidationIssue issue) {
    if (issue == null) {
      FieldHighlighter.clear(textField);
      applyButton.setVisible(false);
      suggestion = null;
      return;
    }

    FieldHighlighter.apply(textField, ValidationSeverity.WARNING, issue.getMessage());

    suggestion = issue.getSuggestedValue();
    applyButton.setVisible(suggestion != null);
    if (suggestion != null) {
      applyButton.setText("Use \"" + suggestion + "\"");
    }
  }
}