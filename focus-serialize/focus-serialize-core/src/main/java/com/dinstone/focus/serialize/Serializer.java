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
package com.dinstone.focus.serialize;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface Serializer {

    public static final String SERIALIZER_KEY = "protocol.serializer";

    public static final Map<String, Serializer> SERIALIZER_MAP = new ConcurrentHashMap<>();

    public static Serializer lookup(String serializerId) {
        if (serializerId != null) {
            return SERIALIZER_MAP.get(serializerId);
        }
        return null;
    }

    public static void regist(Serializer serializer) {
        SERIALIZER_MAP.put(serializer.serializerId(), serializer);
    }

    /**
     * The Serializer ID
     * 
     * @return
     */
    public String serializerId();

    public byte[] encode(Object content, Class<?> contentType) throws IOException;

    public Object decode(byte[] contentBytes, Class<?> contentType) throws IOException;

}
