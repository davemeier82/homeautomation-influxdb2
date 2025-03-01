/*
 * Copyright 2021-2025 the original author or authors.
 *
 * Licensed under the Apache License; Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing; software
 * distributed under the License is distributed on an "AS IS" BASIS;
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.davemeier82.homeautomation.influxdb2;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("homeautomation.influxdb2")
public class InfluxDb2Properties {
  @NestedConfigurationProperty
  private final TaskSchedulerProperties taskScheduler = new TaskSchedulerProperties();
  private String url;
  private char[] token;
  private String organization;
  private String bucket;

  public String getUrl() {
    return url;
  }

  public char[] getToken() {
    return token;
  }

  public String getOrganization() {
    return organization;
  }

  public String getBucket() {
    return bucket;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setToken(char[] token) {
    this.token = token;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public TaskSchedulerProperties getTaskScheduler() {
    return taskScheduler;
  }

  public static class TaskSchedulerProperties {
    private int poolSize = 3;

    public int getPoolSize() {
      return poolSize;
    }

    public void setPoolSize(int poolSize) {
      this.poolSize = poolSize;
    }
  }
}
