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
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import io.github.davemeier82.homeautomation.core.device.Device;
import io.github.davemeier82.homeautomation.core.device.DeviceId;
import io.github.davemeier82.homeautomation.core.device.DeviceType;
import io.github.davemeier82.homeautomation.core.device.property.DefaultDevicePropertyValueType;
import io.github.davemeier82.homeautomation.core.device.property.DevicePropertyId;
import io.github.davemeier82.homeautomation.core.event.DataWithTimestamp;
import io.github.davemeier82.homeautomation.core.repositories.DevicePropertyValueRepository;
import io.github.davemeier82.homeautomation.core.updater.PowerValueUpdateService;
import io.github.davemeier82.homeautomation.core.updater.RelayStateValueUpdateService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.davemeier82.homeautomation.influxdb2.device.InfluxDb2DeviceType.INFLUX_DB2_POWER_SENSOR;
import static java.util.Objects.requireNonNull;

public class InfluxDb2PowerSensor implements Device {
  private static final Logger log = LoggerFactory.getLogger(InfluxDb2PowerSensor.class);

  public static final String QUERY_PARAMETER = "query";
  public static final String ON_THRESHOLD_PARAMETER = "onThreshold";
  public static final String OFF_THRESHOLD_PARAMETER = "offThreshold";
  public static final String UPDATE_CRON_EXPRESSION_PARAMETER = "updateCronExpression";
  public static final String VERSION_PARAMETER = "version";
  public static final String PARAMETER_VERSION = "1.0.0";
  private final String id;
  private String displayName;
  private Map<String, String> customIdentifiers;
  private final PowerValueUpdateService powerValueUpdateService;
  private final RelayStateValueUpdateService relayStateValueUpdateService;
  private final DevicePropertyValueRepository devicePropertyValueRepository;
  private final QueryApi queryApi;
  private final String query;
  private final double onThreshold;
  private final double offThreshold;
  private final String cronExpression;

  private final DevicePropertyId relayDevicePropertyId;
  private final DevicePropertyId powerDevicePropertyId;

  public InfluxDb2PowerSensor(String id,
                              String displayName,
                              QueryApi queryApi,
                              String query,
                              double onThreshold,
                              double offThreshold,
                              String cronExpression,
                              Map<String, String> customIdentifiers,
                              PowerValueUpdateService powerValueUpdateService,
                              RelayStateValueUpdateService relayStateValueUpdateService,
                              DevicePropertyValueRepository devicePropertyValueRepository
  ) {
    this.id = id;
    this.displayName = displayName;
    this.queryApi = queryApi;
    this.query = query;
    this.onThreshold = onThreshold;
    this.offThreshold = offThreshold;
    this.cronExpression = cronExpression;
    this.customIdentifiers = customIdentifiers;
    this.powerValueUpdateService = powerValueUpdateService;
    this.relayStateValueUpdateService = relayStateValueUpdateService;
    this.devicePropertyValueRepository = devicePropertyValueRepository;
    DeviceId deviceId = new DeviceId(id, INFLUX_DB2_POWER_SENSOR);
    relayDevicePropertyId = new DevicePropertyId(deviceId, "relay");
    powerDevicePropertyId = new DevicePropertyId(deviceId, "power");
  }

  /**
   * This method gets called by the scheduler to pull new data form the influx database
   */
  @SchedulerLock(name = "InfluxDb2PowerSensor",
      lockAtLeastFor = "PT5S", lockAtMostFor = "PT1M")
  public void checkState() {
    log.debug("reading power value of {}", displayName);
    List<FluxTable> tables = queryApi.query(query);
    if (tables.isEmpty()) {
      return;
    }
    List<FluxRecord> records = tables.getFirst().getRecords();
    if (records.isEmpty()) {
      return;
    }
    List<DataWithTimestamp<Double>> values = records.stream()
                                                    .map(record -> new DataWithTimestamp<>(requireNonNull(record.getTime()).atOffset(ZoneOffset.UTC), (Double) record.getValueByKey("_value")))
                                                    .toList();
    if (values.isEmpty()) {
      log.info("no new values");
      return;
    }

    powerValueUpdateService.setValue(values.getLast().getValue(), values.getLast().getDateTime(), powerDevicePropertyId, displayName);

    isOn().ifPresentOrElse(isOn -> {
      if (isOn) {
        Optional<DataWithTimestamp<Double>> firstOff = values.stream().filter(data -> data.getValue() <= offThreshold).findFirst();
        if (firstOff.isPresent()) {
          setRelayState(false, firstOff.get().getDateTime());
          log.debug("{} state change to off", displayName);
        } else {
          setRelayState(true, values.getLast().getDateTime());
        }
      } else {
        Optional<DataWithTimestamp<Double>> firstOn = values.stream().filter(data -> data.getValue() >= onThreshold).findFirst();
        if (firstOn.isPresent()) {
          setRelayState(true, firstOn.get().getDateTime());
          log.debug("{} state change to on", displayName);
        } else {
          setRelayState(false, values.getLast().getDateTime());
        }
      }
    }, () -> setRelayState((values.stream().anyMatch(data -> data.getValue() >= onThreshold)), values.getLast().getDateTime()));
  }

  private void setRelayState(boolean isOn, OffsetDateTime dateTime) {
    relayStateValueUpdateService.setValue(isOn, dateTime, relayDevicePropertyId, displayName);
  }

  @Override
  public DeviceType getType() {
    return INFLUX_DB2_POWER_SENSOR;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public Map<String, String> getParameters() {
    return Map.of(QUERY_PARAMETER, query, ON_THRESHOLD_PARAMETER, String.valueOf(onThreshold), OFF_THRESHOLD_PARAMETER, String.valueOf(offThreshold), UPDATE_CRON_EXPRESSION_PARAMETER, cronExpression,
        VERSION_PARAMETER, PARAMETER_VERSION);
  }

  @Override
  public Map<String, String> getCustomIdentifiers() {
    return customIdentifiers;
  }

  @Override
  public void setCustomIdentifiers(Map<String, String> customIdentifiers) {
    this.customIdentifiers = customIdentifiers;
  }

  private Optional<Boolean> isOn() {
    return devicePropertyValueRepository.findLatestValue(relayDevicePropertyId, DefaultDevicePropertyValueType.RELAY_STATE, Boolean.class).map(DataWithTimestamp::getValue);
  }
}
