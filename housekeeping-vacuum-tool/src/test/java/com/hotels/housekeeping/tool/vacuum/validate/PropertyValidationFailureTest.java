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
package com.hotels.housekeeping.tool.vacuum.validate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import com.hotels.housekeeping.tool.vacuum.conf.Table;
import com.hotels.housekeeping.tool.vacuum.conf.TablesValidationConfig;

public class PropertyValidationFailureTest {

  private final Table table = new Table();

  @Before
  public void setUp() {
    table.setDatabaseName("db");
    table.setTableName("table");
  }

  @Test
  public void propertyValidationFailure() {
    TablesValidationConfig tablesValidationConfig = new TablesValidationConfig();
    tablesValidationConfig.setHiveTableProperties(Lists.newArrayList("prop1"));
    Map<String, String> parameters = new HashMap<>();
    parameters.put("foo", "bar");
    PropertyValidationFailure failure = new PropertyValidationFailure(table, tablesValidationConfig, parameters);
    String message = failure.getMessage();
    assertThat(message, containsString("prop1"));
    assertThat(message, containsString("foo=bar"));
    assertThat(failure.getQualifiedTableName(), is("db.table"));
  }

}
