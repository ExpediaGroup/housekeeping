/**
 * Copyright (C) 2016-2019 Expedia Inc.
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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hotels.housekeeping.model.HousekeepingLegacyReplicaPath;
import com.hotels.housekeeping.model.LegacyReplicaPath;
import com.hotels.housekeeping.repository.LegacyReplicaPathRepository;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FileSystemHousekeepingCleanupService.class, FileSystem.class })
public class FileSystemHousekeepingPathServiceTest {

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  private static final String EVENT_ID = "eventId-a";
  private static final String PATH_EVENT_ID = "pathEventId-n";

  private final Instant now = new Instant();
  private Path valPath;
  private LegacyReplicaPath cleanUpPath;

  private @Mock LegacyReplicaPathRepository<LegacyReplicaPath> legacyReplicationPathRepository;
  private @Spy final FileSystem spyFs = new LocalFileSystem();

  private FileSystemHousekeepingPathService service;

  @Before
  public void init() throws Exception {
//    spyFs.initialize(spyFs.getUri(), conf);
//    mockStatic(FileSystem.class);
//    when(FileSystem.get(any(URI.class), any(Configuration.class))).thenReturn(spyFs);
    valPath = new Path(tmpFolder.newFolder("foo", "bar", PATH_EVENT_ID, "test=1", "val=1").getCanonicalPath());
    service = PowerMockito.spy(new FileSystemHousekeepingPathService(legacyReplicationPathRepository));
    cleanUpPath = new HousekeepingLegacyReplicaPath(EVENT_ID, PATH_EVENT_ID, valPath.toString(), null, null);
  }

  @Test
  public void sheduleForHousekeeping() {
    service.scheduleForHousekeeping(cleanUpPath);
    verify(legacyReplicationPathRepository).save(cleanUpPath);
  }

  @Test(expected = RuntimeException.class)
  public void scheduleFails() {
    when(legacyReplicationPathRepository.save(cleanUpPath)).thenThrow(new RuntimeException());
    service.scheduleForHousekeeping(cleanUpPath);
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
}
