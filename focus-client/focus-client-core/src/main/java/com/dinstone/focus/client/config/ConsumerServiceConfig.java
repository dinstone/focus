/*
 * Copyright (C) 2019~2023 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.focus.client.config;

import java.lang.reflect.Method;

import com.dinstone.focus.config.AbstractServiceConfig;

public class ConsumerServiceConfig extends AbstractServiceConfig {

    protected int timeoutMillis;

    protected int timeoutRetry;

    protected int connectRetry;

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public int getTimeoutRetry() {
        return timeoutRetry;
    }

    public void setTimeoutRetry(int timeoutRetry) {
        this.timeoutRetry = timeoutRetry;
    }

    public int getConnectRetry() {
        return connectRetry;
    }

    public void setConnectRetry(int connectRetry) {
        this.connectRetry = connectRetry;
    }

    public void parseMethod(Method... methods) {
        for (Method method : methods) {
            ConsumerMethodConfig methodConfig = parse(method, ConsumerMethodConfig::new);
            if (methodConfig != null) {
                methodConfig.setTimeoutMillis(timeoutMillis);
                methodConfig.setTimeoutRetry(timeoutRetry);
                addMethodConfig(methodConfig);
            }
        }
    }

    @Override
    public String toString() {
        return "ConsumerServiceConfig [service=" + service + ", provider=" + provider + ", consumer=" + consumer
                + ", serializer=" + serializer + ", compressor=" + compressor + ", compressThreshold="
                + compressThreshold + ", methodConfigs=" + methodConfigs + ", timeoutMillis=" + timeoutMillis
                + ", timeoutRetry=" + timeoutRetry + ", connectRetry=" + connectRetry + "]";
    }

}
