/*
 * Copyright 2021-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.davemeier82.homeautomation.influxdb2;

import com.github.davemeier82.homeautomation.spring.core.HomeAutomationCoreAutoConfiguration;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(HomeAutomationCoreAutoConfiguration.class)
public class HomeAutomationInfluxDb2AutoConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "influxdb2", name = "url")
  InfluxDBClient influxDBClient(@Value("${influxdb2.url}") String url,
                                @Value("${influxdb2.token}") char[] token,
                                @Value("${influxdb2.organization}") String organization,
                                @Value("${influxdb2.url}") String bucket
  ) {
    return InfluxDBClientFactory.create(url, token, organization, bucket);
  }

  @Bean
  @ConditionalOnBean(InfluxDBClient.class)
  InfluxDb2DeviceStateRepository influxDb2DeviceStateRepository(InfluxDBClient influxDBClient) {
    return new InfluxDb2DeviceStateRepository(influxDBClient);
  }

}
