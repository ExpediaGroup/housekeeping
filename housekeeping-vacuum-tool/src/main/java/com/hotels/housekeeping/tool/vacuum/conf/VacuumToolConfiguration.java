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

import static org.apache.hadoop.security.alias.CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.conf.HiveConf;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.google.common.base.Supplier;

import com.hotels.hcommon.hive.metastore.client.api.CloseableMetaStoreClient;
import com.hotels.hcommon.hive.metastore.client.api.ConditionalMetaStoreClientFactory;
import com.hotels.hcommon.hive.metastore.client.api.MetaStoreClientFactory;
import com.hotels.hcommon.hive.metastore.client.conditional.ConditionalMetaStoreClientFactoryManager;
import com.hotels.hcommon.hive.metastore.client.conditional.ThriftHiveMetaStoreClientFactory;
import com.hotels.hcommon.hive.metastore.client.supplier.HiveMetaStoreClientSupplier;
import com.hotels.hcommon.hive.metastore.client.tunnelling.MetastoreTunnel;
import com.hotels.hcommon.hive.metastore.client.tunnelling.TunnellingMetaStoreClientSupplierBuilder;
import com.hotels.hcommon.hive.metastore.conf.HiveConfFactory;
import com.hotels.housekeeping.repository.LegacyReplicaPathRepository;
import com.hotels.housekeeping.service.HousekeepingService;
import com.hotels.housekeeping.service.impl.FileSystemHousekeepingService;
import com.hotels.housekeeping.tool.vacuum.validate.TablesValidator;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ComponentScan(VacuumToolConfiguration.HOUSEKEEPING_PACKAGE)
@EntityScan(basePackages = { VacuumToolConfiguration.HOUSEKEEPING_PACKAGE })
@EnableJpaRepositories(basePackages = { VacuumToolConfiguration.HOUSEKEEPING_PACKAGE })
public class VacuumToolConfiguration {

  final static String HOUSEKEEPING_PACKAGE = "com.hotels.housekeeping";

  private static final String BEAN_BASE_CONF = "baseConf";

  @Bean(name = BEAN_BASE_CONF)
  org.apache.hadoop.conf.Configuration baseConf(Security security) {
    Map<String, String> properties = new HashMap<>();
    setCredentialProviderPath(security, properties);
    org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      conf.set(entry.getKey(), entry.getValue());
    }
    return conf;
  }

  private void setCredentialProviderPath(Security security, Map<String, String> properties) {
    if (security.getCredentialProvider() != null) {
      // TODO perhaps we should have a source catalog scoped credential provider instead on one specific to S3?
      properties.put(CREDENTIAL_PROVIDER_PATH, security.getCredentialProvider());
    }
  }

  @Bean
  HiveConf hiveConf(Catalog catalog, @Qualifier("baseConf") org.apache.hadoop.conf.Configuration baseConf) {
    return newHiveConf(catalog, baseConf);
  }

  private HiveConf newHiveConf(TunnelMetastoreCatalog hiveCatalog, org.apache.hadoop.conf.Configuration baseConf) {
    List<String> siteXml = hiveCatalog.getSiteXml();
    Map<String, String> properties = new HashMap<>();
    for (Map.Entry<String, String> entry : baseConf) {
      properties.put(entry.getKey(), entry.getValue());
    }
    if (hiveCatalog.getHiveMetastoreUris() != null) {
      properties.put(HiveConf.ConfVars.METASTOREURIS.varname, hiveCatalog.getHiveMetastoreUris());
    }
    putConfigurationProperties(hiveCatalog.getConfigurationProperties(), properties);
    HiveConf hiveConf = new HiveConfFactory(siteXml, properties).newInstance();
    return hiveConf;
  }

  @Bean
  Supplier<CloseableMetaStoreClient> metaStoreClientSupplier(
      Catalog catalog,
      @Value("#{hiveConf}") HiveConf hiveConf,
      ConditionalMetaStoreClientFactoryManager conditionalMetaStoreClientFactoryManager) {
    MetaStoreClientFactory replicaMetaStoreClientFactory = conditionalMetaStoreClientFactoryManager
        .factoryForUri(catalog.getHiveMetastoreUris());
    return metaStoreClientSupplier(catalog.getName(), hiveConf, catalog.getMetastoreTunnel(),
        replicaMetaStoreClientFactory);
  }

  private Supplier<CloseableMetaStoreClient> metaStoreClientSupplier(
      String name,
      HiveConf hiveConf,
      MetastoreTunnel metastoreTunnel,
      MetaStoreClientFactory metaStoreClientFactory) {
    if (metastoreTunnel != null) {
      return metaStoreClientSupplier(hiveConf, name, metaStoreClientFactory, metastoreTunnel);
    } else {
      return new HiveMetaStoreClientSupplier(metaStoreClientFactory, hiveConf, name);
    }
  }

  Supplier<CloseableMetaStoreClient> metaStoreClientSupplier(
      HiveConf hiveConf,
      String name,
      MetaStoreClientFactory metaStoreClientFactory,
      MetastoreTunnel metastoreTunnel) {
    if (metastoreTunnel != null) {
      return new TunnellingMetaStoreClientSupplierBuilder()
          .withName(name)
          .withRoute(metastoreTunnel.getRoute())
          .withKnownHosts(metastoreTunnel.getKnownHosts())
          .withLocalHost(metastoreTunnel.getLocalhost())
          .withPort(metastoreTunnel.getPort())
          .withPrivateKeys(metastoreTunnel.getPrivateKeys())
          .withTimeout(metastoreTunnel.getTimeout())
          .withStrictHostKeyChecking(metastoreTunnel.getStrictHostKeyChecking())
          .build(hiveConf, metaStoreClientFactory);
    } else {
      return new HiveMetaStoreClientSupplier(metaStoreClientFactory, hiveConf, name);
    }
  }

  private void putConfigurationProperties(Map<String, String> configurationProperties, Map<String, String> properties) {
    if (configurationProperties != null) {
      properties.putAll(configurationProperties);
    }
  }

  @Bean
  ConditionalMetaStoreClientFactory thriftHiveMetaStoreClientFactory() {
    return new ThriftHiveMetaStoreClientFactory();
  }

  @Bean
  ConditionalMetaStoreClientFactoryManager conditionalMetaStoreClientFactoryManager(
      List<ConditionalMetaStoreClientFactory> factories) {
    return new ConditionalMetaStoreClientFactoryManager(factories);
  }

  @Bean
  HousekeepingService housekeepingService(
      LegacyReplicaPathRepository legacyReplicaPathRepository,
      @Qualifier("baseConf") org.apache.hadoop.conf.Configuration baseConf) {
    return new FileSystemHousekeepingService(legacyReplicaPathRepository, baseConf);
  }

  @Bean
  TablesValidator tablesValidator(TablesValidationConfig tablesValidationConfig) {
    return new TablesValidator(tablesValidationConfig);
  }
}
