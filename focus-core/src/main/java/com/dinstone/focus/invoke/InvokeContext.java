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

package com.dinstone.focus.invoke;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public class InvokeContext {

    private static final ThreadLocal<InvokeContext> CONTEXT_LOCAL = new ThreadLocal<InvokeContext>();

    private static final ThreadLocal<Deque<InvokeContext>> DEQUE_LOCAL = new ThreadLocal<Deque<InvokeContext>>();

    private InetSocketAddress serviceAddress;

    private ConcurrentHashMap<String, Object> context;

    public InetSocketAddress getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(InetSocketAddress serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    /**
     * put if absent
     *
     * @param key
     * @param value
     */
    public void putIfAbsent(String key, Object value) {
        this.context.putIfAbsent(key, value);
    }

    /**
     * put
     *
     * @param key
     * @param value
     */
    public void put(String key, Object value) {
        this.context.put(key, value);
    }

    /**
     * get
     *
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) this.context.get(key);
    }

    /**
     * get and use default if not found
     * 
     * @param key
     * @param defaultIfNotFound
     * @param                   <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultIfNotFound) {
        return this.context.get(key) != null ? (T) this.context.get(key) : defaultIfNotFound;
    }

    /**
     * clear all mappings.
     */
    public void clear() {
        this.context.clear();
    }

    public static InvokeContext getContext() {
        InvokeContext context = CONTEXT_LOCAL.get();
        if (context == null) {
            context = new InvokeContext();
            CONTEXT_LOCAL.set(context);
        }
        return context;
    }

    public static void setContext(InvokeContext context) {
        CONTEXT_LOCAL.set(context);
    }

    public static InvokeContext peekContext() {
        return CONTEXT_LOCAL.get();
    }

    public static void removeContext() {
        CONTEXT_LOCAL.remove();
    }

    public static void pushContext() {
        InvokeContext context = CONTEXT_LOCAL.get();
        if (context != null) {
            Deque<InvokeContext> deque = DEQUE_LOCAL.get();
            if (deque == null) {
                deque = new ArrayDeque<InvokeContext>();
                DEQUE_LOCAL.set(deque);
            }
            deque.push(context);
            CONTEXT_LOCAL.set(null);
        }
    }

    public static void popContext() {
        Deque<InvokeContext> deque = DEQUE_LOCAL.get();
        if (deque != null) {
            InvokeContext context = deque.peek();
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
