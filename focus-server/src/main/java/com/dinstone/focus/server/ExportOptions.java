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
package com.dinstone.focus.server;

public class ExportOptions {

    private static final String DEFAULT_SERIALIZER_ID = "jackson";

    private static final String DEFAULT_SERVICE_GROUP = "";

    private static final int DEFAULT_INVOKE_TIMEOUT = 3000;

    private String service;

    private String group;

    private int timeout;

    private String serializerId;

    private String compressorId;

    private int compressThreshold;

    public ExportOptions(String service) {
        this(service, null);
    }

    public ExportOptions(String service, String group) {
        this.service = service;
        if (group != null && group.length() > 0) {
            this.group = group;
        } else {
            this.group = DEFAULT_SERVICE_GROUP;
        }

        this.timeout = DEFAULT_INVOKE_TIMEOUT;
        this.serializerId = DEFAULT_SERIALIZER_ID;
    }

    public String getService() {
        return service;
    }

    public String getGroup() {
        return group;
    }

    public int getTimeout() {
        return timeout;
    }

    public ExportOptions setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public String getSerializerId() {
        return serializerId;
    }

    public ExportOptions setSerializerId(String serializerId) {
        this.serializerId = serializerId;
        return this;
    }

    public String getCompressorId() {
        return compressorId;
    }

    public ExportOptions setCompressorId(String compressorId) {
        this.compressorId = compressorId;
        return this;
    }

    public int getCompressThreshold() {
        return compressThreshold;
    }

    public void setCompressThreshold(int compressThreshold) {
        this.compressThreshold = compressThreshold;
    }

}
