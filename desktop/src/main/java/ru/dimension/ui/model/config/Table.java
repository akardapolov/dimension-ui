package ru.dimension.ui.model.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import ru.dimension.db.model.profile.TProfile;

public class Table extends TProfile {

  @Getter
  @Setter
  List<String> dimensionColumnList;

  @Override
  public String toString() {
    return "Table{" +
        "tableName='" + getTableName() + '\'' +
        ", tableType=" + getTableType() +
        ", indexType=" + getIndexType() +
        ", compression=" + getCompression() +
        ", cProfiles=" + getCProfiles() +
        ", dimensionColumnList=" + getDimensionColumnList() +
        '}';
  }
}
