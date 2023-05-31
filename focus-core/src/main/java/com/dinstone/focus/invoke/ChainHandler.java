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

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

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
                invokeHandler = new InterceptorHandler(iterator.previous(), invokeHandler);
            }
        }
        return this;
    }

    @Override
    public CompletableFuture<Reply> handle(Call call) throws Exception {
        return invokeHandler.handle(call);
    }

    public class InterceptorHandler implements Handler {

        private Interceptor interceptor;

        private Handler handler;

        public InterceptorHandler(Interceptor interceptor, Handler handler) {
            super();
            this.interceptor = interceptor;
            this.handler = handler;
        }

        @Override
        public CompletableFuture<Reply> handle(Call call) throws Exception {
            return interceptor.intercept(call, handler);
        }

    }
}
