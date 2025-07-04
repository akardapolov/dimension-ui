<!DOCTYPE html>
<html lang="ru">
    <head>

        <meta charset="UTF-8"></meta>
        <title>Report</title>

        <style>
            h1 {
                color: #ccc;
            }

            .table-ptq tr td {
                text-align: center;
                border: 1px solid gray;
                padding: 4px;
            }

            .table-ptq tr th {
                background-color: #84C7FD;
                color: #fff;
                width: 100px;
            }


            .itext-left {
                color: #84C7FD;
                font-weight: bold;
                text-align: left;
                margin-right: -10px;
            }

            .description {
                color: gray;
                font-family: Arial Unicode MS, FreeSans;
                font-size:16px;
                font-weight: normal;
            }

            .img {
                border: 3px solid #ffffff;
                padding: 20px;
            }

            .table-mc tr td {
                color: #84C7FD;
                font-weight: bold;
                text-align: center;
                border: 3px solid #ffffff;
                padding-left: 0;
                padding-top: 20px;
                padding-bottom: 5px;
            }

            .table-desc tr td {
                color: gray;
                text-align: left;
                border: 3px solid #ffffff;
                padding-left: 0;
                padding-top: 15px;
                padding-bottom: 15px;
            }

        </style>

    </head>
    <body>
        <h1>Report for the period </h1>
        <h2> ${dateFrom} - ${dateTo} </h2>

        <table class="table-ptq">
            <tr>
                <th>Profile</th>
                <td>${profileName}</td>
            </tr>
            <tr>
                <th>Task</th>
                <td>${taskName}</td>
            </tr>
            <tr>
                <th>Query</th>
                <td>${queryName}</td>
            </tr>
        </table>


        <table class="table-mc">
            <tr>
                <td>${nameCard}</td>
                <td class="itext-left">${nameFunction} </td>
            </tr>
        </table>
        <table class="table-desc">
            <tr>
                <td>
                    <span class="description"> ${description}</span>
                </td>
            </tr>
        </table>

        <img class="img" src="${pathChart}" alt="chart"></img>
    </body>
</html>