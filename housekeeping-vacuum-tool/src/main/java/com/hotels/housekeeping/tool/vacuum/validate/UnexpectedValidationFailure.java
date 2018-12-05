package com.hotels.housekeeping.tool.vacuum.validate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import com.hotels.housekeeping.tool.vacuum.conf.Table;

class UnexpectedValidationFailure implements ValidationFailure {

  private final Table table;
  private final Exception e;

  UnexpectedValidationFailure(Table table, Exception e) {
    this.table = table;
    this.e = e;
  }

  @Override
  public String getMessage() {
    Writer stringWriter = new StringWriter();
    try (PrintWriter writer = new PrintWriter(stringWriter)) {
      writer.println("Failed to validate with exception:");
      e.printStackTrace(writer);
      writer.flush();
    }
    return stringWriter.toString();
  }

  @Override
  public String getQualifiedTableName() {
    return table.getDatabaseName() + "." + table.getTableName();
  }
}
