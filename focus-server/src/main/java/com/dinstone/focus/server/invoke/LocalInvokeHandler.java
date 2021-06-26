/*
 * Copyright (C) 2019~2021 dinstone<dinstone@163.com>
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
import java.lang.reflect.Method;

import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.ExceptionHelper;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class LocalInvokeHandler implements InvokeHandler {

    private ServiceConfig serviceConfig;

    public LocalInvokeHandler(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        try {
            Method method = serviceConfig.findMethod(call.getMethod());
            Object resObj = method.invoke(serviceConfig.getTarget(), call.getParams());
            return new Reply(resObj);
        } catch (InvocationTargetException e) {
            Throwable te = ExceptionHelper.targetException(e);
            String message = ExceptionHelper.getMessage(te);
            return new Reply(new FocusException(message, te));
        }
    }

}
