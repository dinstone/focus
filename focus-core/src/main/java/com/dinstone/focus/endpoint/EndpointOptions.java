/*
 * Copyright (C) 2019~2020 dinstone<dinstone@163.com>
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
package com.dinstone.focus.endpoint;

import com.dinstone.clutch.RegistryConfig;
import com.dinstone.focus.filter.FilterInitializer;

@SuppressWarnings("unchecked")
public class EndpointOptions<T extends EndpointOptions<T>> {

    private int defaultTimeout = 3000;

    private String endpointCode;

    private String endpointName;

    private RegistryConfig registryConfig;

    private FilterInitializer filterInitializer;

    public EndpointOptions() {
        super();
    }

    public String getEndpointCode() {
        return endpointCode;
    }

    public T setEndpointCode(String endpointCode) {
        this.endpointCode = endpointCode;
        return (T) this;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public T setEndpointName(String endpointName) {
        this.endpointName = endpointName;
        return (T) this;
    }

    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public T setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
        return (T) this;
    }

    public RegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public T setRegistryConfig(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
        return (T) this;
    }

    public FilterInitializer getFilterInitializer() {
        return filterInitializer;
    }

    public T setFilterInitializer(FilterInitializer filterInitializer) {
        this.filterInitializer = filterInitializer;
        return (T) this;
    }

}
