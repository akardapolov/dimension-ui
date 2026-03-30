---
name: runtime-diagnosis
description: Diagnoses Dimension UI crashes, hangs, and unexpected runtime behavior. Use this skill when the application crashes on startup, charts don't render, data doesn't load, or analysis features fail.
---

# Runtime Diagnosis

## Evidence Collection

1. Check Java version: `java -version` (must be 25+)
2. Verify Maven build: `mvn clean compile`
3. Check LaF parameter: `-DLaF=dark|light|default`
4. Review logs for EventBus errors
5. Verify Dimension-DI, Dimension-DB, and Dimension-TT installed

## Decision Matrix

| Symptom                | First Check                              |
|------------------------|------------------------------------------|
| Crash on startup       | Java version + dependencies installed    |
| Chart rendering issues | JFreeChart-FSE module + LaF theme        |
| Data not loading       | JDBC/HTTP connection configuration       |
| Analysis not working   | Matrix Profile/ARIMA algorithms loaded   |