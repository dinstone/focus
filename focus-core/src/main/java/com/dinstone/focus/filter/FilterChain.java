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
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class FilterChain implements InvokeHandler {

    private FilterContext headContext;

    private FilterContext tailContext;

    public FilterChain(InvokeHandler invokeHandler) {
        if (invokeHandler == null) {
            throw new IllegalArgumentException("invokeHandler is null");
        }
        this.tailContext = new FilterContext(this, new Filter() {

            @Override
            public Reply invoke(FilterContext next, Call call) throws Exception {
                return invokeHandler.invoke(call);
            }
        });
        this.headContext = new FilterContext(this, null);

        this.headContext.setNextContext(tailContext);
        this.tailContext.setPrevContext(headContext);
    }

    public FilterChain addFilter(Filter... filters) {
        addFilter(Arrays.asList(filters));
        return this;
    }

    public FilterChain addFilter(List<Filter> filters) {
        if (filters != null && !filters.isEmpty()) {
            for (Filter filter : filters) {
                FilterContext now = new FilterContext(this, filter);

                FilterContext last = tailContext.getPrevContext();
                last.setNextContext(now);

                now.setPrevContext(last);
                now.setNextContext(tailContext);

                tailContext.setPrevContext(now);
            }
        }
        return this;
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        return headContext.invoke(call);
    }

}
