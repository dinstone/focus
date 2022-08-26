/*
 * Copyright (C) 2019~2022 dinstone<dinstone@163.com>
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
package com.dinstone.focus.compress;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface Compressor {

    public static final String COMPRESSOR_KEY = "protocol.compressor";

    public static final Map<String, Compressor> COMPRESSOR_MAP = new ConcurrentHashMap<>();

    public static Compressor lookup(String compressId) {
        if (compressId != null) {
            return COMPRESSOR_MAP.get(compressId);
        }
        return null;
    }

    public static void regist(Compressor compress) {
        COMPRESSOR_MAP.put(compress.compressorId(), compress);
    }

    /**
     * The compressor ID
     * 
     * @return
     */
    public String compressorId();

    /**
     * The data enable compress if grate than threshold
     * 
     * @param data
     * 
     * @return
     */
    public boolean enable(byte[] data);

    /**
     * The Data compress.
     * 
     * @param data
     * 
     * @return
     * 
     * @throws IOException
     */
    byte[] encode(byte[] data) throws IOException;

    /**
     * The Data uncompress.
     * 
     * @param data
     * 
     * @return
     * 
     * @throws IOException
     */
    byte[] decode(byte[] data) throws IOException;

}
