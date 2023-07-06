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
package com.dinstone.focus.client;

import java.util.List;

/**
 * service level options
 * 
 * @author dinstone
 *
 */
public class ImportOptions {

    public static final int DEFAULT_TIMEOUT_MILLIS = 3000;

    private static final int DEFAULT_CONNECT_RETRY = 1;

    private static final int DEFAULT_TIMEOUT_RETRY = 0;

    private String application;

    private String service;

    private int connectRetry;

    private int timeoutRetry;

    private int timeoutMillis;

    private String serializerType;

    private String compressorType;

    private int compressThreshold;

    private List<InvokeOptions> invokeOptions;

    public ImportOptions(String service) {
        this(null, service);
    }

    public ImportOptions(String application, String service) {
        this.application = application;
        this.service = service;

        this.timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
        this.timeoutRetry = DEFAULT_TIMEOUT_RETRY;
        this.connectRetry = DEFAULT_CONNECT_RETRY;
    }

    public String getService() {
        return service;
    }

    public String getApplication() {
        return application;
    }

    public int getConnectRetry() {
        return connectRetry;
    }

    public void setConnectRetry(int connectRetry) {
        this.connectRetry = connectRetry;
    }

    public int getTimeoutRetry() {
        return timeoutRetry;
    }

    public void setTimeoutRetry(int timeoutRetry) {
        this.timeoutRetry = timeoutRetry;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public ImportOptions setTimeoutMillis(int timeoutMillis) {
        if (timeoutMillis > 0) {
            this.timeoutMillis = timeoutMillis;
        }
        return this;
    }

    public String getSerializerType() {
        return serializerType;
    }

    public ImportOptions setSerializerType(String serializerType) {
        this.serializerType = serializerType;
        return this;
    }

    public String getCompressorType() {
        return compressorType;
    }

    public ImportOptions setCompressorType(String compressorType) {
        this.compressorType = compressorType;
        return this;
    }

    public List<InvokeOptions> getInvokeOptions() {
        return invokeOptions;
    }

    public ImportOptions setInvokeOptions(List<InvokeOptions> invokeOptions) {
        this.invokeOptions = invokeOptions;
        return this;
    }

    public int getCompressThreshold() {
        return compressThreshold;
    }

    public void setCompressThreshold(int compressThreshold) {
        this.compressThreshold = compressThreshold;
    }

}
