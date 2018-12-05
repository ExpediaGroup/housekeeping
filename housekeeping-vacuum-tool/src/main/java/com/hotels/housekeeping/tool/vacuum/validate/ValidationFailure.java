package com.hotels.housekeeping.tool.vacuum.validate;

public interface ValidationFailure {

  /**
   * @return explanation about the failure
   */
  String getMessage();

  /**
   * @return 'database.table' name that is invalid and cannot be vacuumed
   */
  String getQualifiedTableName();
}
