/**
 * Copyright (C) 2016-2019 Expedia Inc.
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
package com.hotels.housekeeping.tool.vacuum.conf;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.collect.Lists;

public class TablesValidationConfigTest {

  @Test
  public void defaults() {
    TablesValidationConfig tablesValidationConfig = new TablesValidationConfig();

    assertThat(tablesValidationConfig.getHiveTableProperties().size(), is(1));
    assertThat(tablesValidationConfig.getHiveTableProperties().get(0), is("com.hotels.bdp.circustrain.replication.event"));
  }

  @Test
  public void overrideDefaults() {
    TablesValidationConfig tablesValidationConfig = new TablesValidationConfig();
    tablesValidationConfig.setHiveTableProperties(Lists.newArrayList("override"));

    assertThat(tablesValidationConfig.getHiveTableProperties().size(), is(1));
    assertThat(tablesValidationConfig.getHiveTableProperties().get(0), is("override"));
  }

}
