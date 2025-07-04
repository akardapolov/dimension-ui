package ru.dimension.ui.model.config;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EntityList<T> {

  private List<T> entityList;
}
