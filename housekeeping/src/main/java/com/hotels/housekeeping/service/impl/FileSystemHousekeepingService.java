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

import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.google.common.annotations.VisibleForTesting;

import com.hotels.housekeeping.HousekeepingException;
import com.hotels.housekeeping.conf.Housekeeping;
import com.hotels.housekeeping.model.LegacyReplicaPath;
import com.hotels.housekeeping.repository.LegacyReplicaPathRepository;
import com.hotels.housekeeping.service.HousekeepingService;

public class FileSystemHousekeepingService implements HousekeepingService {
  private static final Logger LOG = LoggerFactory.getLogger(FileSystemHousekeepingService.class);

  private final LegacyReplicaPathRepository<LegacyReplicaPath> legacyReplicaPathRepository;

  private final Configuration conf;

  private final int fetchLegacyReplicaPathPageSize;

  public FileSystemHousekeepingService(
      LegacyReplicaPathRepository<LegacyReplicaPath> legacyReplicaPathRepository,
      Configuration conf) {
    this(legacyReplicaPathRepository, conf, Housekeeping.DEFAULT_FETCH_LEGACY_REPLICA_PATH_PAGE_SIZE);
  }

  public FileSystemHousekeepingService(
      LegacyReplicaPathRepository<LegacyReplicaPath> legacyReplicaPathRepository,
      Configuration conf,
      int fetchLegacyReplicaPathPageSize) {
    this.legacyReplicaPathRepository = legacyReplicaPathRepository;
    this.conf = conf;
    this.fetchLegacyReplicaPathPageSize = fetchLegacyReplicaPathPageSize;
    // TODO remove this when there are no more records around that hit this.
    LOG.warn("{}.fixIncompleteRecord(LegacyReplicaPath) should be removed in future.", getClass());
  }

  private FileSystem fileSystemForPath(Path path) throws IOException {
    return path.getFileSystem(conf);
  }

  private void housekeepPath(LegacyReplicaPath cleanUpPath) {
    final Path path = new Path(cleanUpPath.getPath());
    final FileSystem fs;
    boolean cleanUpPathExists = true;
    try {
      fs = fileSystemForPath(path);
      LOG.info("Attempting to delete path '{}' from file system", cleanUpPath);
      if (fs.exists(path)) {
        fs.delete(path, true);
        cleanUpPathExists = false;
        LOG.info("Path '{}' has been deleted from file system", cleanUpPath);
      } else {
        cleanUpPathExists = false;
        LOG.warn("Path '{}' does not exist.", cleanUpPath);
      }
      deleteParents(fs, path, cleanUpPath.getPathEventId());
    } catch (IOException e) {
      LOG.warn("Unable to delete path '{}' from file system. Will try next time. {}", cleanUpPath, e.getMessage());
    }

    if (!cleanUpPathExists) {
      // BEWARE the eventual consistency of your blobstore!
      try {
        LOG.info("Deleting path '{}' from housekeeping database", cleanUpPath);
        legacyReplicaPathRepository.delete(cleanUpPath);
      } catch (ObjectOptimisticLockingFailureException e) {
        LOG
            .debug("Failed to delete path '{}': probably already cleaned up by process running at same time. "
                + "Ok to ignore. {}", cleanUpPath, e.getMessage());
      } catch (Exception e) {
        LOG.warn("Path '{}' was not deleted from the housekeeping database. {}", cleanUpPath, e.getMessage());
      }
    }
  }

  @Override
  public void cleanUp(Instant referenceTime) {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    try {
      Pageable pageRequest = new PageRequest(0, fetchLegacyReplicaPathPageSize);
      Page<LegacyReplicaPath> page = legacyReplicaPathRepository
          .findByCreationTimestampLessThanEqual(referenceTime.getMillis(), pageRequest);
      processPage(page, executor);
      int pagesProcessed = 1;
      int totalPages = page.getTotalPages();
      // We keep fetching the first page while we have pages. Because we are deleting entries the number of pages
      // changes so we use the total number of pages from the first request to loop over all pages once.
      while (page.hasNext() && pagesProcessed < totalPages) {
        page = legacyReplicaPathRepository.findByCreationTimestampLessThanEqual(referenceTime.getMillis(), pageRequest);
        processPage(page, executor);
        pagesProcessed++;
      }
    } catch (Throwable e) {
      throw new HousekeepingException(format("Unable to execute housekeeping at instant %d", referenceTime.getMillis()),
          e);
    } finally {
      executor.shutdownNow();
    }
  }

  private void processPage(Page<LegacyReplicaPath> page, ExecutorService executor) throws Throwable {
    List<Future<Void>> futures = new ArrayList<>(page.getSize());
    for (final LegacyReplicaPath cleanUpPath : page) {
      Future<Void> future = executor.submit(new Callable<Void>() {

        @Override
        public Void call() throws Exception {
          LegacyReplicaPath path = fixIncompleteRecord(cleanUpPath);
          housekeepPath(path);
          return null;
        }
      });
      futures.add(future);
    }
    for (Future<Void> future : futures) {
      try {
        future.get();
      } catch (ExecutionException e) {
        throw e.getCause();
      }
    }
  }

  // TODO remove this when there are no more records around that hit this.
  private LegacyReplicaPath fixIncompleteRecord(LegacyReplicaPath cleanUpPath) {
    Path path = new Path(cleanUpPath.getPath());
    if (StringUtils.isBlank(cleanUpPath.getPathEventId())) {
      String previousEventId = EventIdExtractor.extractFrom(path);
      if (previousEventId != null) {
        LOG.debug("Fixing path event for path '{}' -> '{}'.", path, previousEventId);
        cleanUpPath.setPathEventId(previousEventId);
      }
    }
    return cleanUpPath;
  }

  @VisibleForTesting
  Path deleteParents(FileSystem fs, Path path, String pathEventId) throws IOException {
    if (pathEventId == null || pathEventId.equals(path.getName()) || path.getParent() == null) {
      return path;
    }
    Path parent = path.getParent();
    if (fs.exists(parent)) {
      if (isEmpty(fs, parent)) {
        LOG.info("Deleting parent path '{}'", parent);
        fs.delete(parent, false);
        return deleteParents(fs, parent, pathEventId);
      } else {
        return parent;
      }
    }
    return deleteParents(fs, parent, pathEventId);
  }

  private boolean isEmpty(FileSystem fs, Path path) throws IOException {
    // TODO PD this is wrong for S3AFileSystems when you have a path that has those empty ..$folder$ files.
    // You need to do:
    // S3AFileStatus s3fileStatus = (S3AFileStatus) fs.getFileStatus(path);
    // s3fileStatus.isEmptyDirectory()
    // That will correctly mark the folders as non-empty.
    return !fs.exists(path) || fs.listStatus(path).length == 0;
  }

  @Override
  public void scheduleForHousekeeping(LegacyReplicaPath cleanUpPath) {
    try {
      legacyReplicaPathRepository.save(cleanUpPath);
    } catch (Exception e) {
      throw new HousekeepingException(format("Unable to schedule path %s for deletion", cleanUpPath.getPath()), e);
    }
  }

}
