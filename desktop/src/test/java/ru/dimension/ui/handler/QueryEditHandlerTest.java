package ru.dimension.ui.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.HandlerMock;
import ru.dimension.ui.model.config.Query;

@Log4j2
public class QueryEditHandlerTest extends HandlerMock {

  @Test
  public void create_new_query_test() {
    assertNotNull(queryButtonPanelHandlerLazy.get());
    assertNotNull(queryPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(configViewLazy.get());

    String queryName = "Test";
    String queryDescription = "Description Test";

    Query query = new Query();
    query.setName(queryName);
    query.setDescription(queryDescription);

    createQueryTest(query);

    assertEquals(queryName, getQueryName(queryName));
    assertEquals(queryDescription, getQueryDescription(queryName));
  }

  @Test
  public void copy_new_query_test() {
    assertNotNull(queryButtonPanelHandlerLazy.get());
    assertNotNull(queryPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(configViewLazy.get());

    String queryName = "Test1";
    String queryDescription = "Description Test";

    Query query = new Query();
    query.setName(queryName);
    query.setDescription(queryDescription);

    createQueryTest(query);

    buttonQueryPanelMock.getBtnCopy().doClick();
    buttonQueryPanelMock.getBtnSave().doClick();

    String queryNameCopy = queryName + "_copy";
    String queryDescriptionCopy = queryDescription + "_copy";
    assertEquals(queryNameCopy, getQueryName(queryNameCopy));
    assertEquals(queryDescriptionCopy, getQueryDescription(queryNameCopy));
  }

  @Test
  public void copy_new_query_duplicate_test() {
    assertNotNull(queryButtonPanelHandlerLazy.get());
    assertNotNull(queryPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(configViewLazy.get());

    String queryName = "Test3";

    Query query = new Query();
    query.setName(queryName);

    createQueryTest(query);

    buttonQueryPanelMock.getBtnCopy().doClick();

    queryPanelLazy.get().getMainQueryPanel().getQueryName().setText(queryName);

    disposeWindows();

    assertThrows(NotFoundException.class, buttonQueryPanelMock.getBtnSave()::doClick,
                 String.format("Name %s already exists, please enter another one.", queryName));
  }

  private String getQueryName(String queryNameCopy) {
    List<Query> queryList = configurationManagerLazy.get().getConfigList(Query.class);

    return queryList.stream()
        .filter(f -> f.getName().equals(queryNameCopy))
        .findAny()
        .orElseThrow()
        .getName();
  }

  private String getQueryDescription(String queryNameCopy) {
    return configurationManagerLazy.get().getConfig(Query.class, queryNameCopy).getDescription();
  }
}
