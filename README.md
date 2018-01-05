# Housekeeping

# Start using
You can obtain Housekeeping from Maven Central : 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.hotels/housekeeping/badge.svg?subject=com.hotels:housekeeping)](https://maven-badges.herokuapp.com/maven-central/com.hotels/beeju) [![Build Status](https://travis-ci.org/HotelsDotCom/housekeeping.svg?branch=master)](https://travis-ci.org/HotelsDotCom/housekeeping) [![Coverage Status](https://coveralls.io/repos/github/HotelsDotCom/housekeeping/badge.svg?branch=master)](https://coveralls.io/github/HotelsDotCom/housekeeping) ![GitHub license](https://img.shields.io/github/license/HotelsDotCom/housekeeping.svg)

# Overview
A database-backed module that stores orphaned paths in a table for later clean up.

# Configuration
The  housekeeping module defaults to using the H2 Database Engine, however this module can be configured 
to use any flavour of SQL that is supported by JDBC, Spring Boot and Hibernate. Using a database which is not in memory
should be preferred when temporarily spinning up instances for jobs before tearing them down. This way the orphaned data 
will still be cleaned from S3, even if the cluster ceases to exist.

In order to connect to your SQL database, you must place a database connector jar that is compatible with your Database onto your classpath.

# Legal
This project is available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).

Copyright 2016-2018 Expedia Inc.
