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
package com.dinstone.focus.tracing;

import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.protocol.Reply;

import brave.rpc.RpcClientResponse;

public class FocusClientResponse extends RpcClientResponse {

    private Reply reply;
    private Throwable error;

    public FocusClientResponse(FocusClientRequest request, Reply reply, Throwable error) {
        this.reply = reply;
        this.error = error;
    }

    @Override
    public String errorCode() {
        if (error != null) {
            return getErrorCode();
        }

        if (reply.getData() instanceof Throwable) {
            return "999";
        }

        return null;
    }

    private String getErrorCode() {
        if (error instanceof InvokeException) {
            return "" + ((InvokeException) error).getCode();
        }
        return "999";
    }

    @Override
    public Throwable error() {
        if (error != null) {
            return error;
        }

        if (reply.getData() instanceof Throwable) {
            return (Throwable) reply.getData();
        }
        return null;
    }

    @Override
    public Object unwrap() {
        return reply;
    }
}
