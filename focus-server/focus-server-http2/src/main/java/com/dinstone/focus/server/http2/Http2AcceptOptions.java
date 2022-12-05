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
package com.dinstone.focus.server.http2;

import com.dinstone.focus.server.ExecutorSelector;
import com.dinstone.focus.server.transport.AcceptOptions;

public class Http2AcceptOptions implements AcceptOptions {

    /**
     * The default accept event loop size = 1
     */
    public static final int DEFAULT_ACCEPT_SIZE = 1;

    /**
     * The default worker event loop size = 0 (2 * Runtime.availableProcessors).
     */
    public static final int DEFAULT_WORKER_SIZE = 2;

    /**
     * The default accept backlog = 1024
     */
    public static final int DEFAULT_ACCEPT_BACKLOG = -1;

    /**
     * The default idle timeout 30s
     */
    private static final int DEFAULT_IDLE_TIMEOUT = 30;

    private int acceptSize;
    private int workerSize;
    private int acceptBacklog;

    private boolean enableSsl;
    private int idleTimeout;

    private ExecutorSelector executorSelector;

    public Http2AcceptOptions() {
        acceptSize = DEFAULT_ACCEPT_SIZE;
        workerSize = DEFAULT_WORKER_SIZE;

        acceptBacklog = DEFAULT_ACCEPT_BACKLOG;
        idleTimeout = DEFAULT_IDLE_TIMEOUT;
    }

    public int getAcceptSize() {
        return acceptSize;
    }

    public void setAcceptSize(int acceptSize) {
        this.acceptSize = acceptSize;
    }

    public int getWorkerSize() {
        return workerSize;
    }

    public void setWorkerSize(int workerSize) {
        this.workerSize = workerSize;
    }

    public int getAcceptBacklog() {
        return acceptBacklog;
    }

    public void setAcceptBacklog(int acceptBacklog) {
        this.acceptBacklog = acceptBacklog;
    }

    public boolean isEnableSsl() {
        return enableSsl;
    }

    public void setEnableSsl(boolean enableSsl) {
        this.enableSsl = enableSsl;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public ExecutorSelector getExecutorSelector() {
        return executorSelector;
    }

    public void setExecutorSelector(ExecutorSelector executorSelector) {
        this.executorSelector = executorSelector;
    }

}
