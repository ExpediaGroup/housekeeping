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
package com.hotels.housekeeping;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.google.common.collect.ImmutableMap;

import com.hotels.housekeeping.conf.Housekeeping;
import com.hotels.housekeeping.converter.StringToDurationConverter;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(Housekeeping.class)
@EnableTransactionManagement
public class HousekeepingConfiguration {
  private final static Logger log = LoggerFactory.getLogger(HousekeepingConfiguration.class);

  private static final String HOUSEKEEPING_ENVIRONMENT = "housekeepingEnvironment";

  @Autowired
  private ConfigurableEnvironment env;

  @Autowired
  private ApplicationContext springContext;

  @PostConstruct
  public void postConstruct() {
    Map<String, Object> properties = (Map<String, Object>) springContext.getBean(HOUSEKEEPING_ENVIRONMENT);
    env.getPropertySources().addLast(new MapPropertySource(HOUSEKEEPING_ENVIRONMENT, properties));
    log.info("--- env = {} ---", env);
    log.info("--- env.getPropertySources() = {} ---", env.getPropertySources()); // ==
                                                                                 // commandLineArgs,systemProperties,systemEnvironment,random,applicationConfig:
                                                                                 // [file:/home/hadoop/specific-schema.yml],defaultProperties,housekeepingEnvironment]
    log.info("--- env.getProperty(applicationConfig) = {} ---", env.getProperty("applicationConfig")); // == null
    log.info("--- env.getProperty(housekeepingEnvironment) = {} ---", env.getProperty("housekeepingEnvironment")); // ==
                                                                                                                   // null
    log.info("--- env.getProperty(housekeeping.schema-name) = {} ---", env.getProperty("housekeeping.schema-name")); // ==
                                                                                                                     // custom_database
    log
        .info("--- env.getProperty(housekeeping.db-init-script) = {} ---",
            env.getProperty("housekeeping.db-init-script")); // == null
  }

  @Bean
  @ConditionalOnMissingBean(name = HOUSEKEEPING_ENVIRONMENT)
  public Map<String, Object> housekeepingEnvironment() {

    String schema = "${housekeeping.db-init-script:classpath:/schema.sql}";
    log.info(">>> env = {} >>>", env);
    log.info(">>> env.getPropertySources() = {} >>>", env.getPropertySources());
    log.info(">>> env.getProperty(applicationConfig) = {} >>>", env.getProperty("applicationConfig"));
    Object sysEnv = env.getPropertySources().get("systemEnvironment").getProperty("housekeeping.schema-name");
    Object sysProp = env.getPropertySources().get("systemProperties").getProperty("housekeeping.schema-name");
    Object defaultProp = env.getPropertySources().get("defaultProperties").getProperty("housekeeping.schema-name");
    Object applicationConfig = env.getPropertySources().get("applicationConfig"); // == null
    Object applicationConfigurationProperties = env.getPropertySources().get("applicationConfigurationProperties"); // ==
                                                                                                                    // null
    // hibernate.default_schema

    // or applicationConfigurationProperties
    log.info(">>> get(\"systemEnvironment\").getProperty(\"housekeeping.schema-name\") = {} >>>", sysEnv); // null
    log.info(">>> get(\"systemProperties\").getProperty(\"housekeeping.schema-name\") = {} >>>", sysProp); // null
    log.info(">>> get(\"defaultProperties\").getProperty(\"housekeeping.schema-name\") = {} >>>", defaultProp); // circus_train

    String envPropertyString = ">>> env.getProperty({}) >>>";
    String housekeepingSchema = "housekeeping.schema-name";
    String hibernateSchema = "hibernate.default_schema";
    String housekeepingInitScript = "housekeeping.db-init-script";
    String initScript = env.getProperty(housekeepingInitScript);
    String springSchema = "spring.datasource.schema";

    log.info("{} = {} >>>", envPropertyString, housekeepingSchema, env.getProperty(housekeepingSchema)); // ==
    log.info(">>> env.getProperty({}) = {} >>>", housekeepingSchema, env.getProperty(housekeepingSchema)); // ==
                                                                                                           // custom_schema
    log.info(">>> env.getProperty({}) = {} >>>", hibernateSchema, env.getProperty(hibernateSchema)); // == null
    log.info(">>> env.getProperty({}) = {} >>>", housekeepingInitScript, env.getProperty(housekeepingInitScript)); // ==
                                                                                                                   // null
    log.info(">>> env.getProperty({}) = {} >>>", springSchema, env.getProperty(springSchema)); // doesn't exist yet;
                                                                                               // housekeepingEnvironment

    // if default property says that schema is circustrain but there is a different schema set, schema should be ""
    if ("circus_train".equals(defaultProp) && !"circus_train".equals(env.getProperty(housekeepingSchema))) {
      // schema = "";
      log.info(">>> Schema would be null here >>>");
    }

    if (!defaultProp.equals(env.getProperty(housekeepingSchema))) {
      log
          .info(">>> default property is not equal to housekeeping.schema-name ; schema should become {} >>>",
              initScript);
      if (initScript != null) {
        schema = env.getProperty("housekeeping.db-init-script");
      } else {
        schema = "";
      }
    }

    Map<String, Object> properties = ImmutableMap
        .<String, Object>builder()
        .put("spring.jpa.hibernate.ddl-auto", "update")
        .put("spring.jpa.hibernate.generate-ddl", true)
        .put("spring.jpa.properties.org.hibernate.envers.store_data_at_delete", true)
        .put("spring.jpa.properties.hibernate.listeners.envers.autoRegister", false)
        .put("spring.jpa.properties.hibernate.default_schema", "${housekeeping.schema-name:housekeeping}")
        .put("spring.datasource.initialize", true)
        .put("spring.datasource.max-wait", 10000)
        .put("spring.datasource.max-active", 2)
        .put("spring.datasource.test-on-borrow", true)
        .put("spring.datasource.schema", schema)
        .put("housekeeping.h2.database", "${instance.home}/data/${instance.name}/housekeeping")
        .put("housekeeping.data-source.url",
            "jdbc:h2:${housekeeping.h2.database};AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE")
        .build();
    return properties;
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean(name = "housekeepingDataSource")
  DataSource housekeepingDataSource(Housekeeping housekeeping) {
    return DataSourceBuilder
        .create()
        .driverClassName(housekeeping.getDataSource().getDriverClassName())
        .url(housekeeping.getDataSource().getUrl())
        .username(housekeeping.getDataSource().getUsername())
        .password(housekeeping.getDataSource().getPassword())
        .build();
  }

  @Bean
  @ConfigurationPropertiesBinding
  StringToDurationConverter stringToDurationConverter() {
    return new StringToDurationConverter();
  }

}
