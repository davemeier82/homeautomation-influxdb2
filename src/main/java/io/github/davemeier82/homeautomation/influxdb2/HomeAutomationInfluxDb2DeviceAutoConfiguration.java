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
import io.github.davemeier82.homeautomation.core.repositories.DevicePropertyValueRepository;
import io.github.davemeier82.homeautomation.core.updater.PowerValueUpdateService;
import io.github.davemeier82.homeautomation.core.updater.RelayStateValueUpdateService;
import io.github.davemeier82.homeautomation.influxdb2.device.InfluxDb2DeviceFactory;
import io.github.davemeier82.homeautomation.spring.core.HomeAutomationCoreValueUpdateServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;


@Configuration
@AutoConfigureAfter(HomeAutomationCoreValueUpdateServiceAutoConfiguration.class)
public class HomeAutomationInfluxDb2DeviceAutoConfiguration {

  @Bean
  @ConditionalOnBean({InfluxDBClient.class, PowerValueUpdateService.class, PowerValueUpdateService.class, RelayStateValueUpdateService.class, DevicePropertyValueRepository.class})
  InfluxDb2DeviceFactory influxDb2DeviceFactory(InfluxDBClient influxDBClient,
                                                TaskScheduler influxDb2TaskScheduler,
                                                PowerValueUpdateService powerValueUpdateService,
                                                RelayStateValueUpdateService relayStateValueUpdateService,
                                                DevicePropertyValueRepository devicePropertyValueRepository
  ) {
    return new InfluxDb2DeviceFactory(influxDb2TaskScheduler, influxDBClient.getQueryApi(), powerValueUpdateService, relayStateValueUpdateService, devicePropertyValueRepository);
  }

}
