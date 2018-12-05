package com.hotels.housekeeping.tool.vacuum.validate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.thrift.TException;

import com.hotels.housekeeping.tool.vacuum.conf.Table;
import com.hotels.housekeeping.tool.vacuum.conf.TablesValidation;

public class TablesValidator {

  private final TablesValidation tableValidation;

  public TablesValidator(TablesValidation tableValidation) {
    this.tableValidation = tableValidation;
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
        Map<String, String> parameters = getParameters(hiveTable.getParameters());
        if (!allPropertiesExist(parameters)) {
          result.addValidationFailure(new PropertyValidationFailure(table, tableValidation, parameters));
        }
      } catch (TException e) {
        result.addValidationFailure(new UnexpectedValidationFailure(table, e));
      }
    }
    return result;
  }

  private Map<String, String> getParameters(Map<String, String> parameters) {
    if (parameters == null) {
      return Collections.emptyMap();
    }
    return parameters;
  }

  private boolean allPropertiesExist(Map<String, String> parameters) {
    for (String propertyName : tableValidation.getCheckPropertyExists()) {
      if (!parameters.containsKey(propertyName)) {
        return false;
      }
    }
    return true;
  }

}
