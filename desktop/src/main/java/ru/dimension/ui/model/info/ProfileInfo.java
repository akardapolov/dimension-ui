package ru.dimension.ui.model.info;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.dimension.ui.model.RunStatus;

@Data
@Accessors(chain = true)
public class ProfileInfo {

  private int id;
  private String name;
  private String description;
  private RunStatus status;
  private List<Integer> taskInfoList;
}
