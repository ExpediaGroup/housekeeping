# Housekeeping Tool

Housekeeping Tool Parent is a sub-module for Housekeeping debug and maintenance tools.

## Housekeeping Vacuum Tool

### Usage

#### YAML Configuration

    catalog:
      name:
      hive-metastore-uris: thrift://shared-waggle-dance.us-west-2.hcom-data-lab.aws.hcom:48869

    tables:
    -
      database-name: bdp
      table-name: etl_hcom_hex_fact_2

    housekeeping:
      schema-name: circus_train
      dataSource:
        driverClassName: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://vacuum-tool-test-db.c9czcayr4qpx.us-west-2.rds.amazonaws.com:3306/circus_train
        username: bdp
        password: Ch4ll3ng3


Run with your respective YAML configuration file:

    $HOUSEKEEPING_TOOL_HOME/bin/vacuum.sh \
      --config=<your-config>.yml \
      [--dry-run=true] \
      [--partition-batch-size=1000] \
      [--expected-path-count=10000]

#TODO: Explain new YAML

Vacuum looks for any files and folders in the data locations of your replicated tables that are not referenced in either the metastore or housekeeping database. Any paths discovered are again scheduled for removal via the housekeeping process. The respective files and folders will then be removed at a time determined by the specific configuration of your housekeeping process.

We use the housekeeping process for data removal in this scenario as it has useful logic for determining when ancestral paths can also be removed.

The `dry-run` option allows you to observe the status of paths on the file system, the metastore, and the housekeeping database without performing any destructive changes. The partition-batch-size and expected-path-count allow you to tune memory demands should you hit heap limits with large numbers of partitions.

