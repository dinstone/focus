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
package com.dinstone.focus.propagate;

import com.dinstone.focus.invoke.Invocation;

public class Propagator {

    /**
     * inject propagate baggage to invocation
     */
    public void inject(Invocation invocation, Baggage baggage) {
        if (baggage == null) {
            return;
        }

        String c = baggage.pack();
        if (!c.isEmpty()) {
            invocation.attributes().put(Baggage.PropagateKey, c);
        }
    }

    /**
     * extract propagate baggage from invocation
     */
    public void extract(Invocation invocation, Baggage baggage) {
        if (baggage == null) {
            return;
        }

        baggage.parse(invocation.attributes().get(Baggage.PropagateKey));
    }
}
