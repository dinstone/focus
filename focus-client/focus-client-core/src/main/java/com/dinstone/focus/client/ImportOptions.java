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

    public static final int DEFAULT_INVOKE_TIMEOUT = 3000;

    public static final int DEFAULT_INVOKE_RETRY = 0;

    private String application;

    private String service;

    private int timeout;

    private int retry;

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

        this.retry = DEFAULT_INVOKE_RETRY;
        this.timeout = DEFAULT_INVOKE_TIMEOUT;
    }

    public String getService() {
        return service;
    }

    public String getApplication() {
        return application;
    }

    public int getTimeout() {
        return timeout;
    }

    public ImportOptions setTimeout(int timeout) {
        if (timeout > 0) {
            this.timeout = timeout;
        }
        return this;
    }

    public int getRetry() {
        return retry;
    }

    public ImportOptions setRetry(int retry) {
        this.retry = retry;
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
