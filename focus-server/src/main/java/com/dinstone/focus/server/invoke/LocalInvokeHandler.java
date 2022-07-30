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
package com.dinstone.focus.server.invoke;

import java.lang.reflect.InvocationTargetException;

import com.dinstone.focus.config.MethodInfo;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.photon.ExchangeException;
import com.dinstone.photon.utils.ExceptionUtil;

public class LocalInvokeHandler implements InvokeHandler {

    private ServiceConfig serviceConfig;

    public LocalInvokeHandler(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        MethodInfo methodInfo = serviceConfig.getMethodInfo(call.getMethod());
        try {
            Object result = methodInfo.getMethod().invoke(serviceConfig.getTarget(), call.getParameter());
            return new Reply(result);
        } catch (InvocationTargetException e) {
            Throwable te = ExceptionUtil.getTargetException(e);
            if (te instanceof RuntimeException) {
                throw new ExchangeException(302, "method throw a runtime exception", te);
            } else {
                throw new ExchangeException(301, "method throw a declared exception", te);
            }
        }
    }

}
