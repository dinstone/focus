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
package com.dinstone.focus.protocol;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.photon.message.Response;

public class AsyncReply extends Reply {

    private static final long serialVersionUID = 1L;

    private ReplyFuture future = null;

    public AsyncReply(Future<Response> responseFuture, ProtocolCodec protocolCodec, Class<?> returnType) {
        future = new ReplyFuture(responseFuture, protocolCodec, returnType);
    }

    @Override
    public Object getData() {
        return future;
    }

    public class ReplyFuture implements Future<Object> {

        private Future<Response> responseFuture;
        private ProtocolCodec protocolCodec;
        private Class<?> returnType;
        private Reply reply;

        public ReplyFuture(Future<Response> responseFuture, ProtocolCodec protocolCodec, Class<?> returnType) {
            this.responseFuture = responseFuture;
            this.protocolCodec = protocolCodec;
            this.returnType = returnType;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            try {
                return get(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
            } catch (TimeoutException e) {
                throw new ExecutionException(e);
            }
        }

        @Override
        public Object get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            if (reply == null) {
                Response response = responseFuture.get();
                reply = protocolCodec.decode(response, returnType);
            }

            if (reply.getData() instanceof RuntimeException) {
                throw (RuntimeException) reply.getData();
            } else if (reply.getData() instanceof Throwable) {
                throw new ExecutionException((Throwable) reply.getData());
            }
            return reply.getData();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return responseFuture.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return responseFuture.isCancelled();
        }

        @Override
        public boolean isDone() {
            return responseFuture.isDone();
        }

    }

}
