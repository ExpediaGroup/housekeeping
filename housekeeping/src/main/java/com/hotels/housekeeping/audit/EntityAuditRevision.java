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
package com.hotels.housekeeping.audit;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@MappedSuperclass
@RevisionEntity
public abstract class EntityAuditRevision implements AuditRevision {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  @RevisionNumber
  @Column(name = "id")
  private int id;

  @RevisionTimestamp
  @Column(name = "timestamp")
  private long timestamp;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Transient
  public Date getRevisionDate() {
    return new Date(timestamp);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    EntityAuditRevision other = (EntityAuditRevision) obj;
    if (id != other.id) {
      return false;
    }
    return true;
  }

}
