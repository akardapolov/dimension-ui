# Time Series Analysis Rules

- Matrix Profile for anomaly detection and motif discovery (matrix-profile module)
- ARIMA for forecasting (timeseries-forecast module)
- Use Z-normalized Euclidean distance for comparisons
- Algorithm implementations in `algorithm/` subpackages
- Both modules are dependencies of desktop — changes require `mvn -pl desktop -am test`