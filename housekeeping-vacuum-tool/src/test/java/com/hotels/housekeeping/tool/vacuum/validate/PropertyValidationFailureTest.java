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
import com.hotels.housekeeping.tool.vacuum.conf.TablesValidation;

public class PropertyValidationFailureTest {

  private final Table table = new Table();

  @Before
  public void setUp() {
    table.setDatabaseName("db");
    table.setTableName("table");
  }

  @Test
  public void propertyValidationFailure() throws Exception {
    TablesValidation tablesValidation = new TablesValidation();
    tablesValidation.setCheckPropertyExists(Lists.newArrayList("prop1"));
    Map<String, String> parameters = new HashMap<>();
    parameters.put("foo", "bar");
    PropertyValidationFailure failure = new PropertyValidationFailure(table, tablesValidation, parameters);
    String message = failure.getMessage();
    System.out.println(message);
    assertThat(message, containsString("prop1"));
    assertThat(message, containsString("foo=bar"));
    assertThat(failure.getQualifiedTableName(), is("db.table"));

  }

}
