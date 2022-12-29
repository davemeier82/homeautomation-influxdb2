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
import io.github.davemeier82.homeautomation.core.event.EventPublisher;
import io.github.davemeier82.homeautomation.core.event.factory.EventFactory;
import io.github.davemeier82.homeautomation.influxdb2.device.InfluxDb2DeviceFactory;
import io.github.davemeier82.homeautomation.spring.core.HomeAutomationCoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Auto configuration.
 *
 * @author David Meier
 * @since 0.1.0
 */
@AutoConfiguration
@AutoConfigureBefore(HomeAutomationCoreAutoConfiguration.class)
public class HomeAutomationInfluxDb2AutoConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "influxdb2", name = "url")
  InfluxDBClient influxDBClient(@Value("${influxdb2.url}") String url,
                                @Value("${influxdb2.token}") char[] token,
                                @Value("${influxdb2.organization}") String organization,
                                @Value("${influxdb2.bucket}") String bucket
  ) {
    return InfluxDBClientFactory.create(url, token, organization, bucket);
  }

  @Bean
  @ConditionalOnBean(InfluxDBClient.class)
  InfluxDb2DeviceStateRepository influxDb2DeviceStateRepository(InfluxDBClient influxDBClient, @Value("${influxdb2.bucket}") String bucket) {
    return new InfluxDb2DeviceStateRepository(influxDBClient, bucket);
  }

  @Bean
  @ConditionalOnBean(InfluxDBClient.class)
  TaskScheduler influxDb2TaskScheduler(@Value("${influxdb2.task-scheduler.pool-size:3}") int poolSize) {
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(poolSize);
    threadPoolTaskScheduler.setThreadNamePrefix("influxDb2TaskScheduler");
    return threadPoolTaskScheduler;
  }

  @Bean
  @ConditionalOnBean(InfluxDBClient.class)
  InfluxDb2DeviceFactory influxDb2DeviceFactory(EventPublisher eventPublisher,
                                                EventFactory eventFactory,
                                                InfluxDBClient influxDBClient,
                                                TaskScheduler influxDb2TaskScheduler
  ) {
    return new InfluxDb2DeviceFactory(eventPublisher, eventFactory, influxDb2TaskScheduler, influxDBClient.getQueryApi());
  }

}
