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

package com.github.davemeier82.homeautomation.influxdb2.device;

import com.github.davemeier82.homeautomation.core.device.Device;
import com.github.davemeier82.homeautomation.core.device.DeviceFactory;
import com.github.davemeier82.homeautomation.core.event.factory.EventFactory;
import com.github.davemeier82.homeautomation.core.event.EventPublisher;
import com.influxdb.client.QueryApi;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Map;
import java.util.Set;

import static com.github.davemeier82.homeautomation.influxdb2.device.InfluxDb2PowerSensor.*;
import static java.lang.Double.parseDouble;

public class InfluxDb2DeviceFactory implements DeviceFactory {

  private final EventPublisher eventPublisher;
  private final EventFactory eventFactory;
  private final TaskScheduler scheduler;
  private final QueryApi queryApi;

  public InfluxDb2DeviceFactory(EventPublisher eventPublisher,
                                EventFactory eventFactory,
                                TaskScheduler scheduler,
                                QueryApi queryApi
  ) {
    this.eventPublisher = eventPublisher;
    this.eventFactory = eventFactory;
    this.scheduler = scheduler;
    this.queryApi = queryApi;
  }

  @Override
  public boolean supportsDeviceType(String type) {
    return TYPE.equals(type);
  }

  @Override
  public Set<String> getSupportedDeviceTypes() {
    return Set.of(TYPE);
  }

  @Override
  public Device createDevice(String type, String id, String displayName, Map<String, String> parameters) {
    if (supportsDeviceType(type)) {
      InfluxDb2PowerSensor influxDb2PowerSensor = new InfluxDb2PowerSensor(id,
          displayName,
          eventPublisher,
          eventFactory,
          queryApi,
          parameters.get(QUERY_PARAMETER),
          parseDouble(parameters.get(ON_THRESHOLD_PARAMETER)),
          parseDouble(parameters.get(OFF_THRESHOLD_PARAMETER)));

      scheduler.schedule(influxDb2PowerSensor::checkState, new CronTrigger(parameters.get(UPDATE_CRON_EXPRESSION_PARAMETER)));
      eventPublisher.publishEvent(eventFactory.createNewDeviceCreatedEvent(influxDb2PowerSensor));

      return influxDb2PowerSensor;
    }
    throw new IllegalArgumentException("device type '" + type + "' not supported");
  }

}
