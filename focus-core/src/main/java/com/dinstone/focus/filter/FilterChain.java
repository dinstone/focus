/*
 * Copyright (C) 2019~2020 dinstone<dinstone@163.com>
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

import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;

public class FilterChain implements InvokeHandler {

    private FilterHandler filterHandler;

    public FilterChain(InvokeHandler invokeHandler, Filter... filters) {
        this(invokeHandler, Arrays.asList(filters));
    }

    public FilterChain(InvokeHandler invokeHandler, List<Filter> filters) {
        if (invokeHandler == null) {
            throw new IllegalArgumentException("invokeHandler is null");
        }
        filterHandler = new FilterHandler(null, invokeHandler);

        if (filters != null && !filters.isEmpty()) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                filterHandler = new FilterHandler(filters.get(i), filterHandler);
            }
        }
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        return filterHandler.invoke(call);
    }

}
