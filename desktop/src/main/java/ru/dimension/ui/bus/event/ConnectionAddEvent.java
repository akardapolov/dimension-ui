package ru.dimension.ui.bus.event;

import ru.dimension.ui.model.type.ConnectionType;

public record ConnectionAddEvent(int connectionId, String connectionName, ConnectionType type) {}