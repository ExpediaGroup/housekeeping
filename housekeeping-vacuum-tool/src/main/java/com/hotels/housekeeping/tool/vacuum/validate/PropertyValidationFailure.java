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

import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.hotels.housekeeping.tool.vacuum.conf.Table;
import com.hotels.housekeeping.tool.vacuum.conf.TablesValidationConfig;

class PropertyValidationFailure implements ValidationFailure {

  private final Table table;
  private final TablesValidationConfig tablesValidationConfig;
  private final Map<String, String> tableParameters;

  PropertyValidationFailure(
      Table table,
      TablesValidationConfig tablesValidationConfig,
      Map<String, String> tableParameters) {
    this.table = table;
    this.tablesValidationConfig = tablesValidationConfig;
    this.tableParameters = tableParameters;

  }

  @Override
  public String getMessage() {
    String validationConfig = new Yaml().dumpAsMap(tablesValidationConfig);
    return "Property validation failed expected these properties: "
        + validationConfig
        + "but got these table parameters \n'"
        + tableParameters
        + "'";
  }

  @Override
  public String getQualifiedTableName() {
    return table.getDatabaseName() + "." + table.getTableName();
  }
}
