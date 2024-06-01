/*
 * Copyright 2021-2024 the original author or authors.
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

import io.github.davemeier82.homeautomation.core.device.DeviceType;

import java.util.Arrays;
import java.util.Optional;

public enum InfluxDb2DeviceType implements DeviceType {
  INFLUX_DB2_POWER_SENSOR("influxdb2-power");

  private final String typeName;

  InfluxDb2DeviceType(String typeName) {
    this.typeName = typeName;
  }

  public static Optional<? extends DeviceType> getByTypeName(String typeName) {
    return Arrays.stream(values()).filter(t -> t.typeName.equals(typeName)).findAny();
  }

  @Override
  public String getTypeName() {
    return typeName;
  }
}
