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
package com.dinstone.focus.server.invoke;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.dinstone.focus.exception.ExceptionHelper;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class LocalInvokeHandler implements InvokeHandler {

    private Class<?> clazz;
    private Object target;

    public LocalInvokeHandler(Class<?> sic, Object sio) {
        this.clazz = sic;
        this.target = sio;
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        try {
            Class<?>[] paramTypes = getParamTypes(call.getParams(), call.getParamTypes());
            Method method = clazz.getDeclaredMethod(call.getMethod(), paramTypes);
            Object resObj = method.invoke(target, call.getParams());
            return new Reply(resObj);
        } catch (InvocationTargetException e) {
            Throwable te = ExceptionHelper.targetException(e);
            String message = ExceptionHelper.getMessage(te);
            return new Reply(new FocusException(message, te));
        }
    }

    private Class<?>[] getParamTypes(Object[] params, Class<?>[] paramTypes) {
        if (paramTypes == null && params != null) {
            paramTypes = parseParamTypes(params);
        }
        return paramTypes;
    }

    protected Class<?>[] getParamTypes(Call call) {
        Class<?>[] paramTypes = call.getParamTypes();
        Object[] params = call.getParams();
        if (paramTypes == null && params != null) {
            paramTypes = parseParamTypes(params);
        }
        return paramTypes;
    }

    private Class<?>[] parseParamTypes(Object[] args) {
        Class<?>[] cs = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            cs[i] = arg.getClass();
        }

        return cs;
    }

}
