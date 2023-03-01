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

import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.serialize.Serializer;

public class ConsumerConfig extends ServiceConfig {

    public ConsumerConfig(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setGroup(String group) {
        if (group != null && group.length() > 0) {
            this.group = group;
        }
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public void setHandler(InvokeHandler handler) {
        this.handler = handler;
    }

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    public void setCompressor(Compressor compressor) {
        this.compressor = compressor;
    }

    public void setCompressThreshold(int compressThreshold) {
        this.compressThreshold = compressThreshold;
    }

    public void parseMethod(Method... methods) {
        for (Method method : methods) {
            MethodConfig methodConfig = createMethodConfig(method);
            if (methodConfig != null) {
                methodConfig.setInvokeTimeout(timeout);
                methodConfig.setInvokeRetry(retry);
                addMethodConfig(methodConfig);
            }
        }
    }

}
