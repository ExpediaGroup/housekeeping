# Housekeeping Tool

Housekeeping Tool Parent is a sub-module for Housekeeping debug and maintenance tools.

## Housekeeping Vacuum Tool

### Usage

#### YAML Configuration

|Property|Required|Description|
|:----|:----:|:----|
|`catalog.name`|Yes|A name for the source catalog for events and logging.|
|`catalog.hive-metastore-uris`|No|Fully qualified URI of the source cluster's Hive metastore Thrift service. If not specified values are taken from the hive-site.xml on the Hadoop classpath of the machine that's running Circus Train. This property mimics the Hive property "hive.metastore.uris" and allows multiple comma separated URIs.|
|`catalog.site-xml`|No|A list of Hadoop configuration XML files to add to the configuration for the source.|
|`catalog.configuration-properties`|No|A list of `key: value` pairs to add to the Hadoop configuration for the source.|
|`catalog.metastore-tunnel.route`|No|A SSH tunnel can be used to connect to source metastores. The tunnel may consist of one or more hops which must be declared in this property. See [Configuring a SSH tunnel](#configuring-a-ssh-tunnel) for details.|
|`catalog.metastore-tunnel.private-keys`|No|A comma-separated list of paths to any SSH keys required in order to set up the SSH tunnel.|
|`catalog.metastore-tunnel.known-hosts`|No|Path to a known hosts file.|
|`catalog.metastore-tunnel.port`|No|The port on which SSH runs on the source master node. Default is `22`.|
|`catalog.metastore-tunnel.local-host`|No|The address on which to bind the local end of the tunnel. Default is '`localhost`'.|
|`tables.database-name`|Yes| The database name for the table the vacuum tool will interrogate.|
|`tables.table-name`|Yes| The table name for the table the vacuum tool will interrogate.|
|`housekeeping.schema-name`|No|The schema name that is used in your housekeeping instance. Defaults to `housekeeping` |
|`housekeeping.h2.home`|No| The path to your H2 filesystem database specified as a full path. Defaults to `$HOME/data`|
|`housekeeping.h2.database-name`|No| The name of your database within H2. This must be configured when using H2, otherwise it does not need to be specified |

#### Example YAML Configurations:

##### Vacuum Tool configured with MySQL Housekeeping database

Add your MySQL driver to `$VACUUM_TOOL_HOME/lib/`

    catalog:
      name: vacuum_tool
      hive-metastore-uris: thrift://my-metastore-uri.aws.hcom:9083

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
        url: jdbc:mysql://vacuum-tool-db.123456aws.us-west-2.rds.amazonaws.com:3306
        username: user
        password: foo


##### Vacuum Tool configured with H2 Housekeeping database

    catalog:
      name: vacuum_tool
      hive-metastore-uris: thrift://my-metastore-uri.aws.hcom:9083

    tables:
    -
      database-name: db
      table-name: table_1

    housekeeping:
      schema-name: my_db
      h2:
        home: /home/hadoop/vacuumtest
        database-name: db_1_test



Run with your respective YAML configuration file:

    $HOUSEKEEPING_TOOL_HOME/bin/vacuum.sh \
      --config=<your-config>.yml \
      [--dry-run=true] \
      [--partition-batch-size=1000] \
      [--expected-path-count=10000]

The vacuum tool looks for any files and folders in the data locations of your tables that are not referenced in either the metastore or housekeeping database. Any paths discovered are again scheduled for removal via the housekeeping process. The respective files and folders will then be removed at a time determined by the specific configuration of your housekeeping process.

We use the housekeeping process for data removal in this scenario as it has useful logic for determining when ancestral paths can also be removed.

The `dry-run` option allows you to observe the status of paths on the file system, the metastore, and the housekeeping database without performing any destructive changes. The partition-batch-size and expected-path-count allow you to tune memory demands should you hit heap limits with large numbers of partitions.

