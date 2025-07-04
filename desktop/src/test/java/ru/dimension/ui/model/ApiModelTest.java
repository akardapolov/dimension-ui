package ru.dimension.ui.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.model.config.Task;


@Log4j2
public class ApiModelTest {

  @Test
  public void getQueryListByConnection() {
    Task task1 = new Task();
    task1.setConnectionId(2);
    task1.setQueryList(List.of(1, 2, 3));
    Task task2 = new Task();
    task2.setConnectionId(2);
    task2.setQueryList(List.of(1, 2, 3));

    List<Task> list = new ArrayList<>();
    list.add(task1);
    list.add(task2);

    List<Integer> queryList = list
        .stream()
        .filter(f -> f.getConnectionId() == 2)
        .flatMap(t -> t.getQueryList().stream())
        .distinct()
        .toList();

    assertEquals(3, queryList.size());
  }
}
