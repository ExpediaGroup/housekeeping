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
package com.hotels.housekeeping;

import javax.sql.DataSource;

import org.apache.hadoop.conf.Configuration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;

@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan("com.hotels.housekeeping")
@EntityScan(basePackages = { "com.hotels.housekeeping.model" })
@EnableJpaRepositories(basePackages = { "com.hotels.housekeeping.repository" })
public class TestApplication {

  @Bean
  public Configuration replicaHiveConf() {
    return new Configuration();
  }

  @Bean
  DatabaseConfigBean dbUnitDatabaseConfig() {
    DatabaseConfigBean databaseConfigBean = new DatabaseConfigBean();
    databaseConfigBean.setQualifiedTableNames(true);
    databaseConfigBean.setCaseSensitiveTableNames(false);
    return databaseConfigBean;
  }

  @Bean
  DatabaseDataSourceConnectionFactoryBean dbUnitDatabaseConnection(
      DatabaseConfigBean dbUnitDatabaseConfig,
      DataSource dataSource) {
    DatabaseDataSourceConnectionFactoryBean factory = new DatabaseDataSourceConnectionFactoryBean();
    factory.setDatabaseConfig(dbUnitDatabaseConfig);
    factory.setDataSource(dataSource);
    return factory;
  }
}
