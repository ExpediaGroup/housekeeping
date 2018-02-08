# Housekeeping

# Start using
You can obtain Housekeeping from Maven Central :

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.hotels/housekeeping/badge.svg?subject=com.hotels:housekeeping)](https://maven-badges.herokuapp.com/maven-central/com.hotels/beeju) [![Build Status](https://travis-ci.org/HotelsDotCom/housekeeping.svg?branch=master)](https://travis-ci.org/HotelsDotCom/housekeeping) [![Coverage Status](https://coveralls.io/repos/github/HotelsDotCom/housekeeping/badge.svg?branch=master)](https://coveralls.io/github/HotelsDotCom/housekeeping) ![GitHub license](https://img.shields.io/github/license/HotelsDotCom/housekeeping.svg)

# Overview
A database-backed module that stores orphaned paths in a table for later clean up.

# Configuration
The  housekeeping module defaults to using the H2 Database Engine, however this module can be configured
to use any flavour of SQL that is supported by JDBC, Spring Boot and Hibernate. Using a database which is not in memory
should be preferred when spinning up short-lived instances for jobs before tearing them down. This way the orphaned data
will still be cleaned from S3, even if the cluster ceases to exist.

## Database Connectors
In order to connect to your SQL database, you must place a database connector jar that is compatible with your Database onto your application's classpath.

## Spring YAML Housekeeping Configuration
If your project utilises Spring YAML you can define your Housekeeping within the YAML. For example:

    housekeeping:
      # Name of the schema/database to use -defaults to housekeeping 
      schema-name: housekeeping
      # Location of the script file to initialize the schema - defaults to classpath:/schema.sql 
      db-init-script: housekeeping
      # Connection details
      data-source:
        # The package of your driver class
        driver-class-name: com.mysql.cj.jdbc.Driver
        # JDBC URL for your Database
        url: jdbc:mysql://housekeeping.foo1baz123.us-east-1.rds.amazonaws.com:3306/housekeeping_db
        # Database Username
        username: bdp
        # Database Password
        password: Ch4ll3ng3

## Programmatic Housekeeping Configuration
Housekeeping allows you to configure your housekeeping job in a more fine grained manner by providing a certain set of Spring beans in your application.

You can configure your housekeeping data source in code by defining the bean `DataSource housekeepingDataSource(...)`.
For example:

      @Bean(destroyMethod = "close")
      DataSource housekeepingDataSource(
        String driverClassName,
        String jdbcUrl,
        String username,
        String encryptedPassword) {
        return DataSourceBuilder
          .create()
          .driverClassName(driverClassName)
          .url(jdbcUrl)
          .username(username)
          .password(encryptedPassword)
          .build();
      }

Housekeeping comes with a default `HousekeepingService` implementation, however you can choose to provide your own implementation. To
run housekeeping you must provide a `HousekeepingService` bean which either constructs the default `FileSystemHousekeepingService` or a custom implementation of the `HousekeepingService` interface.

      @Bean
      HousekeepingService housekeepingService(
          LegacyReplicaPathRepository legacyReplicaPathRepository) {
        return new FileSystemHousekeepingService(legacyReplicaPathRepository, new org.apache.hadoop.conf.Configuration());
      }

The housekeeping support provided defaults to creating a database of the name `housekeeping` and a table of the name `legacy_replica_path` within which housekeeping data is stored. To enable this database you must provide a schema.sql file on your classpath. This file contains any SQL code which must be run to initialise your database upon application startup. This is particularly important if running Housekeeping in your application for the first time.

An example schema.sql file for use with the default housekeeping entity configuration is given below:

      CREATE SCHEMA IF NOT EXISTS housekeeping;


Applications which leverage housekeeping support can define their own schema and table within which housekeeping data is to be stored. This can be achieved by following the steps below.

You must create your database initialisation schema.sql script, and add it to your classpath (or alternatively provide it as a resource in your application).
The simplest schema.sql initialisation script will create your schema if it does not exist.

       CREATE SCHEMA IF NOT EXISTS my_custom_schema;

Within your application you will need to extend two base classes provided in the Housekeeping project: `com.hotels.housekeeping.model.LegacyReplicaPath` and `com.hotels.housekeeping.repository.LegacyReplicaPathRepository`.
These classes provide default implementations of the LegacyReplicaPath `Entity` and the LegacyReplicaPathRepository `CrudRepository`.

The `LegacyReplicaPath` should be extended when you wishes to store your legacy paths in a database schema with a name other than `housekeeping`, with a table name other than `legacy_replica_path` or both. The class which extends LegacyReplicaPath must be annotated with the `@Entity` annotation and the `@Table` annotation. An example is given below which will provide the basis for creating a schema named `my_custom_schema` in your database, and a table named `legacy_replica_path` within the `my_custom_schema` schema.  

      @Entity
      @Table(schema = "my_custom_schema", name = "legacy_replica_path",
          uniqueConstraints = @UniqueConstraint(columnNames = { "path", "creation_timestamp" }))
      public class MyJobsLegacyReplicaPath extends EntityLegacyReplicaPath {

        protected MyJobsLegacyReplicaPath() {}

        public MyJobsLegacyReplicaPath(String path) {
          this.path = path;
          this.eventId = "";
          this.pathEventId = "";
        }
      }

To accompany the custom `LegacyReplicaPath` implementation you should extend the `LegacyReplicaPathRepository` interface providing the custom `LegacyReplicaPath` implementation as a generic type argument. This simplifies the creation of a `CrudRepository` for your `LegacyReplicaPath`.
For example:

      public interface MyJobLegacyReplicaPathRepository
          extends LegacyReplicaPathRepository<MyJobsLegacyReplicaPath> {
      }

Whether you are using a custom housekeeping configuration, or the defaults, your application must provide two crucial annotations which will load the Entities and CrudRepositories that you require. These are the `@EntityScan` and `@EnableJpaRepositories` annotations. These annotations are best demonstrated in an example:

    //The class annotated with `@Entity` that defines the required LegacyReplicaPath implementation
    @EntityScan(basePackageClasses = HousekeepingLegacyReplicaPath.class)
    //The class which extends LegacyReplicaPathRepository and contains your desired `CrudRepository` implementation
    @EnableJpaRepositories(basePackageClasses = HousekeepingLegacyReplicaPathRepository.class)


## Password Encryption
Housekeeping allows you to provide encrypted passwords in your configuration or programs. The Housekeeping project depends on the [jasypt library](http://www.jasypt.org/download.html) that can be used to generate encrypted passwords which in turn can be decrypted by Spring Boot's jasypt support.

An encrypted password can be generated by doing the following:

    java -cp jasypt-1.9.2.jar  org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI input="Ch4ll3ng3" password=db_password algorithm=PBEWithMD5AndDES

    ----ENVIRONMENT-----------------

    Runtime: Oracle Corporation OpenJDK 64-Bit Server VM 25.121-b13


    ----ARGUMENTS-------------------

    algorithm: PBEWithMD5AndDES
    input: Ch4ll3ng3
    password: db_password


    ----OUTPUT----------------------

    EHL/foiBKY2Ucy3oYmxdkFiXzWnOu7by

The 'input' is your database password. The 'password' is a password specified by you that can be used to decrypt the data.
The 'output' is your encrypted password. This encrypted password can then be used in the yaml configuration:

    housekeeping:
      data-source:
        #The package of your driver class
        driver-class-name: com.mysql.cj.jdbc.Driver
        #JDBC URL for your Database
        url: jdbc:mysql://housekeeping.foo1baz123.us-east-1.rds.amazonaws.com:3306/housekeeping_db
        #Database Username
        username: bdp
        #Encrypted Database Password
        password: ENC(EHL/foiBKY2Ucy3oYmxdkFiXzWnOu7by)

Or be decrypted from special properties file(s) on your classpath:

        @Configuration
	      @EncryptablePropertySources({@EncryptablePropertySource("classpath:encrypted.properties"), @EncryptablePropertySource("classpath:encrypted2.properties")})
	      public class MyApplication {
		         ...
	      }

The encrypted.properties file would look something like this:

        database.username=ENC(nrmZtkF7T0kjG/VodDvBw93Ct8EgjCA+)
        database.password=ENC(EHL/foiBKY2Ucy3oYmxdkFiXzWnOu7by)

You can then access the decrypted username and password in your application by doing something akin to the following:

        private @Autowired ConfigurableEnvironment env;

        @Bean(destroyMethod = "close")
        DataSource housekeepingDataSource(
          String driverClassName,
          String jdbcUrl) {

          String username = env.getProperty("database.username");
          String password = env.getProperty("database.password");

          return DataSourceBuilder
            .create()
            .driverClassName(driverClassName)
            .url(jdbcUrl)
            .username(username)
            .password(password)
            .build();
        }

Or

        @Bean(destroyMethod = "close")
        DataSource housekeepingDataSource(
          String driverClassName,
          String jdbcUrl,
          @Value("${database.username}") username,
          @Value("${database.password}") password) {
          return DataSourceBuilder
            .create()
            .driverClassName(driverClassName)
            .url(jdbcUrl)
            .username(username)
            .password(password)
            .build();
        }


Finally, if you are using an encrypted password, when you run your application you must provide the application with your
jasypt.encryptor.password 

There are a few approaches to doing so:

Run your application with the jasypt.encryptor.password parameter:

    java jar <your-jar>  --jasypt.encryptor.password=db_password

Pass jasypt.encryptor.password as a system property by creating application.properties or application.yml and adding:

    jasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD:}

Or in YAML

    jasypt:
      encryptor:
        password: ${JASYPT_ENCRYPTOR_PASSWORD:}

# Legal
This project is available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).

Copyright 2016-2018 Expedia Inc.
