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
package com.dinstone.focus.codec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.rpc.Reply;
import com.dinstone.photon.message.Response;

public class CodecManager {

    private static final Map<String, RpcCodec> NAME_CODEC_MAP = new ConcurrentHashMap<>();

    private static final Map<Integer, RpcCodec> CODE_CODEC_MAP = new ConcurrentHashMap<>();

    private static final ErrorCodec ERROR_CODEC = new ErrorCodec();

    public static void regist(String name, RpcCodec codec) {
        NAME_CODEC_MAP.put(name, codec);
        CODE_CODEC_MAP.put((int) codec.code(), codec);
    }

    public static RpcCodec codec(String name) {
        return NAME_CODEC_MAP.get(name);
    }

    public static RpcCodec codec(int code) {
        return CODE_CODEC_MAP.get(code);
    }

    public static ErrorCodec error() {
        return ERROR_CODEC;
    }

    public static Reply decode(Response response) {
        if (response.getCodec() == 0) {
            return ERROR_CODEC.decode(response);
        }
        return codec(response.getCodec()).decode(response);
    }

    public static void encode(Response response, Reply reply) {
        if (reply.getData() instanceof FocusException) {
            response.setCodec((byte) 0);
            ERROR_CODEC.encode(response, reply);
        } else {
            codec(response.getCodec()).encode(response, reply);
        }
    }

}
