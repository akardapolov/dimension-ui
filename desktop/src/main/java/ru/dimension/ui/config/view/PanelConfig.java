package ru.dimension.ui.config.view;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.ui.view.panel.config.connection.ConnectionPanel;
import ru.dimension.ui.view.panel.config.profile.ProfilePanel;
import ru.dimension.ui.view.panel.config.query.QueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;
import ru.dimension.ui.view.panel.template.TemplateConnPanel;
import ru.dimension.ui.view.panel.template.TemplateEditPanel;
import ru.dimension.ui.view.panel.template.TemplateHTTPConnPanel;

@Module
public abstract class PanelConfig {

  @Binds
  @Named("profileConfigPanel")
  public abstract ProfilePanel bindProfilePanel(ProfilePanel profilePanel);

  @Binds
  @Named("taskConfigPanel")
  public abstract TaskPanel bindTaskPanel(TaskPanel taskPanel);

  @Binds
  @Named("connectionConfigPanel")
  public abstract ConnectionPanel bindConnectionPanel(ConnectionPanel connectionPanel);

  @Binds
  @Named("queryConfigPanel")
  public abstract QueryPanel bindQueryPanel(QueryPanel queryPanel);

  @Binds
  @Named("templateConnPanel")
  public abstract TemplateConnPanel bindTemplateConnPanel(TemplateConnPanel templateConnPanel);

  @Binds
  @Named("templateHTTPConnPanel")
  public abstract TemplateHTTPConnPanel bindTemplateHTTPConnPanel(TemplateHTTPConnPanel templateHTTPConnPanel);

  @Binds
  @Named("templateEditPanel")
  public abstract TemplateEditPanel bindTemplateEditPanel(TemplateEditPanel templateEditPanel);
}
