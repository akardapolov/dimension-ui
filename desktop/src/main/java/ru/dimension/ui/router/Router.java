package ru.dimension.ui.router;

public interface Router {

  void runConfigDialog(int profileId);

  void runTemplateDialog();

  void runReportDialog();

  void fireOnSelectProfileOnNavigator(int profileId);
}
