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
package com.dinstone.focus.rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.dinstone.focus.utils.ByteStreamUtil;

public class Attach implements Map<String, String> {

    protected final Map<String, String> store = new HashMap<String, String>();

    public Attach() {
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return store.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return store.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return store.get(key);
    }

    @Override
    public String put(String key, String value) {
        return store.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return store.remove(key);
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public Set<String> keySet() {
        return store.keySet();
    }

    @Override
    public Collection<String> values() {
        return store.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return store.entrySet();
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        store.putAll(m);
    }

    public static Attach decode(byte[] encoded) throws IOException {
        if (encoded != null && encoded.length > 4) {
            ByteArrayInputStream bai = new ByteArrayInputStream(encoded);
            int count = ByteStreamUtil.readInt(bai);
            Attach attach = new Attach();
            for (int i = 0; i < count; i++) {
                String k = ByteStreamUtil.readString(bai);
                String v = ByteStreamUtil.readString(bai);
                attach.put(k, v);
            }
            return attach;
        }

        return null;
    }

    public static byte[] encode(Attach attach) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        if (attach == null || attach.isEmpty()) {
            // count
            ByteStreamUtil.writeInt(bao, 0);
        } else {
            // count
            ByteStreamUtil.writeInt(bao, attach.size());
            for (Entry<String, String> element : attach.entrySet()) {
                ByteStreamUtil.writeString(bao, element.getKey());
                ByteStreamUtil.writeString(bao, element.getValue());
            }
        }
        return bao.toByteArray();
    }

}
