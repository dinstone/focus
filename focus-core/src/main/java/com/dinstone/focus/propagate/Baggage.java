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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Baggage {

    public static final String COMMA = ",";
    public static final String EQUAL = "=";

    public static final String CONTEXT_KEY = "context.baggage";

    public static final String PROPAGATE_KEY = "propagate.baggage";

    private final Map<String, String> stores = new HashMap<>();

    void forEach(BiConsumer<? super String, String> consumer) {
        if (consumer != null) {
            stores.forEach(consumer);
        }
    }

    public boolean isEmpty() {
        return stores.isEmpty();
    }

    public final String get(String key) {
        return stores.get(key);
    }

    public final void put(String key, String val) {
        stores.put(key, val);
    }

    public void parse(String c) {
        if (c != null && !c.isEmpty()) {
            String[] ps = c.split(COMMA);
            for (String p : ps) {
                int index = p.indexOf(EQUAL);
                if (index > 0) {
                    String k = p.substring(0, index - 1);
                    String v = p.substring(index + 1);
                    stores.put(k, v);
                }
            }
        }
    }

    public String pack() {
        StringBuilder c = new StringBuilder();
        stores.forEach((k, v) -> {
            c.append(k).append(EQUAL).append(v).append(COMMA);
        });
        if (c.length() > 0) {
            // Trim trailing comma
            c.setLength(c.length() - 1);
        }
        return c.toString();
    }

    @Override
    public String toString() {
        return "Baggage " + stores;
    }
}
