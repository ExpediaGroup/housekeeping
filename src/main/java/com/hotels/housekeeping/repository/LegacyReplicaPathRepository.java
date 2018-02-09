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
package com.hotels.housekeeping.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.hotels.housekeeping.model.LegacyReplicaPath;

@NoRepositoryBean
public interface LegacyReplicaPathRepository<T extends LegacyReplicaPath> extends CrudRepository<T, Long> {

  // http://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods
  List<T> findByCreationTimestampLessThanEqual(long creationTimestamp);

}
