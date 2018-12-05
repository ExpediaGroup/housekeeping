package com.hotels.housekeeping.tool.vacuum.conf;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.collect.Lists;

public class TablesValidationTest {

  @Test
  public void defaults() throws Exception {
    TablesValidation tablesValidation = new TablesValidation();

    assertThat(tablesValidation.getCheckPropertyExists().size(), is(1));
    assertThat(tablesValidation.getCheckPropertyExists().get(0), is("com.hotels.bdp.circustrain.replication.event"));
  }

  @Test
  public void overrideDefaults() throws Exception {
    TablesValidation tablesValidation = new TablesValidation();
    tablesValidation.setCheckPropertyExists(Lists.newArrayList("override"));

    assertThat(tablesValidation.getCheckPropertyExists().size(), is(1));
    assertThat(tablesValidation.getCheckPropertyExists().get(0), is("override"));
  }

}
