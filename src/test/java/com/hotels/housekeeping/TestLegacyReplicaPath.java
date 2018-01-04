/**
 * Copyright (C) 2016-2018 Expedia Inc.
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
package com.hotels.housekeeping;

import com.hotels.housekeeping.model.LegacyReplicaPath;

public class TestLegacyReplicaPath implements LegacyReplicaPath {

  private long id = 1l;
  private long creationTimestamp;
  private String eventId;
  private String path;
  private String pathEventId;

  public TestLegacyReplicaPath(String eventId, String pathEventId, String path) {
    this.eventId = eventId;
    this.pathEventId = pathEventId;
    this.path = path;
  }

  public long getId() {return id;}

  public String getEventId() {
    return eventId;
  }

  public String getPathEventId() {
    return pathEventId;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) { this.path = path;}

  public void setPathEventId(String pathEventId) {
    this.pathEventId = pathEventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public long getCreationTimestamp() {
    return creationTimestamp;
  }

  public void setCreationTimestamp(long creationTimestamp) {
    this.creationTimestamp = creationTimestamp;
  }
}
