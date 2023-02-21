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
package com.dinstone.focus.compress;

import java.io.IOException;

public interface Compressor {

    public static final String TYPE_KEY = "compressor.type";

    /**
     * The compressor type
     *
     * @return
     */
    public abstract String compressorType();

    /**
     * The Data compress.
     *
     * @param data
     *
     * @return
     *
     * @throws IOException
     */
    public abstract byte[] encode(byte[] data) throws IOException;

    /**
     * The Data uncompress.
     *
     * @param data
     *
     * @return
     *
     * @throws IOException
     */
    public abstract byte[] decode(byte[] data) throws IOException;

}
