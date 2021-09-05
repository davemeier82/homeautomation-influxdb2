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

import com.github.davemeier82.homeautomation.core.DeviceStateRepository;
import com.github.davemeier82.homeautomation.core.device.DeviceId;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;

import java.time.Instant;

public class InfluxDb2DeviceStateRepository implements DeviceStateRepository, DisposableBean {

  private static final String VALUE_FIELD_NAME = "value";
  private final WriteApi writeApi;

  public InfluxDb2DeviceStateRepository(InfluxDBClient influxDBClient) {
    writeApi = influxDBClient.makeWriteApi();
  }

  @Override
  public void insert(DeviceId deviceId, String category, double value, Instant time) {
    Point point = createPoint(deviceId, category, time);
    point.addField(VALUE_FIELD_NAME, value);
    writeApi.writePoint(point);
  }

  @Override
  public void insert(DeviceId deviceId, String category, int value, Instant time) {
    Point point = createPoint(deviceId, category, time);
    point.addField(VALUE_FIELD_NAME, value);
    writeApi.writePoint(point);
  }

  @Override
  public void insert(DeviceId deviceId, String category, boolean value, Instant time) {
    Point point = createPoint(deviceId, category, time);
    point.addField(VALUE_FIELD_NAME, value);
    writeApi.writePoint(point);
  }

  @NotNull
  private Point createPoint(DeviceId deviceId, String category, Instant time) {
    Point point = new Point(category);
    point.addTag("deviceType", deviceId.getType());
    point.addTag("deviceId", deviceId.getId());
    point.time(time.toEpochMilli(), WritePrecision.MS);
    return point;
  }

  @Override
  public void destroy() {
    writeApi.close();
  }
}
