# Circus Train Tool

Housekeeping Tool Parent is a sub-module for debug and maintenance tools.
* **housekeeping-vacuum-tool:** Tool that removes data orphaned by a bug in Circus Train versions prior to 2.0.0

## Housekeeping Vacuum Tool

A tool to remove data incorrectly orphaned by Circus Train in versions prior to 2.0.0. The necessity of this project will hopefully decrease rapidly.

### Warning
Please upgrade your Circus Train installation to version 2.0.0 or greater before attempting to use this tool.

### Usage

Run with your respective replication YAML configuration file:

    $CIRCUS_TRAIN_TOOL_HOME/bin/vacuum.sh \
      --config=<your-config>.yml \
      [--dry-run=true] \
      [--partition-batch-size=1000] \
      [--expected-path-count=10000]

#TODO: Explain new YAML

Vacuum looks for any files and folders in the data locations of your replicated tables that are not referenced in either the metastore or Circus Train's housekeeping database. Any paths discovered are again scheduled for removal via the housekeeping process. The respective files and folders will then be removed at a time determined by the specific configuration of your housekeeping process.

We use the housekeeping process for data removal in this scenario as it has useful logic for determining when ancestral paths can also be removed.

The `dry-run` option allows you to observe the status of paths on the file system, the metastore, and the housekeeping database without performing any destructive changes. The partition-batch-size and expected-path-count allow you to tune memory demands should you hit heap limits with large numbers of partitions.

### Note
In order to run the circus-train-tools the user must install circus train, and set the bash variable CIRCUS_TRAIN_HOME to the base location of the circus-train folders.
