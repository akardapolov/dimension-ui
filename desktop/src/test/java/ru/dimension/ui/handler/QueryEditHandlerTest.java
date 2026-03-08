package ru.dimension.ui.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.HandlerMock;
import ru.dimension.ui.model.config.Query;

@Log4j2
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryEditHandlerTest extends HandlerMock {

  @Test
  @Order(1)
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

    List<Query> queryList = configurationManagerLazy.get().getConfigList(Query.class);
    log.info("Queries after create: {}", queryList);

    Optional<Query> found = queryList.stream()
        .filter(f -> f.getName().equals(queryName))
        .findAny();

    assertTrue(found.isPresent(), "Query with name '" + queryName + "' should exist after creation");
    assertEquals(queryName, found.get().getName());
    assertEquals(queryDescription, found.get().getDescription());
  }

  @Test
  @Order(2)
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

    queryButtonPanelHandlerLazy.get().onCopy();

    withTaskLinkDialogMocked(() -> queryButtonPanelHandlerLazy.get().onSave());

    String queryNameCopy = queryName + "_copy";
    String queryDescriptionCopy = queryDescription + "_copy";

    List<Query> queryList = configurationManagerLazy.get().getConfigList(Query.class);
    log.info("Queries after copy: {}", queryList);

    Optional<Query> found = queryList.stream()
        .filter(f -> f.getName().equals(queryNameCopy))
        .findAny();

    assertTrue(found.isPresent(), "Query with name '" + queryNameCopy + "' should exist after copy");
    assertEquals(queryNameCopy, found.get().getName());
    assertEquals(queryDescriptionCopy, found.get().getDescription());
  }

  @Test
  @Order(3)
  public void copy_new_query_duplicate_test() {
    assertNotNull(queryButtonPanelHandlerLazy.get());
    assertNotNull(queryPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(configViewLazy.get());

    String queryName = "Test3";

    Query query = new Query();
    query.setName(queryName);

    createQueryTest(query);

    queryButtonPanelHandlerLazy.get().onCopy();

    queryPanelLazy.get().getMainQueryPanel().getQueryName().setText(queryName);

    assertThrows(NotFoundException.class,
                 () -> withTaskLinkDialogMocked(() -> queryButtonPanelHandlerLazy.get().onSave()),
                 String.format("Name %s already exists, please enter another one.", queryName));
  }
}