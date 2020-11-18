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
package com.dinstone.focus.tracing;

import com.dinstone.focus.protocol.Reply;
import com.dinstone.photon.ExchangeException;

import brave.rpc.RpcServerResponse;

public class FocusServerResponse extends RpcServerResponse {

    private FocusServerRequest request;
    private Reply reply;
    private Throwable error;

    public FocusServerResponse(FocusServerRequest request, Reply result, Throwable error) {
        this.request = request;
        this.reply = result;
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
        return "";
    }

    private String getErrorCode() {
        if (error instanceof ExchangeException) {
            return "" + ((ExchangeException) error).getCode();
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
