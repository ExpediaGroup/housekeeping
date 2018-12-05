package com.hotels.housekeeping.tool.vacuum.validate;

import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.hotels.housekeeping.tool.vacuum.conf.Table;
import com.hotels.housekeeping.tool.vacuum.conf.TablesValidation;

class PropertyValidationFailure implements ValidationFailure {

  private final Table table;
  private final TablesValidation tablesValidation;
  private final Map<String, String> tableParameters;

  PropertyValidationFailure(Table table, TablesValidation tablesValidation, Map<String, String> tableParameters) {
    this.table = table;
    this.tablesValidation = tablesValidation;
    this.tableParameters = tableParameters;

  }

  @Override
  public String getMessage() {
    String validationConfig = new Yaml().dumpAsMap(tablesValidation);
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
