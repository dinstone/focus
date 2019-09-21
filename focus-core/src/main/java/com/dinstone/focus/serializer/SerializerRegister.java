/*
 * Copyright (C) 2013~2017 dinstone<dinstone@163.com>
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
package com.dinstone.focus.serializer;

import java.util.HashMap;
import java.util.Map;

public class SerializerRegister {

    private static SerializerRegister INSTANCE = new SerializerRegister();

    private Map<String, Serializer> serializerMap = new HashMap<>();

    public static SerializerRegister getInstance() {
        return INSTANCE;
    }

    protected SerializerRegister() {
        regist(new JacksonSerializer());
    }

    public void regist(Serializer serializer) {
        serializerMap.put(serializer.name(), serializer);
    }

    public Serializer find(String name) {
        return serializerMap.get(name);
    }
}
