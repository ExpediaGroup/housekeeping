/**
 * Copyright (C) 2017-2018 Expedia Inc.
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
package com.hotels.housekeeping.tool.vacuum;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;

import com.hotels.bdp.circustrain.api.Modules;
import com.hotels.bdp.circustrain.core.conf.SpringExpressionParser;
import com.hotels.bdp.circustrain.manifest.ManifestAttributes;

@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(value = { "com.hotels.housekeeping.tool.vacuum" }, basePackageClasses = {
    SpringExpressionParser.class }, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = {
            "com.hotels.bdp.circustrain.core.conf.TableReplications",
            "com.hotels.bdp.circustrain.core.conf.SourceCatalog",
            "com.hotels.bdp.circustrain.core.conf.ReplicaCatalog" }) })
public class VacuumTool {
  private static final Logger LOG = LoggerFactory.getLogger(VacuumTool.class);

  public static void main(String[] args) throws Exception {
    // below is output *before* logging is configured so will appear on console
    logVersionInfo();

    try {
      SpringApplication.exit(new SpringApplicationBuilder(VacuumTool.class)
          .properties("spring.config.location:${config:null}")
          .properties("spring.profiles.active:" + Modules.REPLICATION)
          .properties("instance.home:${housekeeping.h2.home}")
          .properties("instance.name:${housekeeping.h2.db}")
          .bannerMode(Mode.OFF)
          .registerShutdownHook(true)
          .build()
          .run(args));
    } catch (BeanCreationException e) {
      if (e.getMostSpecificCause() instanceof BindException) {
        printVacuumToolHelp(((BindException) e.getMostSpecificCause()).getAllErrors());
      }
      throw e;
    }
  }

  private static void printVacuumToolHelp(List<ObjectError> allErrors) {
    System.out.println(new VacuumToolHelp(allErrors));
  }

  VacuumTool() {
    // below is output *after* logging is configured so will appear in log file
    logVersionInfo();
  }

  private static void logVersionInfo() {
    ManifestAttributes manifestAttributes = new ManifestAttributes(VacuumTool.class);
    LOG.info("{}", manifestAttributes);
  }

}
