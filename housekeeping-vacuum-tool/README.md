## Housekeeping Vacuum Tool

### Usage

#### YAML Configuration

|Property|Required|Description|
|:----|:----:|:----|
|`catalog.name`|Yes|A name for the source catalog for events and logging.|
|`catalog.hive-metastore-uris`|No|Fully qualified URI of the source cluster's Hive metastore Thrift service. If not specified values are taken from the hive-site.xml on the Hadoop classpath of the machine that's running Housekeeping vacuum tool. This property mimics the Hive property "hive.metastore.uris" and allows multiple comma separated URIs.|
|`catalog.site-xml`|No|A list of Hadoop configuration XML files to add to the configuration for the source.|
|`catalog.configuration-properties`|No|A list of `key: value` pairs to add to the Hadoop configuration for the source.|
|`catalog.metastore-tunnel.route`|No|A SSH tunnel can be used to connect to source metastores. The tunnel may consist of one or more hops which must be declared in this property.|
|`catalog.metastore-tunnel.private-keys`|No|A comma-separated list of paths to any SSH keys required in order to set up the SSH tunnel.|
|`catalog.metastore-tunnel.known-hosts`|No|Path to a known hosts file.|
|`catalog.metastore-tunnel.port`|No|The port on which SSH runs on the source master node. Default is `22`.|
|`catalog.metastore-tunnel.local-host`|No|The address on which to bind the local end of the tunnel. Default is '`localhost`'.|
|`tables.database-name`|Yes| The database name for the table the vacuum tool will interrogate.|
|`tables.table-name`|Yes| The table name for the table the vacuum tool will interrogate.|
|`housekeeping.schema-name`|No|The schema name that is used in your housekeeping instance. Defaults to `housekeeping` |
|`housekeeping.h2.home`|No| The path to your H2 filesystem database specified as a full path. Defaults to `$HOME/data`|
|`housekeeping.datasource.driver-class-name` |No| The fully qualified package of your database driver. Defaults to H2 driver if not configured. |
|`housekeeping.datasource.url` |No| JDBC URL for your database. Defaults to H2 filesystem database if not specified. |
|`housekeeping.datasource.username` |No| Username for your database. |
|`housekeeping.datasource.password` |No| Password for your database. |
|`housekeeping.db-init-script`|No|A file containing a script to initialise your schema can be provided if it does not already exist. Defaults to `classpath:/schema.sql`|


#### Example YAML Configurations:

##### Vacuum Tool configured with MySQL Housekeeping database

In order to use an external JDBC-compliant database, the JDBC driver for this database must be made available on the CLASSPATH of the vacuum tool. 
This can be achieved by one of the following:
* Adding the path to the driver jar file to the Housekeeping bootstrap CLASSPATH (e.g. `export HOUSEKEEPING_CLASSPATH=/path/to/mysql-connector-java-x.y.z.jar`). 
* Placing the driver jar file in `$VACUUM_TOOL_HOME/lib/`.

The configuration then needs to be updated to be something like below:

    catalog:
      name: vacuum_tool
      hive-metastore-uris: thrift://my-metastore-uri:9083

    tables:
    -
      database-name: db
      table-name: table_1
    -
      database-name: db
      table-name: table_2

    housekeeping:
      schema-name: my_db
      dataSource:
        driverClassName: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://db-host:3306/${housekeeping.schema-name}
        username: user
        password: foo


##### Vacuum Tool configured with H2 Housekeeping database

The Vacuum tool already has the required H2 drivers on its CLASSPATH so the only change required to use H2 is to create a configuration file similar to below:

    catalog:
      name: vacuum_tool
      hive-metastore-uris: thrift://my-metastore-uri:9083

    tables:
    -
      database-name: db
      table-name: table_1

    housekeeping:
      schema-name: my_db
      h2:
          # Location of H2 DB on filesystem
          database: /home/hadoop/vacuumtest/mydb/${housekeeping.schema-name}
      dataSource:
          username: user
          password: foo

If the schema does not already exist, a `db-init-script` can be provided to initialise it, as shown in the following example:

    ...
    housekeeping:
      schema-name: my_db
      db-init-script: file:///tmp/schema.sql
    ...

Where `/tmp/schema.sql` contains: `CREATE SCHEMA IF NOT EXISTS my_db;`

#### Performing a Vacuum

Run with your respective YAML configuration file:

    $HOUSEKEEPING_TOOL_HOME/bin/vacuum.sh \
      --config=<your-config>.yml \
      [--dry-run=true] \
      [--partition-batch-size=1000] \
      [--expected-path-count=10000]

The vacuum tool looks for any files and folders in the data locations of your tables that are not referenced in either the metastore or housekeeping database. Any paths discovered are again scheduled for removal via the housekeeping process. The respective files and folders will then be removed at a time determined by the specific configuration of your housekeeping process.

We use the housekeeping process for data removal in this scenario as it has useful logic for determining when ancestral paths can also be removed.

The `dry-run` option allows you to observe the status of paths on the file system, the metastore, and the housekeeping database without performing any destructive changes. The `partition-batch-size` and `expected-path-count` allow you to tune memory demands should you hit heap limits with large numbers of partitions.
