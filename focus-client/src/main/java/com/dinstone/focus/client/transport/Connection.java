/*
 * Copyright (C) 2018~2020 dinstone<dinstone@163.com>
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
package com.dinstone.focus.client.transport;

import java.net.InetSocketAddress;

import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;

/**
 * connection abstract.
 *
 * @author guojf
 * @version 1.0.0.2013-4-10
 */
public interface Connection {

    /**
     * synchronous call.
     *
     * @param call
     * @return reply
     * @throws Exception
     */
    public Reply invoke(Call call) throws Exception;

    public InetSocketAddress getRemoteAddress();

    public InetSocketAddress getLocalAddress();

    public boolean isAlive();

    public void destroy();

}