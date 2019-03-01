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

import com.hotels.housekeeping.HousekeepingException;
import com.hotels.housekeeping.model.LegacyReplicaPath;
import com.hotels.housekeeping.repository.LegacyReplicaPathRepository;
import com.hotels.housekeeping.service.HousekeepingPathService;

public class FileSystemHousekeepingPathService implements HousekeepingPathService {

  private final LegacyReplicaPathRepository<LegacyReplicaPath> legacyReplicaPathRepository;

  public FileSystemHousekeepingPathService(
      LegacyReplicaPathRepository<LegacyReplicaPath> legacyReplicaPathRepository) {
    this.legacyReplicaPathRepository = legacyReplicaPathRepository;
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
