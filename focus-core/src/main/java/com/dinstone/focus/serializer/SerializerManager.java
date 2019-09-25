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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dinstone.focus.serializer.json.CallSerializer;
import com.dinstone.focus.serializer.json.ExceptionSerializer;
import com.dinstone.focus.serializer.json.ReplySerializer;

public class SerializerManager {

    private static SerializerManager INSTANCE = new SerializerManager();

    private Map<String, Serializer> serializerMap = new ConcurrentHashMap<>();

    public static SerializerManager getInstance() {
        return INSTANCE;
    }

    protected SerializerManager() {
        regist(new CallSerializer());
        regist(new ReplySerializer());
        regist(new ExceptionSerializer());
    }

    public void regist(Serializer serializer) {
        serializerMap.put(serializer.name(), serializer);
    }

    public Serializer find(String name) {
        return serializerMap.get(name);
    }
}
