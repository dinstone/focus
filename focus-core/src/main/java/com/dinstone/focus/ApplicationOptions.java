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
package com.dinstone.focus;

import java.util.ArrayList;
import java.util.List;

import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.Interceptor;

@SuppressWarnings("unchecked")
public class ApplicationOptions<T extends ApplicationOptions<T>> {

    public static final String DEFAULT_NAMESPACE = "default";

    public static final String DEFAULT_SERIALIZER_Type = "json";

    private static final int DEFAULT_COMPRESS_THRESHOLD = 10240;

    /**
     * application identity for micro-service name
     */
    private String identity;

    /**
     * application identity for runtime environment
     */
    private String namespace;

    private String serializerType;

    private String compressorType;

    private int compressThreshold;

    private ClutchOptions clutchOptions;

    private List<Interceptor> interceptors = new ArrayList<Interceptor>();

    public ApplicationOptions(String identity) {
        if (identity == null) {
            throw new FocusException("application.identity must be not empty");
        }
        this.identity = identity;

        namespace = DEFAULT_NAMESPACE;
        serializerType = DEFAULT_SERIALIZER_Type;
        compressThreshold = DEFAULT_COMPRESS_THRESHOLD;
    }

    public String getIdentity() {
        return identity;
    }

    public String getNamespace() {
        return namespace;
    }

    public T setNamespace(String namespace) {
        this.namespace = namespace;
        return (T) this;
    }

    public ClutchOptions getClutchOptions() {
        return clutchOptions;
    }

    public T setClutchOptions(ClutchOptions clutchOptions) {
        this.clutchOptions = clutchOptions;
        return (T) this;
    }

    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    public T addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
        return (T) this;
    }

    public int getCompressThreshold() {
        return compressThreshold;
    }

    public T setCompressThreshold(int compressThreshold) {
        this.compressThreshold = compressThreshold;
        return (T) this;
    }

    public String getSerializerType() {
        return serializerType;
    }

    public T setSerializerType(String serializerType) {
        this.serializerType = serializerType;
        return (T) this;
    }

    public String getCompressorType() {
        return compressorType;
    }

    public T setCompressorType(String compressorType) {
        this.compressorType = compressorType;
        return (T) this;
    }

}
