package com.hotels.housekeeping.tool.vacuum.validate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;

import com.hotels.housekeeping.tool.vacuum.conf.Table;
import com.hotels.housekeeping.tool.vacuum.conf.TablesValidation;

@RunWith(MockitoJUnitRunner.class)
public class TablesValidatorTest {

  private static final String DATABASE = "db";
  private static final String TABLE1 = "table1";
  private static final String TABLE2 = "table2";

  @Mock
  private IMetaStoreClient metastore;
  private final List<Table> tables = new ArrayList<>();
  private final Table table1 = new Table();
  private final Table table2 = new Table();
  private final TablesValidation emptyTablesValidation = new TablesValidation();

  private final org.apache.hadoop.hive.metastore.api.Table hiveTable1 = new org.apache.hadoop.hive.metastore.api.Table();
  private final org.apache.hadoop.hive.metastore.api.Table hiveTable2 = new org.apache.hadoop.hive.metastore.api.Table();

  @Before
  public void setUp() {
    table1.setDatabaseName(DATABASE);
    table1.setTableName(TABLE1);
    tables.add(table1);
    hiveTable1.setDbName(DATABASE);
    hiveTable1.setTableName(TABLE1);
    hiveTable1.setParameters(new HashMap<String, String>());

    table2.setDatabaseName(DATABASE);
    table2.setTableName(TABLE2);
    tables.add(table2);
    hiveTable2.setDbName(DATABASE);
    hiveTable2.setTableName(TABLE2);
    hiveTable2.setParameters(new HashMap<String, String>());

    emptyTablesValidation.setCheckPropertyExists(new ArrayList<String>());
  }

  @Test
  public void validateAllOk() throws Exception {
    when(metastore.getTable(DATABASE, TABLE1)).thenReturn(hiveTable1);
    when(metastore.getTable(DATABASE, TABLE2)).thenReturn(hiveTable2);

    TablesValidator validator = new TablesValidator(emptyTablesValidation);
    ValidationResult validationResult = validator.validate(metastore, tables);

    assertThat(validationResult.isValid(), is(true));
    assertThat(validationResult.getValidationFailures().size(), is(0));
  }

  @Test
  public void validateFailsTableNotFound() throws Exception {
    when(metastore.getTable(DATABASE, TABLE1)).thenReturn(hiveTable1);
    when(metastore.getTable(DATABASE, TABLE2)).thenThrow(new NoSuchObjectException());

    TablesValidator validator = new TablesValidator(emptyTablesValidation);
    ValidationResult validationResult = validator.validate(metastore, tables);

    assertThat(validationResult.isValid(), is(false));
    assertThat(validationResult.getValidationFailures().size(), is(1));
    assertThat(validationResult.getValidationFailures().get(0).getQualifiedTableName(), is(DATABASE + "." + TABLE2));
  }

  @Test
  public void validateFailsOnTableProperty() throws Exception {
    // table 1 has all properties set
    HashMap<String, String> parameters = new HashMap<>();
    parameters.put("prop1", "");
    parameters.put("prop2", "");
    hiveTable1.setParameters(parameters);
    when(metastore.getTable(DATABASE, TABLE1)).thenReturn(hiveTable1);
    // table 2 has not
    HashMap<String, String> parameters2 = new HashMap<>();
    parameters2.put("prop1", "");
    hiveTable2.setParameters(parameters2);
    when(metastore.getTable(DATABASE, TABLE2)).thenReturn(hiveTable2);

    TablesValidation tablesValidation = new TablesValidation();
    tablesValidation.setCheckPropertyExists(Lists.newArrayList("prop1", "prop2"));
    TablesValidator validator = new TablesValidator(tablesValidation);
    ValidationResult validationResult = validator.validate(metastore, tables);

    assertThat(validationResult.isValid(), is(false));
    assertThat(validationResult.getValidationFailures().size(), is(1));
    assertThat(validationResult.getValidationFailures().get(0).getQualifiedTableName(), is(DATABASE + "." + TABLE2));
  }

  @Test
  public void validateFailsOnTableParametersIsNull() throws Exception {
    // table 1 has all properties set
    hiveTable1.setParameters(null);
    when(metastore.getTable(DATABASE, TABLE1)).thenReturn(hiveTable1);
    hiveTable2.setParameters(null);
    when(metastore.getTable(DATABASE, TABLE2)).thenReturn(hiveTable2);

    TablesValidation tablesValidation = new TablesValidation();
    tablesValidation.setCheckPropertyExists(Lists.newArrayList("prop1"));
    TablesValidator validator = new TablesValidator(tablesValidation);
    ValidationResult validationResult = validator.validate(metastore, tables);

    assertThat(validationResult.isValid(), is(false));
    assertThat(validationResult.getValidationFailures().size(), is(2));
    assertThat(validationResult.getValidationFailures().get(0).getQualifiedTableName(), is(DATABASE + "." + TABLE1));
    assertThat(validationResult.getValidationFailures().get(1).getQualifiedTableName(), is(DATABASE + "." + TABLE2));
  }

}
