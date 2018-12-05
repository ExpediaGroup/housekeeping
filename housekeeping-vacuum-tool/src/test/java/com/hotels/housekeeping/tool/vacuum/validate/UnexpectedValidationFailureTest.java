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
