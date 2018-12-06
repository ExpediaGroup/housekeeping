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
