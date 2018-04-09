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
package com.hotels.housekeeping.tool.vacuum.conf;

import static org.apache.hadoop.security.alias.CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH;

import static com.hotels.bdp.circustrain.core.metastore.TunnellingMetaStoreClientSupplier.TUNNEL_SSH_LOCAL_HOST;
import static com.hotels.bdp.circustrain.core.metastore.TunnellingMetaStoreClientSupplier.TUNNEL_SSH_ROUTE;

import java.util.Arrays;
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

import com.hotels.bdp.circustrain.api.metastore.CloseableMetaStoreClient;
import com.hotels.bdp.circustrain.api.metastore.MetaStoreClientFactory;
import com.hotels.bdp.circustrain.core.conf.MetastoreTunnel;
import com.hotels.bdp.circustrain.core.conf.Security;
import com.hotels.bdp.circustrain.core.conf.TunnelMetastoreCatalog;
import com.hotels.bdp.circustrain.core.metastore.DefaultMetaStoreClientSupplier;
import com.hotels.bdp.circustrain.core.metastore.HiveConfFactory;
import com.hotels.bdp.circustrain.core.metastore.MetaStoreClientFactoryManager;
import com.hotels.bdp.circustrain.core.metastore.SessionFactorySupplier;
import com.hotels.bdp.circustrain.core.metastore.ThriftMetaStoreClientFactory;
import com.hotels.bdp.circustrain.core.metastore.TunnelConnectionManagerFactory;
import com.hotels.bdp.circustrain.core.metastore.TunnellingMetaStoreClientSupplier;
import com.hotels.housekeeping.repository.LegacyReplicaPathRepository;
import com.hotels.housekeeping.service.HousekeepingService;
import com.hotels.housekeeping.service.impl.FileSystemHousekeepingService;

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
    configureMetastoreTunnel(hiveCatalog.getMetastoreTunnel(), properties);
    putConfigurationProperties(hiveCatalog.getConfigurationProperties(), properties);
    HiveConf hiveConf = new HiveConfFactory(siteXml, properties).newInstance();
    return hiveConf;
  }

  @Bean
  Supplier<CloseableMetaStoreClient> metaStoreClientSupplier(
      Catalog catalog,
      @Value("#{hiveConf}") HiveConf hiveConf,
      MetaStoreClientFactoryManager metaStoreClientFactoryManager) {
    MetaStoreClientFactory replicaMetaStoreClientFactory = metaStoreClientFactoryManager
        .factoryForUrl(catalog.getHiveMetastoreUris());
    return metaStoreClientSupplier(catalog.getName(), hiveConf, catalog.getMetastoreTunnel(),
        replicaMetaStoreClientFactory);
  }

  private Supplier<CloseableMetaStoreClient> metaStoreClientSupplier(
      String name,
      HiveConf hiveConf,
      MetastoreTunnel metastoreTunnel,
      MetaStoreClientFactory metaStoreClientFactory) {
    if (metastoreTunnel != null) {
      SessionFactorySupplier sessionFactorySupplier = new SessionFactorySupplier(metastoreTunnel.getPort(),
          metastoreTunnel.getKnownHosts(), Arrays.asList(metastoreTunnel.getPrivateKeys().split(",")));
      return new TunnellingMetaStoreClientSupplier(hiveConf, name, metaStoreClientFactory,
          new TunnelConnectionManagerFactory(sessionFactorySupplier));
    } else {
      return new DefaultMetaStoreClientSupplier(hiveConf, name, metaStoreClientFactory);
    }
  }

  private void configureMetastoreTunnel(MetastoreTunnel metastoreTunnel, Map<String, String> properties) {
    if (metastoreTunnel != null) {
      properties.put(TUNNEL_SSH_ROUTE, metastoreTunnel.getRoute());
      properties.put(TUNNEL_SSH_LOCAL_HOST, metastoreTunnel.getLocalhost());
    }
  }

  private void putConfigurationProperties(Map<String, String> configurationProperties, Map<String, String> properties) {
    if (configurationProperties != null) {
      properties.putAll(configurationProperties);
    }
  }

  @Bean
  MetaStoreClientFactoryManager metaStoreClientFactoryManager(List<MetaStoreClientFactory> metaStoreClientFactories) {
    return new MetaStoreClientFactoryManager(metaStoreClientFactories);
  }

  @Bean
  MetaStoreClientFactory thriftMetaStoreClientFactory() {
    return new ThriftMetaStoreClientFactory();
  }

  @Bean
  HousekeepingService housekeepingService(
      LegacyReplicaPathRepository legacyReplicaPathRepository,
      @Qualifier("baseConf") org.apache.hadoop.conf.Configuration baseConf) {
    return new FileSystemHousekeepingService(legacyReplicaPathRepository, baseConf);
  }
}
