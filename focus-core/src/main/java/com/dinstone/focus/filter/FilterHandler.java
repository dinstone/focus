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

import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;

public class FilterHandler implements InvokeHandler {

    private final Filter nextFilter;

    private final InvokeHandler invokeHandler;

    public FilterHandler(Filter nextFilter, InvokeHandler invokeHandler) {
        super();
        this.nextFilter = nextFilter;
        this.invokeHandler = invokeHandler;

        if (nextFilter == null && invokeHandler == null) {
            throw new IllegalArgumentException("nextFilter and invokeHandler is null");
        }
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        if (nextFilter != null) {
            return nextFilter.invoke(invokeHandler, call);
        } else {
            return invokeHandler.invoke(call);
        }
    }
}
