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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.thrift.TException;

import com.hotels.housekeeping.tool.vacuum.conf.Table;
import com.hotels.housekeeping.tool.vacuum.conf.TablesValidationConfig;

public class TablesValidator {

  private final TablesValidationConfig tableValidationConfig;

  public TablesValidator(TablesValidationConfig tableValidationConfig) {
    this.tableValidationConfig = tableValidationConfig;
  }

  /**
   * @param metastore
   * @param tables
   * @return {@link ValidationResult}
   */
  public ValidationResult validate(IMetaStoreClient metastore, List<Table> tables) {
    ValidationResult result = new ValidationResult();
    for (Table table : tables) {
      try {
        org.apache.hadoop.hive.metastore.api.Table hiveTable = metastore
            .getTable(table.getDatabaseName(), table.getTableName());
        Map<String, String> parameters = extractParameters(hiveTable.getParameters());
        if (!allPropertiesExist(parameters)) {
          result.addValidationFailure(new PropertyValidationFailure(table, tableValidationConfig, parameters));
        }
      } catch (NoSuchObjectException e) {
        result.addValidationFailure(new MissingTableValidationFailure(table));
      } catch (TException e) {
        throw new RuntimeException(e);
      }
    }
    return result;
  }

  private Map<String, String> extractParameters(Map<String, String> parameters) {
    if (parameters == null) {
      return Collections.emptyMap();
    }
    return parameters;
  }

  private boolean allPropertiesExist(Map<String, String> parameters) {
    for (String propertyName : tableValidationConfig.getHiveTableProperties()) {
      if (!parameters.containsKey(propertyName)) {
        return false;
      }
    }
    return true;
  }

}
