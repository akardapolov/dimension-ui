package ru.dimension.ui.model.info;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.db.model.profile.cstype.CSType;

public class TableInfo extends TProfile {

  public TableInfo() {
  }

  public TableInfo(TProfile profile) {
    this.setTableName(profile.getTableName())
        .setTableType(profile.getTableType())
        .setIndexType(profile.getIndexType())
        .setBackendType(profile.getBackendType())
        .setCompression(profile.getCompression())
        .setCProfiles(profile.getCProfiles());
  }

  public SProfile getSProfile() {
    SProfile sProfile = new SProfile();
    sProfile.setTableName(this.getTableName());
    sProfile.setTableType(this.getTableType());
    sProfile.setIndexType(this.getIndexType());
    sProfile.setBackendType(this.getBackendType());
    sProfile.setCompression(this.getCompression());

    Map<String, CSType> map = new HashMap<>();

    if (this.getCProfiles() != null) {
      this.getCProfiles().forEach(e -> {
        map.put(e.getColName(), e.getCsType());
      });
    }

    sProfile.setCsTypeMap(map);

    return sProfile;
  }

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
