package ru.dimension.ui.prompt;

import java.util.ListResourceBundle;

public class Resource_ru extends ListResourceBundle {

  private static final Object[][]
      prtText =
      {
          {"pName", "Имя профиля"},
          {"pDesc", "Описание профиля"},
          {"tName", "Имя задания"},
          {"tDesc", "Описание задания"},
          {"cName", "Имя подключения"},
          {"cURL", "URL подключения"},
          {"cUserName", "Имя пользователя"},
          {"cPass", "Пароль"},
          {"cJar", "Путь к jar файлу"},
          {"cDriver", "Драйвер"},
          {"qName", "Имя запроса"},
          {"qDesc", "Описание запроса"},
          {"qSqlText", "Текст SQl"},
          {"metaName", "Имя запроса"},
          {"loadMeta", "Загрузка метаданных из базы данных"},
          {"metricName", "Имя метрики"},
          {"metricDef", "По умолчанию"},
          {"xAxis", "Значение по оси X"},
          {"yAxis", "Значение по оси Y"},

          {"btnNew", "Создать"},
          {"btnCopy", "Копировать"},
          {"btnDel", "Удалить"},
          {"btnEdit", "Редактировать"},
          {"btnSave", "Сохранить"},
          {"btnCancel", "Отменить"},

          {"sysErrMsg", "Системная ошибка, смотрите журнал сообщений приложения"},
      };

  @Override
  protected Object[][] getContents() {
    return prtText;
  }
}
