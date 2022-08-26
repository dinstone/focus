/*
 * Copyright (C) 2019~2022 dinstone<dinstone@163.com>
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

import java.util.ArrayList;
import java.util.List;

import com.dinstone.clutch.RegistryConfig;
import com.dinstone.focus.filter.Filter;

@SuppressWarnings("unchecked")
public class EndpointOptions<T extends EndpointOptions<T>> {

    private String endpoint;

    private int defaultTimeout = 3000;

    private int compressThreshold = 10240;

    private RegistryConfig registryConfig;

    private List<Filter> filters = new ArrayList<Filter>();

    public EndpointOptions() {
        super();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public T setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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

    public List<Filter> getFilters() {
        return filters;
    }

    public T addFilter(Filter filter) {
        filters.add(filter);
        return (T) this;
    }

    public int getCompressThreshold() {
        return compressThreshold;
    }

    public void setCompressThreshold(int compressThreshold) {
        this.compressThreshold = compressThreshold;
    }

}
