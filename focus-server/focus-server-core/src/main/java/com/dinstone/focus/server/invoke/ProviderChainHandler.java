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
package com.dinstone.focus.server.invoke;

import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.ChainHandler;
import com.dinstone.focus.invoke.Context;
import com.dinstone.focus.invoke.DefaultInvocation;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.propagate.Baggage;
import com.dinstone.focus.propagate.Propagator;

/**
 * server-side service invoker.
 *
 * @author dinstone
 * 
 * @version 1.0.0
 */
public class ProviderChainHandler extends ChainHandler {

    private final Propagator propagator;

    public ProviderChainHandler(ServiceConfig serviceConfig, Handler invokeHandler) {
        super(serviceConfig, invokeHandler);
        this.propagator = new Propagator();
    }

    public CompletableFuture<Object> handle(Invocation invocation) {
        try (Context context = Context.create()) {
            ((DefaultInvocation) invocation).context(context);

            // extract propagate baggage from invocation
            if (invocation.attributes().containsKey(Baggage.PROPAGATE_KEY)) {
                Baggage baggage = new Baggage();
                propagator.extract(invocation, baggage);
                context.put(Baggage.CONTEXT_KEY, baggage);
            }

            return invokeHandler.handle(invocation);
        }
    }

}
