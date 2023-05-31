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
package com.dinstone.focus.filter;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.filter.Filter;
import com.dinstone.focus.filter.FilterContext;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class FilterChainHandler implements Handler {

    protected ServiceConfig serviceConfig;

    protected FilterContext filterChain;

    public FilterChainHandler(ServiceConfig serviceConfig, Handler invokeHandler) {
        if (invokeHandler == null) {
            throw new IllegalArgumentException("invokeHandler is null");
        }

        this.serviceConfig = serviceConfig;
        this.filterChain = new FilterContext(serviceConfig, new Filter() {

            @Override
            public CompletableFuture<Reply> invoke(FilterContext next, Call call) throws Exception {
                return invokeHandler.handle(call);
            }
        }, null);
    }

    public FilterChainHandler addFilter(Filter... filters) {
        return addFilter(Arrays.asList(filters));
    }

    public FilterChainHandler addFilter(List<Filter> filters) {
        if (filters != null) {
            for (ListIterator<Filter> iterator = filters.listIterator(filters.size()); iterator.hasPrevious();) {
                filterChain = new FilterContext(serviceConfig, iterator.previous(), filterChain);
            }
        }
        return this;
    }

    @Override
    public CompletableFuture<Reply> handle(Call call) throws Exception {
        return filterChain.invoke(call);
    }
}
