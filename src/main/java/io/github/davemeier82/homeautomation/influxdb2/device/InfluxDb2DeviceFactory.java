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

package io.github.davemeier82.homeautomation.influxdb2.device;

import com.influxdb.client.QueryApi;
import io.github.davemeier82.homeautomation.core.device.Device;
import io.github.davemeier82.homeautomation.core.device.DeviceFactory;
import io.github.davemeier82.homeautomation.core.device.DeviceType;
import io.github.davemeier82.homeautomation.core.repositories.DevicePropertyValueRepository;
import io.github.davemeier82.homeautomation.core.updater.PowerValueUpdateService;
import io.github.davemeier82.homeautomation.core.updater.RelayStateValueUpdateService;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.github.davemeier82.homeautomation.influxdb2.device.InfluxDb2DeviceType.INFLUX_DB2_POWER_SENSOR;
import static io.github.davemeier82.homeautomation.influxdb2.device.InfluxDb2PowerSensor.*;
import static java.lang.Double.parseDouble;

/**
 * Factory for Influx 2 database devices (<a href="https://www.influxdata.com">https://www.influxdata.com</a>).
 */
public class InfluxDb2DeviceFactory implements DeviceFactory {

  public static final Set<DeviceType> SUPPORTED_DEVICE_TYPES = Set.of(INFLUX_DB2_POWER_SENSOR);
  private final TaskScheduler scheduler;
  private final QueryApi queryApi;
  private final PowerValueUpdateService powerValueUpdateService;
  private final RelayStateValueUpdateService relayStateValueUpdateService;
  private final DevicePropertyValueRepository devicePropertyValueRepository;

  public InfluxDb2DeviceFactory(TaskScheduler scheduler,
                                QueryApi queryApi,
                                PowerValueUpdateService powerValueUpdateService,
                                RelayStateValueUpdateService relayStateValueUpdateService,
                                DevicePropertyValueRepository devicePropertyValueRepository
  ) {
    this.scheduler = scheduler;
    this.queryApi = queryApi;
    this.powerValueUpdateService = powerValueUpdateService;
    this.relayStateValueUpdateService = relayStateValueUpdateService;
    this.devicePropertyValueRepository = devicePropertyValueRepository;
  }

  @Override
  public boolean supportsDeviceType(DeviceType type) {
    return SUPPORTED_DEVICE_TYPES.contains(type);
  }

  @Override
  public Set<DeviceType> getSupportedDeviceTypes() {
    return SUPPORTED_DEVICE_TYPES;
  }

  @Override
  public Optional<Device> createDevice(DeviceType type, String id, String displayName, Map<String, String> parameters, Map<String, String> customIdentifiers) {
    if (supportsDeviceType(type)) {
      InfluxDb2PowerSensor influxDb2PowerSensor = new InfluxDb2PowerSensor(id,
          displayName,
          queryApi,
          parameters.get(QUERY_PARAMETER),
          parseDouble(parameters.get(ON_THRESHOLD_PARAMETER)),
          parseDouble(parameters.get(OFF_THRESHOLD_PARAMETER)),
          parameters.get(UPDATE_CRON_EXPRESSION_PARAMETER),
          customIdentifiers,
          powerValueUpdateService,
          relayStateValueUpdateService,
          devicePropertyValueRepository
      );

      scheduler.schedule(influxDb2PowerSensor::checkState, new CronTrigger(parameters.get(UPDATE_CRON_EXPRESSION_PARAMETER)));

      return Optional.of(influxDb2PowerSensor);
    }
    return Optional.empty();
  }

}
