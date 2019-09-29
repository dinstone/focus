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

package com.dinstone.focus.invoker;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;

public class InvocationContext {

    private static final ThreadLocal<InvocationContext> LOCAL = new ThreadLocal<InvocationContext>();

    private static final ThreadLocal<Deque<InvocationContext>> DEQUE_LOCAL = new ThreadLocal<Deque<InvocationContext>>();

    private InetSocketAddress serviceAddress;

    public InetSocketAddress getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(InetSocketAddress serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public static InvocationContext get() {
        InvocationContext context = LOCAL.get();
        if (context == null) {
            context = new InvocationContext();
            LOCAL.set(context);
        }
        return context;
    }

    public static void set(InvocationContext context) {
        LOCAL.set(context);
    }

    public static InvocationContext peek() {
        return LOCAL.get();
    }

    public static void remove() {
        LOCAL.remove();
    }

    public static void push() {
        InvocationContext context = LOCAL.get();
        if (context != null) {
            Deque<InvocationContext> deque = DEQUE_LOCAL.get();
            if (deque == null) {
                deque = new ArrayDeque<InvocationContext>();
                DEQUE_LOCAL.set(deque);
            }
            deque.push(context);
            LOCAL.set(null);
        }
    }

    public static void pop() {
        Deque<InvocationContext> deque = DEQUE_LOCAL.get();
        if (deque != null) {
            InvocationContext context = deque.peek();
            if (context != null) {
                LOCAL.set(deque.pop());
            }
        }
    }

    public static void clear() {
        LOCAL.remove();
        DEQUE_LOCAL.remove();
    }
}
