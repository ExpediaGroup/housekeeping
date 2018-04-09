/**
 * Copyright (C) 2017-2018 Expedia Inc.
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
package com.hotels.housekeeping.model;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;

@Entity
// @Converter doesn't work with @Audited until https://hibernate.atlassian.net/browse/HHH-9042 is released
@Audited
@Table(name = "legacy_replica_path", uniqueConstraints = @UniqueConstraint(columnNames = {
    "path",
    "creation_timestamp" }))
public class HousekeepingLegacyReplicaPath extends EntityLegacyReplicaPath {

  protected HousekeepingLegacyReplicaPath() {}

  public HousekeepingLegacyReplicaPath(String path) {
    this.path = path;
    eventId = "";
    pathEventId = "";
  }

  public HousekeepingLegacyReplicaPath(String eventId, String pathEventId, String path) {
    this.eventId = eventId;
    this.pathEventId = pathEventId;
    this.path = path;
  }

}
