/*
 * Copyright (C) 2019~2023 dinstone<dinstone@163.com>
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
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.BusinessException;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.server.config.ProviderServiceConfig;

public class LocalInvokeHandler implements Handler {

    private ProviderServiceConfig serviceConfig;

    public LocalInvokeHandler(ServiceConfig serviceConfig) {
        this.serviceConfig = (ProviderServiceConfig) serviceConfig;
    }

    @Override
    public CompletableFuture<Reply> handle(Call call) throws Exception {
        MethodConfig methodConfig = serviceConfig.lookup(call.getMethod());
        try {
            Object parameter = call.getParameter();
            Object target = serviceConfig.getTarget();
            Object result = methodConfig.getMethod().invoke(target, parameter);
            if (methodConfig.isAsyncInvoke() && result instanceof Future) {
                Future<?> future = (Future<?>) result;
                int invokeTimeout = call.getTimeout();
                result = future.get(invokeTimeout, TimeUnit.MILLISECONDS);
            }
            return CompletableFuture.completedFuture(new Reply(result));
        } catch (InvocationTargetException e) {
            Throwable te = e.getTargetException();
            if (te instanceof UndeclaredThrowableException) {
                // undeclared checked exception
                throw new BusinessException(ErrorCode.UNDECLARED_ERROR, te.getCause());
            } else if (te instanceof RuntimeException) {
                // runtime exception
                throw new BusinessException(ErrorCode.RUNTIME_ERROR, te);
            } else {
                // declared checked exception
                throw new BusinessException(ErrorCode.DECLARED_ERROR, te);
            }
        } catch (IllegalArgumentException e) {
            throw new ServiceException(ErrorCode.PARAM_ERROR, e);
        } catch (IllegalAccessException e) {
            throw new ServiceException(ErrorCode.ACCESS_ERROR, e);
        }
    }

}
