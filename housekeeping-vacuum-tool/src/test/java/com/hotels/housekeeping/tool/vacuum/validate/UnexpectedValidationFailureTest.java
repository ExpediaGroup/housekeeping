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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.hotels.housekeeping.tool.vacuum.conf.Table;

public class UnexpectedValidationFailureTest {

  private final Table table = new Table();

  @Before
  public void setUp() {
    table.setDatabaseName("db");
    table.setTableName("table");
  }

  @Test
  public void unexpectedValidationFailure() {
    UnexpectedValidationFailure failure = new UnexpectedValidationFailure(table,
        new Exception("oh no", new Exception("unexpectedValidationFailure cause")));
    String message = failure.getMessage();
    assertThat(message, containsString("oh no"));
    assertThat(message, containsString("unexpectedValidationFailure cause"));
    assertThat(failure.getQualifiedTableName(), is("db.table"));
  }

}
