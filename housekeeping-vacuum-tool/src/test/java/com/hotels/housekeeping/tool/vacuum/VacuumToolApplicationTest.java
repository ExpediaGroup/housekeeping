/**
 * Copyright (C) 2016-2021 Expedia, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.housekeeping.tool.vacuum;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Supplier;

import com.hotels.hcommon.hive.metastore.client.api.CloseableMetaStoreClient;
import com.hotels.hcommon.hive.metastore.paths.PathUtils;
import com.hotels.housekeeping.model.HousekeepingLegacyReplicaPath;
import com.hotels.housekeeping.model.LegacyReplicaPath;
import com.hotels.housekeeping.repository.LegacyReplicaPathRepository;
import com.hotels.housekeeping.service.HousekeepingService;
import com.hotels.housekeeping.tool.vacuum.conf.Table;
import com.hotels.housekeeping.tool.vacuum.conf.Tables;
import com.hotels.housekeeping.tool.vacuum.validate.TablesValidator;
import com.hotels.housekeeping.tool.vacuum.validate.ValidationFailure;
import com.hotels.housekeeping.tool.vacuum.validate.ValidationResult;

@RunWith(MockitoJUnitRunner.class)
public class VacuumToolApplicationTest {

  private static final String PARTITION_NAME = "partition=1/x=y";
  private static final String PARTITION_EVENT_1 = "ctp-20160728T110821.830Z-w5npK1yY";
  private static final String PARTITION_EVENT_2 = "ctp-20160728T110821.830Z-w5npK2yY";
  private static final String PARTITION_EVENT_3 = "ctp-20160728T110821.830Z-w5npK3yY";
  private static final String TABLE_EVENT_1 = "ctt-20160728T110821.830Z-w5npK1yY";
  private static final String UNPARTITIONED_TABLE_NAME = "unpartitioned_table";
  private static final String PARTITIONED_TABLE_NAME = "partitioned_table";
  private static final String DATABASE_NAME = "database";

  private HiveConf conf;
  private Tables replications;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private Supplier<CloseableMetaStoreClient> clientSupplier;
  @Mock
  private CloseableMetaStoreClient client;
  @Mock
  private org.apache.hadoop.hive.metastore.api.Table unpartitionedTable;
  @Mock
  private StorageDescriptor unpartitionedSd;
  @Mock
  private org.apache.hadoop.hive.metastore.api.Table partitionedTable;
  @Mock
  private StorageDescriptor partitionedSd;
  @Mock
  private Partition partition;
  @Mock
  private StorageDescriptor partitionSd;
  @Mock
  private LegacyReplicaPathRepository legacyReplicaPathRepository;
  @Mock
  private HousekeepingService housekeepingService;
  @Mock
  private TablesValidator tablesValidator;
  @Mock
  private ValidationResult validationResult;
  @Mock
  private ValidationFailure validationFailure1;
  @Mock
  private ValidationFailure validationFailure2;
  @Captor
  private ArgumentCaptor<LegacyReplicaPath> pathCaptor;

  private String unpartitionLocation;
  private String partitionedBaseLocation;
  private String partitionLocation1;
  private String partitionLocation2;
  private String partitionLocation3;

  @Before
  public void initialise() throws TException, IOException {
    unpartitionLocation = new Path(temporaryFolder.newFolder("unpartitioned", TABLE_EVENT_1).toURI().toString())
        .toString();

    partitionedBaseLocation = PathUtils
        .normalise(new Path(temporaryFolder.newFolder("partitioned").toURI()))
        .toString();
    partitionLocation1 = PathUtils
        .normalise(new Path(temporaryFolder.newFolder("partitioned", PARTITION_EVENT_1, "yyyy", "mm", "dd").toURI()))
        .toString();
    partitionLocation2 = PathUtils
        .normalise(new Path(temporaryFolder.newFolder("partitioned", PARTITION_EVENT_2, "yyyy", "mm", "dd").toURI()))
        .toString();
    partitionLocation3 = PathUtils
        .normalise(new Path(temporaryFolder.newFolder("partitioned", PARTITION_EVENT_3, "yyyy", "mm", "dd").toURI()))
        .toString();

    conf = new HiveConf(new Configuration(false), VacuumToolApplicationTest.class);

    Table partitionedReplicaTable = new Table();
    partitionedReplicaTable.setDatabaseName(DATABASE_NAME);
    partitionedReplicaTable.setTableName(PARTITIONED_TABLE_NAME);

    Table unpartitionedReplicaTable = new Table();
    unpartitionedReplicaTable.setDatabaseName(DATABASE_NAME);
    unpartitionedReplicaTable.setTableName(UNPARTITIONED_TABLE_NAME);

    replications = new Tables();
    replications.setTables(Arrays.asList(partitionedReplicaTable, unpartitionedReplicaTable));

    when(clientSupplier.get()).thenReturn(client);

    LegacyReplicaPath legacyReplicaPath = new HousekeepingLegacyReplicaPath("eventId", PARTITION_EVENT_1,
        partitionLocation1, DATABASE_NAME, PARTITIONED_TABLE_NAME);
    when(legacyReplicaPathRepository.findAll()).thenReturn(Collections.singletonList(legacyReplicaPath));

    when(unpartitionedTable.getDbName()).thenReturn(DATABASE_NAME);
    when(unpartitionedTable.getTableName()).thenReturn(UNPARTITIONED_TABLE_NAME);
    when(unpartitionedTable.getPartitionKeys()).thenReturn(Collections.<FieldSchema>emptyList());
    when(unpartitionedTable.getSd()).thenReturn(unpartitionedSd);
    when(unpartitionedSd.getLocation()).thenReturn(unpartitionLocation);
    when(partitionedTable.getDbName()).thenReturn(DATABASE_NAME);
    when(partitionedTable.getTableName()).thenReturn(PARTITIONED_TABLE_NAME);
    when(partitionedTable.getPartitionKeys()).thenReturn(
        Collections.singletonList(new FieldSchema("local_date", "string", "comment")));
    when(partitionedTable.getSd()).thenReturn(partitionedSd);
    when(partitionedSd.getLocation()).thenReturn(partitionedBaseLocation);
    when(partition.getSd()).thenReturn(partitionSd);

    when(client.getTable(DATABASE_NAME, UNPARTITIONED_TABLE_NAME)).thenReturn(unpartitionedTable);
    when(client.getTable(DATABASE_NAME, PARTITIONED_TABLE_NAME)).thenReturn(partitionedTable);

    when(client.listPartitionNames(DATABASE_NAME, PARTITIONED_TABLE_NAME, (short) -1))
        .thenReturn(Collections.singletonList(PARTITION_NAME));
  }

  @Test
  public void removePath() {
    VacuumToolApplication tool = new VacuumToolApplication(conf, clientSupplier, legacyReplicaPathRepository,
        housekeepingService, tablesValidator, replications, false, (short) 100);
    tool.removePath(new Path(partitionLocation1), "db", "table");

    verify(housekeepingService).scheduleForHousekeeping(pathCaptor.capture());
    LegacyReplicaPath legacyReplicaPath = pathCaptor.getValue();
    assertThat(legacyReplicaPath.getPath(), is(partitionLocation1));
    assertThat(legacyReplicaPath.getPathEventId(), is(PARTITION_EVENT_1));
    assertThat(legacyReplicaPath.getEventId().startsWith("vacuum-"), is(true));
    assertThat(legacyReplicaPath.getMetastoreDatabaseName(), is("db"));
    assertThat(legacyReplicaPath.getMetastoreTableName(), is("table"));
  }

  @Test
  public void fetchHousekeepingPaths() throws Exception {
    VacuumToolApplication tool = new VacuumToolApplication(conf, clientSupplier, legacyReplicaPathRepository,
        housekeepingService, tablesValidator, replications, false, (short) 100);
    Set<Path> paths = tool.fetchHousekeepingPaths(legacyReplicaPathRepository);

    assertThat(paths.size(), is(1));
    assertThat(paths.iterator().next(), is(new Path(partitionLocation1)));
  }

  @Test
  public void run() throws Exception {
    // There are 3 paths on the FS: partitionLocation1, partitionLocation2, partitionLocation3
    Table table = new Table();
    table.setDatabaseName(DATABASE_NAME);
    table.setTableName(PARTITIONED_TABLE_NAME);
    replications.setTables(Collections.singletonList(table));

    // The MS references path 1
    when(partitionSd.getLocation()).thenReturn(partitionLocation1);
    when(partition.getSd()).thenReturn(partitionSd);
    when(client.getTable(DATABASE_NAME, PARTITIONED_TABLE_NAME)).thenReturn(partitionedTable);

    when(client.listPartitions(DATABASE_NAME, PARTITIONED_TABLE_NAME, (short) 1))
        .thenReturn(Collections.singletonList(partition));
    when(client.getPartitionsByNames(DATABASE_NAME, PARTITIONED_TABLE_NAME, Collections.singletonList(PARTITION_NAME)))
        .thenReturn(Collections.singletonList(partition));

    // The HK references path 2
    when(legacyReplicaPathRepository.findAll())
        .thenReturn(Collections
            .singletonList(new HousekeepingLegacyReplicaPath("eventId", PARTITION_EVENT_2, partitionLocation2,
                DATABASE_NAME, PARTITIONED_TABLE_NAME)));
    when(validationResult.isValid()).thenReturn(true);
    when(tablesValidator.validate(client, replications.getTables())).thenReturn(validationResult);

    // So we expect path 3 to be scheduled for removal
    VacuumToolApplication tool = new VacuumToolApplication(conf, clientSupplier, legacyReplicaPathRepository,
        housekeepingService, tablesValidator, replications, false, (short) 100);
    tool.run(null);

    verify(housekeepingService, times(1)).scheduleForHousekeeping(pathCaptor.capture());
    LegacyReplicaPath legacyReplicaPath = pathCaptor.getValue();
    assertThat(legacyReplicaPath.getPath(), is(partitionLocation3));
    assertThat(legacyReplicaPath.getPathEventId(), is(PARTITION_EVENT_3));
    assertThat(legacyReplicaPath.getEventId().startsWith("vacuum-"), is(true));
  }

  @Test
  public void runValidationFails() {
    Table table = new Table();
    table.setDatabaseName(DATABASE_NAME);
    table.setTableName(PARTITIONED_TABLE_NAME);
    replications.setTables(Collections.singletonList(table));

    when(validationResult.isValid()).thenReturn(false);
    List<ValidationFailure> validationFailures = new ArrayList<>();
    validationFailures.add(validationFailure1);
    validationFailures.add(validationFailure2);
    when(validationResult.getValidationFailures()).thenReturn(validationFailures);
    when(tablesValidator.validate(client, replications.getTables())).thenReturn(validationResult);

    VacuumToolApplication tool = new VacuumToolApplication(conf, clientSupplier, legacyReplicaPathRepository,
        housekeepingService, tablesValidator, replications, false, (short) 100);
    try {
      tool.run(null);
      fail("Should have thrown an exception to stop vacuuming and sort out the invalid config");
    } catch (Exception e) {
      verifyZeroInteractions(housekeepingService);
      verify(validationFailure1).getMessage();
      verify(validationFailure2).getMessage();
    }
  }
}
