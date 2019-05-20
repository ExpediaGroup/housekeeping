# Housekeeping

# Start using
You can obtain Housekeeping from Maven Central :

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.hotels/housekeeping/badge.svg?subject=com.hotels:housekeeping)](https://maven-badges.herokuapp.com/maven-central/com.hotels/housekeeping) [![Build Status](https://travis-ci.org/HotelsDotCom/housekeeping.svg?branch=master)](https://travis-ci.org/HotelsDotCom/housekeeping) [![Coverage Status](https://coveralls.io/repos/github/HotelsDotCom/housekeeping/badge.svg?branch=master)](https://coveralls.io/github/HotelsDotCom/housekeeping) ![GitHub license](https://img.shields.io/github/license/HotelsDotCom/housekeeping.svg)

# Overview
A database-backed module that stores orphaned paths in a table for later clean up.

# Housekeeping Configuration
The housekeeping module defaults to using the H2 Database Engine, however this module can be configured to use any flavour of SQL database that is supported by JDBC, Spring Boot and Hibernate. Using a database which is not in-memory should be preferred when spinning up short-lived instances for jobs before tearing them down. This ensures that the orphaned data will be stored in a persistent database and will be considered for housekeeping even if the cluster ceases to exist.

## Database Connectors
In order to connect to your SQL database, you must place a database connector jar that is compatible with your database onto your application's classpath.

## Spring YAML Housekeeping Configuration
If your project utilises Spring YAML you can define your Housekeeping within the YAML. For example, Housekeeping can be set up to use a MySQL schema:

```yaml
housekeeping:
  # Name of the schema/database to use - defaults to housekeeping 
  schema-name: my_db
  # Connection details
  data-source:
    # The name of your JDBC Driver class  
    driver-class-name: org.mariadb.jdbc.Driver
    # JDBC URL for your database
    url: jdbc:mysql://foo1baz123.us-east-1.rds.amazonaws.com:3306/${housekeeping.schema-name}
    # Database username
    username: bdp
    # Database password
    password: Ch4ll3ng3
```

Notes:
* Some users have seen issues using the MySQL driver on AWS Aurora. [AWS recommends](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.Connecting.html) to use the MariaDB driver for those use cases.
* To use MySQL and similar database systems, the schema specified in the configuration needs to exist, as the value for `housekeeping.data-source.url` needs to be a valid URI.

Houseekeping can also be set up to use the default database engine (H2) and schema:

```yaml
housekeeping:
  schema-name: my_db
  db-init-script: classpath:/schema.sql
  data-source:
    username: bdp
    password: Ch4ll3ng3
```

If the schema does not already exist and the `db-init-script` is not in the default location (`classpath:/schema.sql`), then a custom path can be provided to initialise it, as shown in the following example:

```yaml
housekeeping:
  schema-name: my_db
  db-init-script: file:///tmp/schema.sql
...
```

Where `/tmp/schema.sql` contains: `CREATE SCHEMA IF NOT EXISTS my_db;`

Full list of configuration options:

|Property|Required|Description|
|----|----|----|
|`housekeeping.expired-path-duration`|No|Time To Live (TTL) of legacy replica paths in [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601#Durations) format: only days, hours, minutes and seconds can be specified in the expression.|
|`housekeeping.schema-name`|Yes|Database schema name to use. Tables will be created or used (if already existing) using this schema. Default: 'housekeeping'|
|`housekeeping.db-init-script`|No|Database init script to use.|
|`housekeeping.data-source.driver-class-name`|No|Java classname of the database JDBC driver.|
|`housekeeping.data-source.url`|No|JDBC connection URL.|
|`housekeeping.h2.database`|No|If the `housekeeping.data-source.url` is not overridden then the default H2 database can be configured using this property which also controls where H2 will write its database files. Defaults to `${instance.home}/data/${instance.name}/${housekeeping.schema-name}` (where `instance.home`, `instance.name` and `housekeeping.schema-name` can be configured separately for more fine-grained control).|
|`housekeeping.data-source.username`|No|Database user with access to schema.|
|`housekeeping.data-source.password`|No|Database user's password.|
|`housekeeping.fetch-legacy-replica-path-page-size`|No|Number of paths to fetch on each call to the database. Tune this if you run out of memory or if the query seems too slow. The higher the number, the more memory is required. Default: '500'|
|`housekeeping.cleanup-threads`|No|Number of threads used to cleanup the files. Default: '10'|



You can override Spring Boot (HikariCP/Hibernate) settings in the YAML by providing the relevant properties. Housekeeping defaults are added with lower precedence. For example, to override the default connection pool maximum active size to 5 add this:

```yaml
spring.datasource.max-active: 5 
```

Refer to the Spring Boot documentation for a full list of properties that can be set.   
    
## Programmatic Housekeeping Configuration
Housekeeping allows you to configure your housekeeping job in a more fine-grained manner by providing a certain set of Spring beans in your application.

You can configure your housekeeping data source in code by defining the bean `DataSource housekeepingDataSource(...)`. For example:

```java
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
```

Housekeeping comes with a default `HousekeepingService` implementation, however you can choose to provide your own. To run housekeeping you must provide a `HousekeepingService` bean which either constructs the default `FileSystemHousekeepingService` or a custom implementation of the `HousekeepingService` interface:

```java
@Bean
HousekeepingService housekeepingService(
    LegacyReplicaPathRepository legacyReplicaPathRepository, Housekeeping housekeeping) {
  return new FileSystemHousekeepingService(legacyReplicaPathRepository, new org.apache.hadoop.conf.Configuration(), housekeeping.getFetchLegacyReplicaPathPageSize());
}
```

The default provided housekeeping implementation creates a database named `housekeeping` and a table named `legacy_replica_path` to store the housekeeping data. To enable this database you must provide a _schema.sql_ file which contains any SQL code that must be run to initialise your database upon application startup. This is particularly important if running Housekeeping in your application for the first time.

An example _schema.sql_ file for use with the default housekeeping entity configuration is given below:

```sql
CREATE SCHEMA IF NOT EXISTS housekeeping;
```

Applications which leverage housekeeping support can define their own schema and table within which housekeeping data is to be stored. This can be achieved by following the steps below.

You must create your database initialisation _schema.sql_ script and either add it to your classpath, provide it as a resource in your application or configure the path to it via the YAML configuration property `housekeeping.db-init-script`. The simplest _schema.sql_ initialisation script will create your schema if it does not exist.

```sql
CREATE SCHEMA IF NOT EXISTS my_custom_schema;
```

The database name must be configured in the YAML property `housekeeping.schema-name`.

Whether you are using a custom housekeeping configuration, or the defaults, your application must provide two crucial annotations which will load the Entities and CrudRepositories that you require. These are the `@EntityScan` and `@EnableJpaRepositories` annotations. These annotations are best demonstrated in an example:

```java
//The class annotated with `@Entity` that defines the required LegacyReplicaPath implementation
@EntityScan(basePackageClasses = HousekeepingLegacyReplicaPath.class)
//The class which extends LegacyReplicaPathRepository and contains your desired `CrudRepository` implementation
@EnableJpaRepositories(basePackageClasses = HousekeepingLegacyReplicaPathRepository.class)
```

### Customising the Housekeeping table name

By default Housekeeping will create a `legacy_replica_path` table in the specified schema. If you need to customize the table name you can do this by extending the base classes and configuring the JPA annotations as desired. The class which extends `EntityLegacyReplicaPath` must be annotated with the `@Entity` annotation and the `@Table` annotation. An example is given below which will provide the basis for creating a schema named `my_custom_schema` in your database, and a table named `my_custom_replica_path` within the `my_custom_schema` schema.  

```java
@Entity
@Table(schema = "my_custom_schema", name = "my_custom_replica_path",
       uniqueConstraints = @UniqueConstraint(columnNames = { "path", "creation_timestamp" }))
public class MyJobsLegacyReplicaPath extends EntityLegacyReplicaPath {
  //required inherited constructors etc. go here
}
```

To accompany the custom `EntityLegacyReplicaPath` implementation you need to extend the `LegacyReplicaPathRepository` interface providing the custom `EntityLegacyReplicaPath` implementation as a generic type argument. This simplifies the creation of a `CrudRepository` for your `EntityLegacyReplicaPath`. For example:

```java
public interface MyJobLegacyReplicaPathRepository
    extends LegacyReplicaPathRepository<MyJobsLegacyReplicaPath> {
}
```

## Password Encryption
Housekeeping allows you to provide encrypted passwords in your configuration or programs. The Housekeeping project depends on the [jasypt library](http://www.jasypt.org/download.html) that can be used to generate encrypted passwords which in turn can be decrypted by Spring Boot's jasypt support.

An encrypted password can be generated by doing the following:

```
java -cp jasypt-1.9.2.jar  org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI input="Ch4ll3ng3" password=db_password algorithm=PBEWithMD5AndDES

----ENVIRONMENT-----------------

Runtime: Oracle Corporation OpenJDK 64-Bit Server VM 25.121-b13


----ARGUMENTS-------------------

algorithm: PBEWithMD5AndDES
input: Ch4ll3ng3
password: db_password


----OUTPUT----------------------

EHL/foiBKY2Ucy3oYmxdkFiXzWnOu7by
```

The 'input' is your database password. The 'password' is a password specified by you that can be used to decrypt the data.
The 'output' is your encrypted password. This encrypted password can then be used in the yaml configuration:

```yaml
housekeeping:
  data-source:
    #The package of your driver class
    driver-class-name: org.mariadb.jdbc.Driver
    #JDBC URL for your Database
    url: jdbc:mysql://housekeeping.foo1baz123.us-east-1.rds.amazonaws.com:3306/housekeeping_db
    #Database Username
    username: bdp
    #Encrypted Database Password
    password: ENC(EHL/foiBKY2Ucy3oYmxdkFiXzWnOu7by)
```

Or be decrypted from special properties file(s) on your classpath:

```java
@Configuration
@EncryptablePropertySources({@EncryptablePropertySource("classpath:encrypted.properties"), @EncryptablePropertySource("classpath:encrypted2.properties")})
public class MyApplication {
  ...
}
```

The encrypted.properties file would look something like this:

```ini
database.username=ENC(nrmZtkF7T0kjG/VodDvBw93Ct8EgjCA+)
database.password=ENC(EHL/foiBKY2Ucy3oYmxdkFiXzWnOu7by)
```

You can then access the decrypted username and password in your application by doing something akin to the following:

```java
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
```

Or

```java
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
```

Finally, if you are using an encrypted password, when you run your application you must provide the application with your `jasypt.encryptor.password`.

There are a few approaches to doing so:

Run your application with the `jasypt.encryptor.password` parameter:

```bash
java jar <your-jar>  --jasypt.encryptor.password=db_password
```

Pass `jasypt.encryptor.password` as a system property by creating application.properties or application.yml and adding:

```ini
jasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD:}
```

Or in YAML

```yaml
jasypt:
  encryptor:
    password: ${JASYPT_ENCRYPTOR_PASSWORD:}
```

# Housekeeping Vacuum Tool

Housekeeping also provides a "Vacuum" tool that can be run against a Hive table to detect any orphaned data that is available for Housekeeping. For more information on running and configuring this refer to the [housekeeping-vacuum-tool documentation](housekeeping-vacuum-tool/README.md).

# Legal
This project is available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).

Copyright 2016-2019 Expedia Inc.
