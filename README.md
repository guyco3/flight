Flight booking management system for CSE344 @ UW

# Setup Instructions
1. PostgreSQL Server Setup
Install PostgreSQL if you haven't already and start the PostgreSQL service
2. Database and Table Creation
Create a new database for the flight booking system, then execute the SQL commands in the createTables.sql
3. Configuration
Create a file named dbconn.properties in the root directory of the project with the following contents:
text
# Database connection settings
    flightapp.server_url = <url that postgres server is running>
    flightapp.database_name = <your database name>
    flightapp.username = <postgres username>
    flightapp.password = <postgres password>
    flightapp.tablename_suffix = <tablename suffix (your username)>
Replace the placeholders (enclosed in <>) with your specific configuration details.

# Usage
    $ mvn compile exec:java

