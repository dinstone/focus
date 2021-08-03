/*
 * Copyright (C) 2019~2021 dinstone<dinstone@163.com>
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

import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class FilterContext {

    private FilterChain chain;
    private Filter filter;

    FilterContext prev;
    FilterContext next;

    public FilterContext(FilterChain chain, Filter filter) {
        this.chain = chain;
        this.filter = filter;
    }

    public Reply invoke(Call call) throws Exception {
        if (filter != null) {
            return filter.invoke(next, call);
        } else {
            return next.invoke(call);
        }
    }

    public FilterChain getFilterChain() {
        return chain;
    }

}
