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
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import io.github.davemeier82.homeautomation.core.device.property.DevicePropertyId;
import io.github.davemeier82.homeautomation.core.device.property.DevicePropertyValueType;
import io.github.davemeier82.homeautomation.core.event.DataWithTimestamp;
import io.github.davemeier82.homeautomation.core.repositories.DevicePropertyValueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;

public class InfluxDb2DeviceStateRepository implements DevicePropertyValueRepository, DisposableBean {

  private static final String VALUE_FIELD_NAME = "value";
  private static final Logger log = LoggerFactory.getLogger(InfluxDb2DeviceStateRepository.class);
  private final WriteApi writeApi;
  private final QueryApi queryApi;
  private final String bucket;

  public InfluxDb2DeviceStateRepository(InfluxDBClient influxDBClient, String bucket) {
    writeApi = influxDBClient.makeWriteApi();
    queryApi = influxDBClient.getQueryApi();
    this.bucket = bucket;
  }

  static <T> T cast(Object value, Class<T> clazz) {

    if (value == null) {
      return null;
    }

    if (value.getClass().equals(clazz)) {
      return (T) value;
    }

    if (value instanceof Number number) {
      if (clazz.equals(Long.class)) {
        return (T) Long.valueOf(number.longValue());
      } else if (clazz.equals(Integer.class)) {
        return (T) Integer.valueOf(number.intValue());
      } else if (clazz.equals(Float.class)) {
        return (T) Float.valueOf(number.floatValue());
      } else if (clazz.equals(Double.class)) {
        return (T) Double.valueOf(number.doubleValue());
      } else if (clazz.equals(Short.class)) {
        return (T) Short.valueOf(number.shortValue());
      } else if (clazz.equals(Byte.class)) {
        return (T) Byte.valueOf(number.byteValue());
      } else if (clazz.equals(String.class)) {
        return (T) String.valueOf(number);
      } else if (clazz.equals(Boolean.class)) {
        return (T) Boolean.valueOf(number.doubleValue() > 0);
      }
    } else if (clazz.equals(Boolean.class) && value instanceof String s) {
      return (T) Boolean.valueOf(s);
    } else if (clazz.isEnum() && value instanceof String s) {
      try {
        Method valueOf = clazz.getMethod("valueOf", String.class);
        Object e = valueOf.invoke(null, s);
        return (T) e;
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        log.error("cast from {} to {} is not supported", value.getClass(), clazz);
      }
    }

    log.error("cast from {} to {} is not supported", value.getClass(), clazz);
    return null;
  }

  @Override
  public void insert(DevicePropertyId devicePropertyId, DevicePropertyValueType devicePropertyValueType, String displayName, Object value, OffsetDateTime time) {
    Point point = new Point(devicePropertyValueType.getTypeName());
    point.addTag("devicePropertyId", devicePropertyId.id());
    point.addTag("deviceId", devicePropertyId.deviceId().id());
    point.addTag("deviceType", devicePropertyId.deviceId().type().getTypeName());
    point.addTag("unit", devicePropertyValueType.getUnit());
    point.addTag("displayName", displayName);
    point.time(time.toInstant().toEpochMilli(), WritePrecision.MS);
    switch (value) {
      case Boolean b -> point.addField(VALUE_FIELD_NAME, b);
      case Integer i -> point.addField(VALUE_FIELD_NAME, i);
      case Float f -> point.addField(VALUE_FIELD_NAME, f);
      case Double d -> point.addField(VALUE_FIELD_NAME, d);
      case Long l -> point.addField(VALUE_FIELD_NAME, l);
      case Number n -> point.addField(VALUE_FIELD_NAME, n);
      case String s -> point.addField(VALUE_FIELD_NAME, s);
      case Enum<?> e -> point.addField(VALUE_FIELD_NAME, e.name());
      default -> point.addField(VALUE_FIELD_NAME, value.toString());
    }
    writeApi.writePoint(point);
  }

  @Override
  public <T> Optional<DataWithTimestamp<T>> findLatestValue(DevicePropertyId devicePropertyId, DevicePropertyValueType devicePropertyValueType, Class<T> clazz) {
    List<FluxTable> tables = queryApi.query(
        "from(bucket: \"" + bucket + "\")\n" +
            "  |> range(start: 0)\n" +
            "  |> filter(fn: (r) => r.devicePropertyId == \"" + devicePropertyId.id() + "\")\n" +
            "  |> filter(fn: (r) => r.deviceId == \"" + devicePropertyId.deviceId().id() + "\")\n" +
            "  |> filter(fn: (r) => r.deviceType == \"" + devicePropertyId.deviceId().type().getTypeName() + "\")\n" +
            "  |> filter(fn: (r) => r._measurement == \"" + devicePropertyValueType.getTypeName() + "\")\n" +
            "  |> filter(fn: (r) => r._field == \"" + VALUE_FIELD_NAME + "\")\n" + "  |> last()");

    if (tables.isEmpty()) {
      return Optional.empty();
    }
    List<FluxRecord> records = tables.getFirst().getRecords();
    if (records.isEmpty()) {
      return Optional.empty();
    }
    FluxRecord record = records.getFirst();

    Object value = record.getValueByKey("_value");
    T mapped = cast(value, clazz);

    return Optional.of(new DataWithTimestamp<>(requireNonNull(record.getTime()).atOffset(UTC), mapped));
  }

  @Override
  public Optional<OffsetDateTime> lastTimeValueMatched(DevicePropertyId devicePropertyId, DevicePropertyValueType devicePropertyValueType, Object value) {
    List<FluxTable> tables = queryApi.query(
        "from(bucket: \"" + bucket + "\")\n" +
            "  |> range(start: 0)\n" +
            "  |> filter(fn: (r) => r.devicePropertyId == \"" + devicePropertyId.id() + "\")\n" +
            "  |> filter(fn: (r) => r.deviceId == \"" + devicePropertyId.deviceId().id() + "\")\n" +
            "  |> filter(fn: (r) => r.deviceType == \"" + devicePropertyId.deviceId().type().getTypeName() + "\")\n" +
            "  |> filter(fn: (r) => r._measurement == \"" + devicePropertyValueType.getTypeName() + "\")\n" +
            "  |> filter(fn: (r) => r._field == \"" + VALUE_FIELD_NAME + "\")\n" +
            "  |> filter(fn: (r) => r._value == \"" + value + "\")\n" +
            "  |> last()");

    if (tables.isEmpty()) {
      return Optional.empty();
    }

    List<FluxRecord> records = tables.getFirst().getRecords();
    if (records.isEmpty()) {
      return Optional.empty();
    }
    FluxRecord record = records.getFirst();
    return Optional.of(requireNonNull(record.getTime()).atOffset(UTC));
  }

  @Override
  public void destroy() {
    writeApi.close();
  }
}
