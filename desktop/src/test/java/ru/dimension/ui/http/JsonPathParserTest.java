package ru.dimension.ui.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.collector.collect.json.JsonPathParser;

@Log4j2
public class JsonPathParserTest {

  @Test
  public void jsonPathParserTest() {
    String jsonContent = """
          {
            "id":100,
            "balance":2150.50,
            "name":"John Doe"
          }
        """;

    String jsonPathExpr = "$";

    Map<String, Object> result = JsonPathParser.parseResponseByJsonPath(jsonContent, jsonPathExpr);

    assertEquals(100, result.get("id"));
    assertEquals(2150.50, (Double) result.get("balance"), 0.001); // Using a small delta for double comparison
    assertEquals("John Doe", result.get("name"));
  }
}
