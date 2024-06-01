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

package io.github.davemeier82.homeautomation.influxdb2;


import io.github.davemeier82.homeautomation.core.device.property.AlarmState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InfluxDb2DeviceStateRepositoryTest {

  @Test
  void cast() {
    assertThat(InfluxDb2DeviceStateRepository.cast(1L, Long.class)).isEqualTo(Long.valueOf(1L));
    assertThat(InfluxDb2DeviceStateRepository.cast(1, Long.class)).isEqualTo(Long.valueOf(1L));
    assertThat(InfluxDb2DeviceStateRepository.cast(1L, Double.class)).isEqualTo(Double.valueOf(1L));
    assertThat(InfluxDb2DeviceStateRepository.cast(1L, Float.class)).isEqualTo(Float.valueOf(1L));
    assertThat(InfluxDb2DeviceStateRepository.cast(1.26, String.class)).isEqualTo("1.26");
    assertThat(InfluxDb2DeviceStateRepository.cast("sdfsdf", String.class)).isEqualTo("sdfsdf");
    assertThat(InfluxDb2DeviceStateRepository.cast("FIRE", AlarmState.class)).isEqualTo(AlarmState.FIRE);
    assertThat(InfluxDb2DeviceStateRepository.cast("true", Boolean.class)).isEqualTo(Boolean.TRUE);
    assertThat(InfluxDb2DeviceStateRepository.cast("TRUE", Boolean.class)).isEqualTo(Boolean.TRUE);
    assertThat(InfluxDb2DeviceStateRepository.cast(0.1, Boolean.class)).isEqualTo(Boolean.TRUE);
    assertThat(InfluxDb2DeviceStateRepository.cast(0, Boolean.class)).isEqualTo(Boolean.FALSE);

  }
}