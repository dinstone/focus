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
package com.dinstone.focus.compress;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CompressorFactory {

    private static final Map<String, Compressor> COMPRESSOR_MAP = new ConcurrentHashMap<>();

    static {
        ServiceLoader<CompressorFactory> cfLoader = ServiceLoader.load(CompressorFactory.class);
        for (CompressorFactory compressorFactory : cfLoader) {
            CompressorFactory.register(compressorFactory.create());
        }
    }

    public static Compressor lookup(String compressType) {
        if (compressType != null) {
            return COMPRESSOR_MAP.get(compressType);
        }
        return null;
    }

    public static void register(Compressor compress) {
        COMPRESSOR_MAP.put(compress.type(), compress);
    }

    public abstract Compressor create();

}
