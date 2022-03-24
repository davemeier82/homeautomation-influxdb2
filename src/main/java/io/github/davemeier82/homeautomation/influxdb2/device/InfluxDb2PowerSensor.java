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
import io.github.davemeier82.homeautomation.core.device.property.DeviceProperty;
import io.github.davemeier82.homeautomation.core.device.property.defaults.DefaultPowerSensor;
import io.github.davemeier82.homeautomation.core.device.property.defaults.DefaultReadOnlyRelay;
import io.github.davemeier82.homeautomation.core.event.DataWithTimestamp;
import io.github.davemeier82.homeautomation.core.event.EventPublisher;
import io.github.davemeier82.homeautomation.core.event.factory.EventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Devices that pulls power values from a influx 2 database (https://www.influxdata.com).
 *
 * @author David Meier
 * @since 0.1.0
 */
public class InfluxDb2PowerSensor implements Device {
  private static final Logger log = LoggerFactory.getLogger(InfluxDb2PowerSensor.class);
  public static final String TYPE = "influxdb2-power";
  public static final String QUERY_PARAMETER = "query";
  public static final String ON_THRESHOLD_PARAMETER = "onThreshold";
  public static final String OFF_THRESHOLD_PARAMETER = "offThreshold";
  public static final String UPDATE_CRON_EXPRESSION_PARAMETER = "updateCronExpression";
  public static final String VERSION_PARAMETER = "version";
  public static final String PARAMETER_VERSION = "1.0.0";
  private final String id;
  private String displayName;
  private Map<String, String> customIdentifiers;
  private final DefaultPowerSensor powerSensor;
  private final DefaultReadOnlyRelay readOnlyRelay;
  private final QueryApi queryApi;
  private final String query;
  private final double onThreshold;
  private final double offThreshold;

  /**
   * Constructor.
   *
   * @param id                the id
   * @param displayName       the display name
   * @param eventPublisher    the event publisher
   * @param eventFactory      the event factory
   * @param queryApi          the query API
   * @param query             the query that returns the power value
   * @param onThreshold       the threshold that triggers a relay on event
   * @param offThreshold      the threshold that triggers a relay off event
   * @param customIdentifiers optional custom identifiers
   */
  public InfluxDb2PowerSensor(String id,
                              String displayName,
                              EventPublisher eventPublisher,
                              EventFactory eventFactory,
                              QueryApi queryApi,
                              String query,
                              double onThreshold,
                              double offThreshold,
                              Map<String, String> customIdentifiers
  ) {
    this.id = id;
    this.displayName = displayName;
    this.queryApi = queryApi;
    this.query = query;
    this.onThreshold = onThreshold;
    this.offThreshold = offThreshold;
    powerSensor = new DefaultPowerSensor(0, this, eventPublisher, eventFactory);
    readOnlyRelay = new DefaultReadOnlyRelay(1, this, eventPublisher, eventFactory);
    this.customIdentifiers = customIdentifiers;
  }

  /**
   * This method gets called by the scheduler to pull new data form the influx database
   */
  public void checkState() {
    log.debug("reading power value of {}", displayName);
    List<FluxTable> tables = queryApi.query(query);
    if (tables.isEmpty()) {
      return;
    }
    List<FluxRecord> records = tables.get(0).getRecords();
    if (records.isEmpty()) {
      return;
    }
    FluxRecord record = records.get(0);
    DataWithTimestamp<Double> powerWithTimestamp = new DataWithTimestamp<>(requireNonNull(record.getTime()).atZone(ZoneId.systemDefault()),
        (Double) record.getValueByKey("_value"));

    powerSensor.setWatt(powerWithTimestamp);
    if (readOnlyRelay.isOn().isEmpty()) {
      readOnlyRelay.setRelayStateTo(powerWithTimestamp.getValue() >= onThreshold);
    } else {
      boolean isOn = readOnlyRelay.isOn().get().getValue();
      if (isOn && powerWithTimestamp.getValue() <= offThreshold) {
        readOnlyRelay.setRelayStateTo(false);
        log.debug("{} state change to on", displayName);
      } else if (!isOn && powerWithTimestamp.getValue() >= onThreshold) {
        readOnlyRelay.setRelayStateTo(true);
        log.debug("{} state change to off", displayName);
      }
    }
  }

  @Override
  public String getType() {
    return TYPE;
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
  public List<? extends DeviceProperty> getDeviceProperties() {
    return List.of(powerSensor, readOnlyRelay);
  }

  @Override
  public Map<String, String> getParameters() {
    return Map.of(
        QUERY_PARAMETER, query,
        ON_THRESHOLD_PARAMETER, String.valueOf(onThreshold),
        OFF_THRESHOLD_PARAMETER, String.valueOf(offThreshold),
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
}
