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
package com.dinstone.focus.protocol;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Attach implements Iterable<Entry<String, String>> {

    private final Map<String, String> store = new HashMap<String, String>();

    public Attach() {
    }

    public Attach(Iterable<Entry<String, String>> as) {
        if (as != null) {
            as.forEach(e -> {
                store.put(e.getKey(), e.getValue());
            });
        }
    }

    public int size() {
        return store.size();
    }

    public boolean isEmpty() {
        return store.isEmpty();
    }

    public boolean containsKey(Object key) {
        return store.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return store.containsValue(value);
    }

    public String get(Object key) {
        return store.get(key);
    }

    public String put(String key, String value) {
        return store.put(key, value);
    }

    public String remove(Object key) {
        return store.remove(key);
    }

    public void clear() {
        store.clear();
    }

    public Set<String> keySet() {
        return store.keySet();
    }

    public Collection<String> values() {
        return store.values();
    }

    public Set<Entry<String, String>> entrySet() {
        return store.entrySet();
    }

    public Iterator<Entry<String, String>> iterator() {
        return store.entrySet().iterator();
    }

    public void putAll(Iterable<Entry<String, String>> as) {
        if (as != null) {
            as.forEach(e -> {
                this.put(e.getKey(), e.getValue());
            });
        }
    }

    @Override
    public String toString() {
        return "Attach [" + store + "]";
    }

}
