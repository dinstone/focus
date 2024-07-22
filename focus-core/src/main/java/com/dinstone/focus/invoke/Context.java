/*
 * Copyright (C) 2019~2024 dinstone<dinstone@163.com>
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
package com.dinstone.focus.invoke;

import java.util.concurrent.ConcurrentHashMap;

public class Context implements AutoCloseable {

    private static final ThreadLocal<Context> CONTEXT_LOCAL = new ThreadLocal<Context>();

    private final ConcurrentHashMap<String, Object> contentMap = new ConcurrentHashMap<>();

    private final Context parent;

    private Context() {
        this(null);
    }

    private Context(Context parent) {
        this.parent = parent;
    }

    /**
     * put k-v if absent
     *
     * @param key
     *            key
     * @param value
     *            value
     */
    public void putIfAbsent(String key, Object value) {
        this.contentMap.putIfAbsent(key, value);
    }

    /**
     * put k-v
     *
     * @param key
     *            key
     * @param value
     *            value
     */
    public void put(String key, Object value) {
        this.contentMap.put(key, value);
    }

    /**
     * get
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object value = this.contentMap.get(key);
        if (value == null && parent != null) {
            value = parent.get(key);
        }
        return (T) value;
    }

    /**
     * get and use default if not found
     */
    public <T> T get(String key, T defaultIfNotFound) {
        T value = get(key);
        return value == null ? defaultIfNotFound : value;
    }

    /**
     * remove the key
     */
    public void remove(String key) {
        this.contentMap.remove(key);
    }

    @Override
    public void close() {
        Context current = CONTEXT_LOCAL.get();
        if (this == current) {
            if (current.parent != null) {
                CONTEXT_LOCAL.set(current.parent);
            } else {
                CONTEXT_LOCAL.remove();
            }
        }
    }

    public static Context create() {
        Context context;
        Context parent = CONTEXT_LOCAL.get();
        if (parent != null) {
            context = new Context(parent);
        } else {
            context = new Context();
        }
        CONTEXT_LOCAL.set(context);
        return context;
    }

    public static Context current() {
        return CONTEXT_LOCAL.get();
    }

}
