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

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.config.ServiceConfig;

public abstract class ChainHandler implements Handler {

    protected ServiceConfig serviceConfig;

    protected Handler invokeHandler;

    public ChainHandler(ServiceConfig serviceConfig, Handler invokeHandler) {
        if (invokeHandler == null) {
            throw new IllegalArgumentException("invokeHandler is null");
        }
        this.serviceConfig = serviceConfig;
        this.invokeHandler = invokeHandler;
    }

    public ChainHandler addInterceptor(Interceptor... interceptors) {
        return addInterceptor(Arrays.asList(interceptors));
    }

    public ChainHandler addInterceptor(List<Interceptor> interceptors) {
        if (interceptors != null) {
            for (ListIterator<Interceptor> iterator = interceptors.listIterator(interceptors.size()); iterator
                    .hasPrevious();) {
                invokeHandler = new HandlerAdapter(iterator.previous(), invokeHandler);
            }
        }
        return this;
    }

    @Override
    public CompletableFuture<Object> handle(Invocation invocation) {
        return invokeHandler.handle(invocation);
    }

    static class HandlerAdapter implements Handler {

        private final Interceptor interceptor;

        private final Handler nextHandler;

        public HandlerAdapter(Interceptor interceptor, Handler nextHandler) {
            super();
            this.interceptor = interceptor;
            this.nextHandler = nextHandler;
        }

        @Override
        public CompletableFuture<Object> handle(Invocation invocation) {
            return interceptor.intercept(invocation, nextHandler);
        }

    }
}
