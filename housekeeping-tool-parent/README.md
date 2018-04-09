# Housekeeping Tool

Housekeeping Tool Parent is a sub-module for debug and maintenance tools.
* **housekeeping-vacuum-tool:** Tool that removes housekeeping data

## Housekeeping Vacuum Tool

### Usage

Run with your respective replication YAML configuration file:

    $HOUSEKEEPING_TOOL_HOME/bin/vacuum.sh \
      --config=<your-config>.yml \
      [--dry-run=true] \
      [--partition-batch-size=1000] \
      [--expected-path-count=10000]

#TODO: Explain new YAML

Vacuum looks for any files and folders in the data locations of your replicated tables that are not referenced in either the metastore or housekeeping database. Any paths discovered are again scheduled for removal via the housekeeping process. The respective files and folders will then be removed at a time determined by the specific configuration of your housekeeping process.

We use the housekeeping process for data removal in this scenario as it has useful logic for determining when ancestral paths can also be removed.

The `dry-run` option allows you to observe the status of paths on the file system, the metastore, and the housekeeping database without performing any destructive changes. The partition-batch-size and expected-path-count allow you to tune memory demands should you hit heap limits with large numbers of partitions.

