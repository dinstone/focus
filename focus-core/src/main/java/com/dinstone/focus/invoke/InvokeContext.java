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

public class InvokeContext {

    private static final ThreadLocal<InvokeContext> CONTEXT_LOCAL = new ThreadLocal<InvokeContext>();

    private static final ThreadLocal<Deque<InvokeContext>> DEQUE_LOCAL = new ThreadLocal<Deque<InvokeContext>>();

    private InetSocketAddress serviceAddress;

    public InetSocketAddress getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(InetSocketAddress serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public static InvokeContext get() {
        InvokeContext context = CONTEXT_LOCAL.get();
        if (context == null) {
            context = new InvokeContext();
            CONTEXT_LOCAL.set(context);
        }
        return context;
    }

    public static void set(InvokeContext context) {
        CONTEXT_LOCAL.set(context);
    }

    public static InvokeContext peek() {
        return CONTEXT_LOCAL.get();
    }

    public static void remove() {
        CONTEXT_LOCAL.remove();
    }

    public static void push() {
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

    public static void pop() {
        Deque<InvokeContext> deque = DEQUE_LOCAL.get();
        if (deque != null) {
            InvokeContext context = deque.peek();
            if (context != null) {
                CONTEXT_LOCAL.set(deque.pop());
            }
        }
    }

    public static void clear() {
        CONTEXT_LOCAL.remove();
        DEQUE_LOCAL.remove();
    }
}
