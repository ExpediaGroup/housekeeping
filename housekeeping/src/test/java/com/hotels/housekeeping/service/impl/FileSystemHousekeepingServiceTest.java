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
package com.hotels.housekeeping.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.hotels.housekeeping.fs.HousekeepingFileSystem;
import com.hotels.housekeeping.fs.HousekeepingFileSystemFactory;
import com.hotels.housekeeping.fs.IdentityFileSystem;
import com.hotels.housekeeping.model.HousekeepingLegacyReplicaPath;
import com.hotels.housekeeping.model.LegacyReplicaPath;
import com.hotels.housekeeping.repository.LegacyReplicaPathRepository;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemHousekeepingServiceTest {

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  private static final String EVENT_ID = "eventId-a";
  private static final String PATH_EVENT_ID = "pathEventId-n";

  private final Instant now = new Instant();
  private Path eventPath;
  private Path test1Path;
  private Path val1Path;
  private Path val2Path;
  private Path val3Path;
  private LegacyReplicaPath cleanUpPath1;
  private LegacyReplicaPath cleanUpPath2;
  private LegacyReplicaPath cleanUpPath3;

  private @Mock LegacyReplicaPathRepository<LegacyReplicaPath> legacyReplicationPathRepository;

  private final LocalFileSystem localFileSystem = new LocalFileSystem();
  private @Spy final HousekeepingFileSystem fs = new IdentityFileSystem(localFileSystem);
  private @Mock HousekeepingFileSystemFactory housekeepingFileSystemFactory;
  private final Configuration conf = new Configuration(false);

  private FileSystemHousekeepingService service;

  @Before
  public void init() throws Exception {
    localFileSystem.initialize(localFileSystem.getUri(), conf);
    when(housekeepingFileSystemFactory.newInstance(any(FileSystem.class))).thenReturn(fs);
    eventPath = new Path(tmpFolder.newFolder("foo", "bar", PATH_EVENT_ID).getCanonicalPath());
    test1Path = new Path(tmpFolder.newFolder("foo", "bar", PATH_EVENT_ID, "test=1").getCanonicalPath());
    val1Path = new Path(tmpFolder.newFolder("foo", "bar", PATH_EVENT_ID, "test=1", "val=1").getCanonicalPath());
    val2Path = new Path(tmpFolder.newFolder("foo", "bar", PATH_EVENT_ID, "test=1", "val=2").getCanonicalPath());
    val3Path = new Path(tmpFolder.newFolder("foo", "bar", PATH_EVENT_ID, "test=1", "val=3").getCanonicalPath());
    service = new FileSystemHousekeepingService(housekeepingFileSystemFactory, legacyReplicationPathRepository, conf,
        50, 3);
    cleanUpPath1 = new HousekeepingLegacyReplicaPath(EVENT_ID, PATH_EVENT_ID, val1Path.toString(), null, null);
    cleanUpPath2 = new HousekeepingLegacyReplicaPath(EVENT_ID, PATH_EVENT_ID, val2Path.toString(), null, null);
    cleanUpPath3 = new HousekeepingLegacyReplicaPath(EVENT_ID, PATH_EVENT_ID, val3Path.toString(), null, null);
  }

  @Test
  public void sheduleForHousekeeping() {
    service.scheduleForHousekeeping(cleanUpPath1);
    verify(legacyReplicationPathRepository).save(cleanUpPath1);
  }

  @Test(expected = RuntimeException.class)
  public void scheduleFails() {
    when(legacyReplicationPathRepository.save(cleanUpPath1)).thenThrow(new RuntimeException());
    service.scheduleForHousekeeping(cleanUpPath1);
  }

  @Test
  public void cleanUp() throws Exception {
    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cleanUpPath1, cleanUpPath2, cleanUpPath3)));
    service.cleanUp(now);

    verify(legacyReplicationPathRepository).delete(cleanUpPath1);
    verify(legacyReplicationPathRepository).delete(cleanUpPath2);
    verify(legacyReplicationPathRepository).delete(cleanUpPath3);
    deleted(eventPath);
  }

  @Test
  public void cleanUpWithPaging() throws Exception {
    service = new FileSystemHousekeepingService(housekeepingFileSystemFactory, legacyReplicationPathRepository, conf, 1,
        3);
    PageRequest pageRequest = new PageRequest(0, 1);
    PageImpl<LegacyReplicaPath> page1 = new PageImpl<>(Arrays.asList(cleanUpPath1), pageRequest, 3);
    PageImpl<LegacyReplicaPath> page2 = new PageImpl<>(Arrays.asList(cleanUpPath2), pageRequest, 3);
    PageImpl<LegacyReplicaPath> page3 = new PageImpl<>(Arrays.asList(cleanUpPath3), pageRequest, 3);
    when(legacyReplicationPathRepository.findByCreationTimestampLessThanEqual(now.getMillis(), pageRequest))
        .thenReturn(page1)
        .thenReturn(page2)
        .thenReturn(page3);

    service.cleanUp(now);

    verify(legacyReplicationPathRepository).delete(cleanUpPath1);
    verify(legacyReplicationPathRepository).delete(cleanUpPath2);
    verify(legacyReplicationPathRepository).delete(cleanUpPath3);
    deleted(eventPath);
  }

  @Test
  public void cleanUpPathEventIdIsNull() throws Exception {
    LegacyReplicaPath cleanUpPathPathEventIdNull = new HousekeepingLegacyReplicaPath(EVENT_ID, null,
        val1Path.toString(), null, null);
    LegacyReplicaPath cleanUpPathEventIdNull = new HousekeepingLegacyReplicaPath(null, PATH_EVENT_ID,
        val2Path.toString(), null, null);
    LegacyReplicaPath cleanUpPathBothNotSet = new HousekeepingLegacyReplicaPath(val3Path.toString());

    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(
                Arrays.asList(cleanUpPathPathEventIdNull, cleanUpPathEventIdNull, cleanUpPathBothNotSet)));

    service.cleanUp(now);

    verify(legacyReplicationPathRepository).delete(cleanUpPathPathEventIdNull);
    verify(legacyReplicationPathRepository).delete(cleanUpPathEventIdNull);
    verify(legacyReplicationPathRepository).delete(cleanUpPathBothNotSet);
    deleted(eventPath);
  }

  @Test
  public void housekeepingPathsWithOneFileSystemLoadFailureCleansUpOtherPaths() throws Exception {
    when(housekeepingFileSystemFactory.newInstance(any(FileSystem.class))).thenThrow(new IOException()).thenReturn(fs);

    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cleanUpPath1, cleanUpPath2, cleanUpPath3)));

    service.cleanUp(now);

    // any path can fail deletion due to threading so just checking that any 2 paths are deleted from DB and FS.
    ArgumentCaptor<LegacyReplicaPath> captor = ArgumentCaptor.forClass(LegacyReplicaPath.class);
    verify(legacyReplicationPathRepository, times(2)).delete(captor.capture());

    List<LegacyReplicaPath> allDeletedPaths = captor.getAllValues();
    verify(fs).delete(eq(new Path(allDeletedPaths.get(0).getPath())), eq(true));
    verify(fs).delete(eq(new Path(allDeletedPaths.get(1).getPath())), eq(true));
  }

  @Test
  public void housekeepingPathThatDoesntExistSkipsDeleteAndRemovesPathFromHousekeepingDatabase() throws Exception {
    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cleanUpPath1)));
    doReturn(false).when(fs).exists(any(Path.class));

    service.cleanUp(now);

    verify(fs, never()).delete(eq(new Path(cleanUpPath1.getPath())), eq(true));
    exists(val1Path);
    verify(legacyReplicationPathRepository).delete(cleanUpPath1);
  }

  @Test
  public void deleteFailureDoesntRemovePathFromHousekeepingDatabase() throws Exception {
    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cleanUpPath1)));
    doThrow(new IOException()).when(fs).delete(eq(new Path(cleanUpPath1.getPath())), eq(true));

    service.cleanUp(now);

    verify(fs, times(1)).delete(eq(new Path(cleanUpPath1.getPath())), eq(true));
    verify(legacyReplicationPathRepository, times(0)).delete(cleanUpPath1);
  }

  @Test
  public void eventuallyConsistentCleanUpFull() throws Exception {
    PageImpl<LegacyReplicaPath> page1 = new PageImpl<>(Arrays.asList(cleanUpPath1, cleanUpPath2, cleanUpPath3));
    PageImpl<LegacyReplicaPath> page2 = new PageImpl<>(Arrays.asList(cleanUpPath1));
    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(page1)
            .thenReturn(page2);

    doReturn(false).doCallRealMethod().when(fs).delete(val1Path, true);
    service.cleanUp(now);

    exists(val1Path);
    deleted(val2Path, val3Path);
    verify(legacyReplicationPathRepository, never()).delete(cleanUpPath1);
    verify(legacyReplicationPathRepository).delete(cleanUpPath2);
    verify(legacyReplicationPathRepository).delete(cleanUpPath3);

    doReturn(false).when(fs).delete(eventPath, false);
    service.cleanUp(now);

    // event path remains, need manual or vacuum process to delete.
    exists(eventPath);
    deleted(val1Path);
    // cleanupPath is gone to prevent us from keep trying the same "undeletable" eventPath.
    verify(legacyReplicationPathRepository).delete(cleanUpPath1);
  }

  @Test
  public void eventuallyConsistentCleanUpRemainingPartition() throws Exception {
    PageImpl<LegacyReplicaPath> page1 = new PageImpl<>(Arrays.asList(cleanUpPath1, cleanUpPath2));
    PageImpl<LegacyReplicaPath> page2 = new PageImpl<>(Arrays.asList(cleanUpPath1));
    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(page1)
            .thenReturn(page2);

    doReturn(false).doCallRealMethod().when(fs).delete(val1Path, true);
    service.cleanUp(now);

    exists(val1Path);
    deleted(val2Path);
    verify(legacyReplicationPathRepository, never()).delete(cleanUpPath1);
    verify(legacyReplicationPathRepository).delete(cleanUpPath2);

    doReturn(false).when(fs).delete(eventPath, false);
    service.cleanUp(now);
    exists(eventPath);
    deleted(val1Path);
    verify(legacyReplicationPathRepository).delete(cleanUpPath1);
  }

  @Test
  public void eventuallyConsistentCleanUpOnlyKeys() throws Exception {
    Path val4Path = new Path(tmpFolder.newFolder("foo", "bar", PATH_EVENT_ID, "test=2", "val=4").getCanonicalPath());
    LegacyReplicaPath cleanUpPath4 = new HousekeepingLegacyReplicaPath(EVENT_ID, PATH_EVENT_ID, val4Path.toString(),
        null, null);

    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cleanUpPath4)));

    doReturn(false).when(fs).exists(eventPath);
    service.cleanUp(now);

    deleted(val4Path);
    verify(legacyReplicationPathRepository).delete(cleanUpPath4);
  }

  @Test
  public void cleanUpNonEmptyParent() throws Exception {
    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cleanUpPath1, cleanUpPath2)));

    service.cleanUp(now);

    verify(legacyReplicationPathRepository).delete(cleanUpPath1);
    verify(legacyReplicationPathRepository).delete(cleanUpPath2);
    exists(val3Path);
    deleted(val1Path, val2Path);
  }

  @Test
  public void onePathCannotBeDeleted() throws Exception {
    doThrow(new IOException()).when(fs).delete(val2Path, true);
    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cleanUpPath1, cleanUpPath2, cleanUpPath3)));

    service.cleanUp(now);

    verify(legacyReplicationPathRepository).delete(cleanUpPath1);
    verify(legacyReplicationPathRepository, never()).delete(cleanUpPath2);
    verify(legacyReplicationPathRepository).delete(cleanUpPath3);
    exists(val2Path);
    deleted(val1Path, val3Path);
  }

  @Test
  public void filesystemParentPathDeletionFailsRemoveCleanUpPathAnyway() throws Exception {
    doThrow(new IOException("Can't delete parent!")).when(fs).delete(test1Path, false);
    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cleanUpPath1, cleanUpPath2, cleanUpPath3)));

    service.cleanUp(now);

    verify(legacyReplicationPathRepository).delete(cleanUpPath1);
    verify(legacyReplicationPathRepository).delete(cleanUpPath2);
    verify(legacyReplicationPathRepository).delete(cleanUpPath3);
    exists(test1Path);
    deleted(val1Path, val2Path, val3Path);
  }

  @Test
  public void repositoryDeletionFailsHousekeepingContinues() throws Exception {
    doThrow(new IllegalArgumentException()).when(legacyReplicationPathRepository).delete(cleanUpPath1);
    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cleanUpPath1)));

    service.cleanUp(now);
  }

  @Test
  public void repositoryDeletionWithOptimisticLockingExceptionIsIgnored() throws Exception {
    doThrow(new ObjectOptimisticLockingFailureException("Error", new Exception()))
        .when(legacyReplicationPathRepository)
        .delete(cleanUpPath1);
    when(legacyReplicationPathRepository
        .findByCreationTimestampLessThanEqual(eq(now.getMillis()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cleanUpPath1, cleanUpPath2)));
    service.cleanUp(now);
    // all paths will still be deleted
    deleted(val1Path, val2Path);
  }

  // TODO remove this when there are no more records around that hit this.
  @Test
  public void regexp() {
    Pattern pattern = Pattern.compile(EventIdExtractor.EVENT_ID_REGEXP);
    assertThat(pattern.matcher("a/ctp-20160726T162136.657Z-Vdqln6v7").matches(), is(true));
    assertThat(pattern.matcher("a/ctt-20160726T162136.657Z-Vdqln6v7").matches(), is(true));
    assertThat(pattern.matcher("a/ctp-20000101T000000.000Z-Vdqln6v7").matches(), is(true));
    assertThat(pattern.matcher("a/ctp-20991231T235959.999Z-Vdqln6v7").matches(), is(true));

    assertThat(pattern.matcher("a/ctp-19990101T000000.000Z-Vdqln6v7").matches(), is(false));
    assertThat(pattern.matcher("a/cta-19990101T000000.000Z-Vdqln6v7").matches(), is(false));
    assertThat(pattern.matcher("a/ctp-20000101T000000.00Z-Vdqln6v7").matches(), is(false));
    assertThat(pattern.matcher("a/ctp-20000101T000000.0000Z-Vdqln6v7").matches(), is(false));
    assertThat(pattern.matcher("a/ctp-20000101T000000.000Z-dqln6v7").matches(), is(false));

    assertThat(pattern.matcher("a/ctp-20991231T235959.999Z-Vdqln6v78").matches(), is(false));
    assertThat(pattern.matcher("a/ctp-21001231T235959.999Z-Vdqln6v7").matches(), is(false));
    assertThat(pattern.matcher("a/ctp-20993231T235959.999Z-Vdqln6v7").matches(), is(false));
    assertThat(pattern.matcher("a/ctp-20991231T236959.999Z-Vdqln6v7").matches(), is(false));
    assertThat(pattern.matcher("a/ctp-20991231T235969.999Z-Vdqln6v7").matches(), is(false));

    Matcher matcher = pattern.matcher("s3://bucket-dj49488/ctt-20160726T162136.657Z-Vdqln6v7/part-00000");
    matcher.matches();
    assertThat(matcher.group(1), is("ctt-20160726T162136.657Z-Vdqln6v7"));

    matcher = pattern.matcher("s3://bucket-dj49488/ctp-20160726T162136.657Z-Vdqln6v7/2012/01/01/00/part-00000");
    matcher.matches();
    assertThat(matcher.group(1), is("ctp-20160726T162136.657Z-Vdqln6v7"));
  }

  @Test
  public void housekeepPathWithNullParent() throws IOException {
    Path nullParentPath = Mockito.mock(Path.class);
    when(nullParentPath.getName()).thenReturn(test1Path.getName());
    when(nullParentPath.getParent()).thenReturn(null);
    when(nullParentPath.toUri()).thenReturn(test1Path.toUri());

    Path deleteParents = service.deleteParents(fs, nullParentPath, PATH_EVENT_ID);
    assertThat(deleteParents, is(nullParentPath));
  }

  @Test
  public void deleteParentsStopRecursionIfParentIsNotEmpty() throws IOException {
    Path path = new Path(tmpFolder.newFolder("foo", "bar", "shouldNotMatchPathEventId").getCanonicalPath());
    Path rootPath = service.deleteParents(fs, path, PATH_EVENT_ID);
    assertThat(rootPath, is(path.getParent()));
  }

  private void deleted(Path... paths) {
    for (Path path : paths) {
      assertFalse(new File(path.toString()).exists());
    }
  }

  private void exists(Path... paths) {
    for (Path path : paths) {
      assertTrue(new File(path.toString()).exists());
    }
  }
}
