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
package com.dinstone.focus.invoke;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public class Context {

    public static final String SERVICE_INSTANCE_KEY = "service.instance";

    private static final ThreadLocal<Deque<Context>> DEQUE_LOCAL = new ThreadLocal<Deque<Context>>();

    private static final ThreadLocal<Context> CONTEXT_LOCAL = new ThreadLocal<Context>();

    private final ConcurrentHashMap<String, Object> contentMap = new ConcurrentHashMap<>();

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
     *
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) this.contentMap.get(key);
    }

    /**
     * get and use default if not found
     *
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultIfNotFound) {
        return this.contentMap.get(key) != null ? (T) this.contentMap.get(key) : defaultIfNotFound;
    }

    /**
     * clear all mappings.
     */
    public void clear() {
        this.contentMap.clear();
    }

    public static Context getContext() {
        Context context = CONTEXT_LOCAL.get();
        if (context == null) {
            context = new Context();
            CONTEXT_LOCAL.set(context);
        }
        return context;
    }

    public static Context peekContext() {
        return CONTEXT_LOCAL.get();
    }

    public static void removeContext() {
        CONTEXT_LOCAL.remove();
    }

    public static void pushContext() {
        Context context = CONTEXT_LOCAL.get();
        if (context != null) {
            Deque<Context> deque = DEQUE_LOCAL.get();
            if (deque == null) {
                deque = new ArrayDeque<Context>();
                DEQUE_LOCAL.set(deque);
            }
            deque.push(context);
            CONTEXT_LOCAL.set(null);
        }
    }

    public static void popContext() {
        Deque<Context> deque = DEQUE_LOCAL.get();
        if (deque != null) {
            Context context = deque.peek();
            if (context != null) {
                CONTEXT_LOCAL.set(deque.pop());
            }
        }
    }

    public static void clearContext() {
        CONTEXT_LOCAL.remove();
        DEQUE_LOCAL.remove();
    }
}
