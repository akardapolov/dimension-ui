# Dimension UI

## Contents

- [Program description](#program-description)
    - [General information](#general-information)
    - [Application Areas](#application-areas)
    - [Minimum technical requirements](#minimum-technical-requirements)
    - [Getting started with the project](#getting-started-with-the-project)
        - [Building the Project](#building-the-project)
        - [Installation and configuration](#installation-and-configuration)
- [Configuration management](#configuration-management)
- [Data collection](#data-collection)
- [Data storage](#data-storage)
- [Templates for data collection](#templates-for-data-collection)
- [Data visualization](#data-visualization)
- [Data analysis](#data-analysis)
- [Reporting system](#reporting-system)
- [Specialised Ad-hoc data queries](#specialised-ad-hoc-data-queries)
- [Algorithm](#algorithm)
- [Application areas](#application-areas)
- [Screencast](#screencast)
- [Support](#support)

## Program Description
**Dimension UI** is a desktop application designed to collect, store, visualize and analyze time series data.

### General Information

Key Features of **Dimension UI**:
- **Real-time data collection and analysis**: Data is collected in real time, enabling users to monitor metrics online, analyze responses to various types of impacts, review historical data, and perform comparative analysis with other indicators.
- **Flexible and rapid configuration of data collection profiles**: This feature is particularly useful when speed is critical, allowing users to quickly gather specialized statistical data for a more detailed evaluation of system or component characteristics.
- **Local storage in the specialized **Dimension DB** database** (project repositories on [**GitFlic**](https://gitflic.ru/project/akardapolov/dimension-db) and [**GitHub**](https://github.com/akardapolov/dimension-db))—a block-columnar database with built-in data compression and deduplication.
- **Advanced time-series data mining capabilities** using [Matrix Profile](https://www.cs.ucr.edu/~eamonn/MatrixProfile.html) and ARIMA.
- **Time-series data visualization** from external databases via JDBC with automatic SQL query generation for data sources (No-code mode).
- **Dashboard system** for displaying metrics from multiple sources in a single interface. Quick access to metrics simplifies user workflows when reviewing large volumes of analytical data.
- **Built-in reporting system** for generating PDF reports based on collected data for further analysis.

[Return to Contents](#Contents)

### Application Areas
1. Monitoring information systems, hardware and software systems, and databases in real time;
2. Evaluation of hardware and software systems during load and stress testing, preparation of reports based on testing results;
3. Monitoring the parameters of the Internet of Things (IoT) devices;
4. Solving the problems of complex monitoring of information security of systems with access to data sources via the JDBC and HTTP API (Prometheus, JSON);
5. Using the application for training in courses related to data processing and analysis, which require quick setup of data collection from test systems, visualization with the ability to perform advanced data analysis to demonstrate certain concepts, for example, for training system administrators, database administrators, developers and analysts;
6. Diagnostics of problems in the operation of complex systems providing APIs for access to monitoring data via JDBC and HTTP (Prometheus, JSON). For example, for detailed diagnostics of the operation of systems and their components in a heterogeneous environment, Java microservices working with a database;
7. Visualization and multidimensional analysis of collected time series data in a local database in real time, historically, and predictive analytics for various types of applications via JDBC and HTTP (Prometheus, JSON);
8. Visualization and multidimensional analysis of time series data for tables and views in external Postgres, Oracle, Microsoft SQL Server, ClickHouse databases via JDBC protocol without writing SQL (No-code mode);

[Return to Contents](#Contents)

## Minimum technical requirements
**Dimension DB** is compatible with Java 21+ and comes with a small set of dependencies.

### Hardware requirements

Table 1. Hardware requirements

| Parameter        | Description                                                                                                                        |
|:-----------------|:-----------------------------------------------------------------------------------------------------------------------------------|
| CPU and RAM      | Processor with a frequency of at least 500 MHz and a memory capacity of at least 250 MB, depending on the volume of processed data |
| CPU architecture | Intel 64-bit (x86_64), AMD 64-bit (x86_64), Arm 64-bit (aarch64)                                                                   |
| Disk             | Disk size depending on the volume of processed data                                                                                |

### Software requirements

Table 2. Software requirements

| Software | Requirements             |
|:---------|:-------------------------|
| Java     | Java version 21+         |
| Maven    | Not lower than version 3 |
| Git      | Latest current version   |
| DBase    | Latest current version   |

### Operating system requirements

Table 3. Operating system requirements

| Software         | Requirements          |
|:-----------------|:----------------------|
| Operating system | Windows, Linux, MacOS |

[Return to Contents](#contents)

### Getting started with the project

## Building the Project

To compile the application into an executable jar file, do the following:

1. Install JDK version 17 or higher, Maven and Git on your local computer:
    ```shell
    java -version  
    mvn -version
    git --version 
    ``` 
2. Download the source codes of the application to your local computer using Git:

    ```shell
    git clone <url>
    cd dimension-ui
    ```

3. Compile the project using Maven:
    ```shell
    mvn clean compile
   ```

4. Execute the Maven command to build an executable jar file with tests running:
    ```shell
     mvn clean package
    ```

[Return to Contents](#contents)

## Installation and configuration

- Windows Platform, run.bat
    ```shell
    SET JAVA_HOME=C:\PROGRAM FILES\JAVA\jdk-17  
    SET JAVA_EXE="%JAVA_HOME%\bin\java.exe"
    chcp 65001
  
    %JAVA_EXE% -Xmx1024m -DtestMode=false -Dfile.encoding=UTF8 -jar desktop-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```
- Linux platform, run.sh
  ```shell
    #!/bin/bash
  
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    export JAVA_EXE=$JAVA_HOME/bin/java
    export LANG=en_US.UTF-8

    $JAVA_EXE -Xmx1024m -DtestMode=false -Dfile.encoding=UTF8 -jar desktop-1.0-SNAPSHOT-jar-with-dependencies.jar
  ```

To start working with the application you need to run the executable file **run.bat/run.sh**

[Return to Contents](#contents)

## Configuration management

The configuration of **Dimension UI** application consists of several main entities including **Profile**, **Task**, **Connection** and **Request**.

- A **Profile** is a repository of information about a particular profile, including its name, a brief description and list of tasks to be performed when you start this profile. <a href="media/config/profile.gif" target="_blank"> Screencast</a>

![Profile](media/config/profile.png#center)

- **Task**, in turn, is an entity that contains a list of queries that must be executed within this task. Each task also contains the name of the request, a brief description, the connection and the frequency of queries to the remote system. <a href="media/config/task.gif" target="_blank">Screencast </a>

![Task](media/config/task.png#center)

- **Connection** is an entity that contains all the necessary information about the details of the connection to the remote JDBC system. Connection attributes: name, URL, username, password, as well as file location information and class name JDBC Driver class. <a href="media/config/connection.gif"  target="_blank">Screencast</a>

![Connection](media/config/connection.png#center)

- **Queries** are textual descriptions of SQL queries that are sent to the server to retrieve data.
  Each query also contains information about the name of the query, a short description, the way of filling the data (locally or on the server)  and data loading mode (direct, JDBC in real time, batch data loading from JDBC source).<a href="media/config/query.gif"  target="_blank">Screencast </a>

![Query main](media/config/query-main.png)

Methods of filling data:

- Locally, when we load data directly from a remote system into the local database;
- On server, when data are filled by separate process, and we only load them locally.

Inside the query interface there are also metadata entities for the local database table and metrics.

- **Metadata** contains information about the table configuration in the local DBase engine data repository according to
  the query data. Table metadata: name, storage type (regular table or table for storing time series data), indexing type
  (local or global), data compression, table column for time tracking, metadata of table columns.  The interface also displays the connection to the job data source in which the query is executed.
  This is needed in order to load metadata on the query into the local storage.

![Query metadata](media/config/query-metadata.png#center)

- **Metrics** is an entity for displaying specially prepared statistics.
  Attributes of metrics: name, X axis (column name), Y axis (column name), grouping of data (by column), function (way of processing of displayed data), way of graphical data displaying (linear, stacked graphs), default value (when displaying detail data for stacked plots). At the bottom there is a list of all the metrics for the given query are displayed at the bottom.

![Query metrics](media/config/query-metrics.png#center)

There are two modes available in the application interface: **View** and **Edit** data.

- In the **View** mode the data is displayed taking into account the hierarchical structure that is formed on the basis of the profiles.
  Each profile can contain several tasks, each of which can include several queries.  Each job is linked to a single connection, which allows you to perform multiple queries to the same data source. <a href="media/config/view.gif"  target="_blank">Screencast </a>

![View](media/config/view-panel.png#center)

- In the **Edit** mode the user has the ability to create new objects, copy, delete and modify existing ones. <a href="media/config/edit.gif"  target="_blank">Screencast</a>

![Edit](media/config/edit-panel.png#center)

The application has functionality that allows you to create a profile using pre-created job templates, connections and requests, which are available in the interface **Settings** and **Templates**.

[Return to Table of Contents](#contents)

## Data collection

The application collects data from remote systems via the JDBC protocol, HTTP (Prometheus) or directly from the application. All data sources for which an appropriate JDBC driver is developed.

The logic for obtaining time series data depends on where the data is collected.

1. If the table data from the remote system is filled on the server, we choose the option **Collect data on the server**. The application tracks the pointer to the timestamp of the last selected record, then loads the data into the local database, that were added on the remote system.
2. The **Collect data on the client** option allows you to collect data from the remote system and store it locally, the timestamps are tracked by the application.

The timestamps are tracked by a column with the data type **Timestamp**, which is defined in the settings by table. In the configuration interface, this setting is located in **Queries** -> **Metadata** -> **Timestamp drop-down list**.

[Return to Table of Contents](#contents)

## Data storage

Data storage in the application is implemented with the use of a specialized storage system of block-column type with compression **Dimension DB** (repositories [**GitFlic**](https://gitflic.ru/project/akardapolov/dimension-db) and [**GitHub**](https://github.com/akardapolov/dimension-db)).

DB settings are located in the **Queries** -> **Metadata** interface. The usual tables are supported and tables for storing time series data. Three types of column data storage are available: RAW, ENUM and HISTOGRAM. The types of column data storage types are determined in the settings at the table or block level (the **Global** or **Local** indexing setting). Local indexing on the block level is a function for automatically selecting the appropriate storage type for the block  based on data distribution. The storage type selection in this option is automatic. Data compression is supported. Settings for data compression and storage type selection are made dynamically.

[Return to Table of Contents](#contents)

## Templates for data collection

The **Templates** interface displays template settings: list of templates by profiles, queries and connections.

![Templates](media/template/template.png)

By clicking on the **Load** button, it is possible to load data on the selected profile, connection and query template into a separate profile, for which its name and description are specified

![Templates edit](media/template/template-edit.png)

Template integration is also built into the application's settings storage and selection system. Interface elements (**Templates tab**, and the **List of templates**)

![Templates in configuration](media/template/template-profile.png)

![Templates in configuration](media/template/template-task.png)

[Return to Table of Contents](#contents)

## Data visualization

The application supports four data display options, which are located in the **Workspace** and **Dashboard** interfaces:

In the **Workspace** interface:

1. Real-time mode, when the data are visualized as they arrive. To display data in this mode, you need to select the appropriate metric or query column in the **Real-time** interface. In the **Details** interface, it is possible to choose **Count**, **Sum** and **Average** functions. For numeric values it is possible any of these functions can be selected, for string data it is not possible to call the sum and average calculation. <a href="media/view/real-time.gif"  target="_blank">Screencast </a>

![Real-time](media/view/real-time.png#center)

2. In the historical section, when the data is displayed for the previous observation period. To do this you need to select a metric or query column and specify the range in **History** interface. The **Custom** field allows for more detailed range selection using the **Relative** and **Absolute** interfaces.

   <a href="media/view/history.gif"  target="_blank"> Screencast</a>

![History](media/view/history.png#center)

3. Ad-hoc queries, when the data is displayed for a particular keyword. To do this, you need to go to the Search interface, specify a substring to search for and click the Go button. <a href="media/view/search.gif"  target="_blank"> Screencast</a>

![Ad hoc query](media/view/search.png#center)

4. Display multidimensional data as a **Pivot** summary report with support for row and column totals.
   
![Stacked pivot](media/view/pivot.png#center)

In the **Dashboard** interface, you can display real-time data while simultaneously tracking multiple selected metrics and columns from running profiles.

![Dashboard](media/view/dashboard.png#center)

[Return to Table of Contents](#contents)

## Data analysis

Data analysis functionality is available in the **Analyze** interface. By selecting a certain range, get the top indicators associated with the selected measurement in the **Visualize** block for all other measurements (for numerical data) and then get a real-time updated graph on them, but with the selected filter:

![Analyze real time](media/analyze/analyze_real_time.png#center)

For history, the display logic is similar, except that the data is not updated, but only the ranges selected in the **Visualize** block are displayed.

![Analyze history](media/analyze/analyze_history.png#center)

It is possible to hide data on indicators in the legend, it is convenient in case of long names of measurement indicators.

To analyse the data, the graph of each selected measurement is divided into blocks: **Data**, **Anomaly** and **Forecast**. The **Data** block displays graphs of the collected data for the selected range, while the **Anomaly** and **Forecast** blocks display the results of data analysis using the connected **Matrix Profile** algorithms and the **Smile** algorithm library in the form of graphs. This type of interface is displayed for each selected measurement.

For the **Anomaly** block, the top part of the interface displays the available algorithms (in the screenshot it is **STAMP**), the left part displays all the available measurement metrics, selecting a value from the list gives the user a **Matrix Profile** calculation graph, where *maximums* at the top of the graph are *anomalies in the data*, *minimums* at the bottom are *repeated values in the metrics*.

For the **Forecast** block, the top part of the interface also shows the available algorithms (in the screenshot it is **ARMA**), the left part shows all available measurement indicators, selecting a value from the list gives the user a graph with data and continued forecast values (dashed line).

![Anomaly](media/analyze/anomaly-forecast.png#center)

For each selected algorithm (for **Anomaly** and for **Forecast**), there is an option to manage its settings through the **Settings** form.

![Settings anomaly](media/analyze/settings_anomaly.png#center) ![Settings forecast](media/analyze/settings_forecast.png#center)

[Return to Table of Contents](#contents)

## Reporting system

The application has a function for creating reports in PDF format.

### Customising report parameters

To customise the report parameters, you need to select the appropriate profile, job and query and then select the corresponding 
metric or query column in the **Report** interface. On the **Design** tab, there is an option to select a date range.
After displaying the design of the future report, you can edit the description, you can select the desired one from the **Count**, **Sum** and
**Average**. For numeric values any of these functions can be selected, for string data it is not possible to call **Count**, **Sum** and **Average** functions.
calculation of the average value.

![Design](media/report/design.png#center)


### Report generation

After the user has configured all the necessary parameters of the report, he starts the process of generating the report using
the button **Report** <a href="media/report/report.gif"  target="_blank">Screencast</a>


### Viewing the report

The created PDF report is displayed on the **Report** tab, designed for viewing reports. The report contains all the data from the design: graphs, tables and comments.

![Report](media/report/report.png#center)

### Exporting a report

The user is given the opportunity to export the report in PDF format. To do this, use the **Save** button to select the directory to save the report file.

[Return to Table of Contents](#contents)

## Specialised Ad-hoc data queries

In the **Ad-Hoc** interface in the left part the list of **JDBC** connections is displayed, by clicking on the connection the information on tables and views used in different database schemes and catalogues is automatically collected, by clicking on each table in the upper part the interface is filled in which the metadata on the table (metrics names, data types for storing temporary metrics) is displayed. By selecting a metric and a column for storing time metrics, you can get a graph of the function for the given query as a result. It is possible to specify a date range (last day, week month or any date range on **Custom**).

By selecting the data range on the **Stacked** graph, the detail is displayed in the form of **Gantt** and **Pivot** graphs for the selected dependent main metric

![Ad-Hoc](media/adhoc/adhoc.png#center)

[Return to Table of Contents](#contents)

## Algorithm
```mermaid
sequenceDiagram
    participant User as User
    participant Program as Program
    participant Profile as Profile
    participant Task as Task
    participant Workspace as Workspace
    participant Metrics as Metrics
    participant Reporting as Reporting
    participant Adhoc as Ad-hoc Queries

    User->>Program: Launch
    User->>Profile: Create Profile
    alt Manual Mode
        User->>Task: Fill Out Task
        Task->>Task: Bind Connection
        Task->>Task: Specify Requests
    else Use Templates
        User->>Profile: Fill Profile Using Templates
    end
    User->>Workspace: Go to Profile
    Workspace->>Profile: Launch with Start Button
    User->>Metrics: View Metrics
    Metrics->>Metrics: Compare
    Metrics->>User: Launch Analysis / View History
    User->>Metrics: Select Metrics from Various Profiles
    User->>Reporting: Go to Reporting Tab
    Reporting->>User: Select Metrics for Report
    User->>Adhoc: Go to Ad-hoc Queries
    Adhoc->>User: Select Connection, Table/View
    Adhoc->>User: Display Data and Analyze
    User->>Program: Complete Program
```
[Return to Table of Contents](#contents)

## Application areas
1. monitoring of information systems, software and hardware systems and databases in real time;
2. assessment of the operation of software and hardware systems during load and stress testing, preparation of reports on testing results;
3. monitoring the parameters of Internet of things (IoT) devices;
4. solving problems of comprehensive monitoring of information security of systems with access to data sources via JDBC and HTTP API (Prometheus, JSON);
5. use of the application for training in courses related to data processing and analysis, which require quick setup of data collection from test systems, visualization with the possibility of advanced data analysis to demonstrate certain concepts, for example, for training system administrators, database administrators, developers and analysts;
6. diagnosing problems in the operation of complex systems that provide APIs for accessing monitoring data via JDBC and HTTP (Prometheus, JSON). For example, for detailed diagnostics of the operation of systems and their components in a heterogeneous environment, microservices in Java working with a database;
7. visualization and multidimensional analysis of collected time series data in a local database in real time, historical context and predictive analytics for various types of applications via JDBC and HTTP (Prometheus, JSON);
8. visualization and multidimensional analysis of time series data for tables and views in external databases Postgres, Oracle, Microsoft SQL Server, ClickHouse via the JDBC protocol without writing SQL (No-code mode);

[Вернуться в оглавление](#содержание)

## Screencast

|               | Screencast                                                                   |
|:--------------|:-----------------------------------------------------------------------------|
| Configuration | <a href="media/config/configuration.gif"  target="_blank">Configuration </a> |
| Workspace     | <a href="media/workspace/workspace.gif"  target="_blank">Workspace </a>      |
| Dashboard     | <a href="media/dashboard/dashboard.gif"  target="_blank">Dashboard </a>      |
| Report        | <a href="media/report/report.gif"  target="_blank">Report </a>               |   
| Ad-hoc        | <a href="media/adhoc/adhoc.gif"  target="_blank">Ad-hoc </a>                 |   

[Return to Table of Contents](#contents)

## Support
Created with support of ["Innovation Promotion Fund"](https://fasie.ru/) by competition ["Code-Digital Technologies"](https://fasie.ru/press/fund/kod-dt/) - ["Results"](https://fasie.ru/press/fund/kod-dt-results/) within the framework of the national program [“Digital Economy of the Russian Federation”](https://digital.gov.ru/ru/activity/directions/858/).

[Return to Table of Contents](#contents)

## Contact
[@akardapolov](mailto:akardapolov@yandex.ru)

[Return to Table of Contents](#contents)