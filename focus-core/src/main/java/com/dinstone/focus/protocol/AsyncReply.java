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

import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.exception.InvokeException;

public class AsyncReply extends Reply {

    private static final long serialVersionUID = 1L;

    private CompletableFuture<Reply> replyFuture;

    public AsyncReply(CompletableFuture<Reply> replyFuture) {
        this.replyFuture = replyFuture;
    }

    @Override
    public Object getData() {
        return replyFuture.thenApply(r -> {
            if (r.getData() instanceof InvokeException) {
                throw (InvokeException) r.getData();
            }
            return r.getData();
        });
    }

    public void addListener(ReplyListener listener) {
        this.replyFuture.whenComplete((r, e) -> {
            listener.complete(r, e);
        });
    }

}
