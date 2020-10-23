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

import java.io.UnsupportedEncodingException;

import com.dinstone.focus.RpcException;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Response;

public class ErrorCodec {

    private static final String UTF_8 = "utf-8";

    public void encode(Response response, RpcException exception) {
        Headers headers = response.getHeaders();
        headers.put("error.code", "" + exception.getCode());
        headers.put("error.message", "" + exception.getMessage());

        if (exception.getStack() != null) {
            try {
                response.setContent(exception.getStack().getBytes(UTF_8));
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
        }
    }

    public RpcException decode(Response response) {
        RpcException error = null;
        try {
            Headers headers = response.getHeaders();
            int code = Integer.parseInt(headers.get("error.code"));
            String message = headers.get("error.message");
            error = new RpcException(code, message);
            if (response.getContent() != null) {
                String stack = new String(response.getContent(), UTF_8);
                error = new RpcException(code, message, stack);
            } else {
                error = new RpcException(code, message);
            }
        } catch (Exception e) {
            error = new RpcException(499, "decode exception error", e);
        }
        return error;
    }

}
