package ru.dimension.ui.bus.event;

import java.util.List;
import ru.dimension.db.model.profile.CProfile;

public record UpdateMetadataColumnsEvent(int queryId, String queryName, List<CProfile> columns) {}