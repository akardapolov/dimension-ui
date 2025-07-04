package ru.dimension.ui.model.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.dimension.db.model.profile.CProfile;

@EqualsAndHashCode(callSuper = true)
@Data
public class CProfileReport extends CProfile {

  private String comment;
}
