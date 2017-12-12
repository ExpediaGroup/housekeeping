# ``Housekeeping

## Overview
A database-backed module that stores orphaned replica paths in a table for later clean up.

## Configuration
The  housekeeping module defaults to using the H2 Database Engine, however this module can be configured 
to use any flavour of SQL that is supported by JDBC, Spring Boot and Hibernate. Using a database which is not in memory
should be preferred when temporarily spinning up instances for jobs before tearing them down. This way the orphaned data 
will still be cleaned from S3, even if the cluster ceases to exist.

### Example Configuration Using YAML File:
    
    housekeeping:
      data-source: 
        #The package of your driver class 
        driver-class-name: com.mysql.cj.jdbc.Driver
        #JDBC URL for your Database
        url: jdbc:mysql://housekeeping.foo1baz123.us-east-1.rds.amazonaws.com:3306/housekeeping_schema
        #Database Username
        username: bdp
        #Database Password
        password: Ch4ll3ng3

In order to connect to your SQL database, you must place a database connector jar that is compatible with your Database onto your classpath.

If no housekeeping configuration is provided,  will default to using an in memory H2 Database.
