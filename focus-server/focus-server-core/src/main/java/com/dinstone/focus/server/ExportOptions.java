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
package com.dinstone.focus.server;

/**
 * service level options
 * 
 * @author dinstone
 *
 */
public class ExportOptions {

    private final String service;

    private String serializerType;

    private String compressorType;

    private int compressThreshold;

    public ExportOptions(String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }

    public String getSerializerType() {
        return serializerType;
    }

    public ExportOptions setSerializerType(String serializerType) {
        this.serializerType = serializerType;
        return this;
    }

    public String getCompressorType() {
        return compressorType;
    }

    public ExportOptions setCompressorType(String compressorType) {
        this.compressorType = compressorType;
        return this;
    }

    public int getCompressThreshold() {
        return compressThreshold;
    }

    public ExportOptions setCompressThreshold(int compressThreshold) {
        this.compressThreshold = compressThreshold;
        return this;
    }

}
