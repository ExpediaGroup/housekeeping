/**
 * Copyright (C) 2016-2017 Expedia Inc.
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

public interface LegacyReplicaPath {
  public String getEventId();

  public String getPathEventId();

  public String getPath();

  public void setPath(String path);

  public void setPathEventId(String pathEventId);

  public void setEventId(String eventId);

  public static final class DEFAULT {
    public static LegacyReplicaPath instance(String path) {
      LegacyReplicaPath legacyReplicaPath = DEFAULT.newInstance();
      legacyReplicaPath.setPath(path);
      return legacyReplicaPath;
    }

    public static LegacyReplicaPath instance(String eventId, String pathEventId, String path) {
      LegacyReplicaPath legacyReplicaPath = DEFAULT.newInstance();
      legacyReplicaPath.setEventId(eventId);
      legacyReplicaPath.setPathEventId(pathEventId);
      legacyReplicaPath.setPath(path);
      return legacyReplicaPath;
    }

    private static LegacyReplicaPath newInstance() {
      return new LegacyReplicaPath() {

        private String eventId;

        private String path;

        private String pathEventId;

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
      };
    }
  }
}
