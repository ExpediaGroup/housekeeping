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
public class EntityAuditRevision implements AuditRevision {

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
