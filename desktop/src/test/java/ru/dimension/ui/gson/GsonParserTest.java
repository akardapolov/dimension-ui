package ru.dimension.ui.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.model.config.Table;
import ru.dimension.ui.model.info.TableInfo;

@Log4j2
public class GsonParserTest {

  @Test
  public void tableTProfileBugTest() throws IOException {
    String jsonData = getTestData("table", "table_tprofile.json");

    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    Table table = gson.fromJson(jsonData, Table.class);
    TableInfo tableInfo = gson.fromJson(jsonData, TableInfo.class);

    String[] array = new String[]{"WAIT_CLASS", "SQL_ID", "SQL_OPNAME"};

    assertEquals(table.getDimensionColumnList(), Arrays.asList(array));
    assertEquals(tableInfo.getDimensionColumnList(), Arrays.asList(array));
  }

  protected String getTestData(String dirName,
                               String fileName) throws IOException {
    return Files.readString(Paths.get("src", "test", "resources", dirName, fileName));
  }
}
