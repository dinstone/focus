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

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SerializerFactory {

    private static final Map<String, Serializer> SERIALIZER_MAP = new ConcurrentHashMap<>();

    static {
        // init serializer
        ServiceLoader<SerializerFactory> sfLoader = ServiceLoader.load(SerializerFactory.class);
        for (SerializerFactory serializerFactory : sfLoader) {
            SerializerFactory.regist(serializerFactory.create());
        }
    }

    public static Serializer lookup(String serializerId) {
        if (serializerId != null) {
            return SERIALIZER_MAP.get(serializerId);
        }
        return null;
    }

    public static void regist(Serializer serializer) {
        SERIALIZER_MAP.put(serializer.serializerId(), serializer);
    }

    public abstract Serializer create();

}
