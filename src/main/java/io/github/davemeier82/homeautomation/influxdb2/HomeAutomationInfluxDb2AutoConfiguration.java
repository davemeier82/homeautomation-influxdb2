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

package io.github.davemeier82.homeautomation.influxdb2;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import io.github.davemeier82.homeautomation.core.repositories.DeviceRepository;
import io.github.davemeier82.homeautomation.influxdb2.device.InfluxDb2DeviceTypeFactory;
import io.github.davemeier82.homeautomation.spring.core.HomeAutomationCoreAutoConfiguration;
import io.github.davemeier82.homeautomation.spring.core.HomeAutomationCorePersistenceAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


@Configuration
@AutoConfigureBefore(HomeAutomationCoreAutoConfiguration.class)
@AutoConfigureAfter(HomeAutomationCorePersistenceAutoConfiguration.class)
public class HomeAutomationInfluxDb2AutoConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "homeautomation.influxdb2", name = "url")
  InfluxDBClient influxDBClient(@Value("${homeautomation.influxdb2.url}") String url,
                                @Value("${homeautomation.influxdb2.token}") char[] token,
                                @Value("${homeautomation.influxdb2.organization}") String organization,
                                @Value("${homeautomation.influxdb2.bucket}") String bucket
  ) {
    return InfluxDBClientFactory.create(url, token, organization, bucket);
  }
  
  @Bean
  @ConditionalOnBean({InfluxDBClient.class, DeviceRepository.class})
  @Primary
  InfluxDb2DeviceStateRepository influxDb2DeviceStateRepository(InfluxDBClient influxDBClient, @Value("${homeautomation.influxdb2.bucket}") String bucket, @Lazy DeviceRepository deviceRepository) {
    return new InfluxDb2DeviceStateRepository(influxDBClient, bucket, deviceRepository);
  }

  @Bean
  @ConditionalOnBean(InfluxDBClient.class)
  TaskScheduler influxDb2TaskScheduler(@Value("${homeautomation.influxdb2.task-scheduler.pool-size:3}") int poolSize) {
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(poolSize);
    threadPoolTaskScheduler.setThreadNamePrefix("influxDb2TaskScheduler");
    return threadPoolTaskScheduler;
  }

  @Bean
  @ConditionalOnMissingBean
  InfluxDb2DeviceTypeFactory influxDb2DeviceTypeFactory() {
    return new InfluxDb2DeviceTypeFactory();
  }

}
