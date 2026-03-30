# Desktop Java Module Rules

## MVP Pattern (mandatory for all new UI modules)

Structure for each GUI module in `component/module/<name>/`:
- **Model**: Data/state in `*Model` class
- **View**: Swing UI implementing `*View` interface, never accesses data directly
- **Presenter**: Business logic in `*Presenter` class, coordinates Model and View

## Communication

- Presenter subscribes to EventBus (`@Handler`) for cross-module events
- Presenter uses EventListener for lifecycle callbacks (toolbar, config, workspace state)
- Use MessageBroker for chart-related panel actions within workspace/dashboard

## Swing Threading

- All UI updates from background threads MUST go through `SwingUtilities.invokeLater`
- EventBus has async dispatch — handlers touching Swing MUST respect EDT

## LaF Theme Support

- Supported parameters: `-DLaF=dark|light|default`
- All custom components must respect FlatLaf color scheme
- Test UI changes under both dark and light themes
