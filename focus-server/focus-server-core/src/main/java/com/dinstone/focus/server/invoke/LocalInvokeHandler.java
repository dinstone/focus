/*
 * Copyright (C) 2019~2024 dinstone<dinstone@163.com>
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.BusinessException;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.server.config.ProviderServiceConfig;

public class LocalInvokeHandler implements Handler {

    private final ProviderServiceConfig serviceConfig;

    public LocalInvokeHandler(ServiceConfig serviceConfig) {
        this.serviceConfig = (ProviderServiceConfig) serviceConfig;
    }

    @Override
    public CompletableFuture<Object> handle(Invocation invocation) {
        Throwable error;
        try {
            Object target = serviceConfig.getTarget();
            Object parameter = invocation.getParameter();
            MethodConfig methodConfig = invocation.getMethodConfig();
            Object result = methodConfig.getMethod().invoke(target, parameter);
            if (methodConfig.isAsyncInvoke() && result instanceof Future) {
                Future<?> future = (Future<?>) result;
                int invokeTimeout = invocation.getTimeout();
                result = future.get(invokeTimeout, TimeUnit.MILLISECONDS);
            }
            return CompletableFuture.completedFuture(result);
        } catch (InvocationTargetException e) {
            Throwable te = e.getTargetException();
            if (te instanceof UndeclaredThrowableException) {
                // undeclared checked exception
                error = new BusinessException(ErrorCode.UNDECLARED_ERROR, te.getCause());
            } else if (te instanceof RuntimeException) {
                // runtime exception
                error = new BusinessException(ErrorCode.RUNTIME_ERROR, te);
            } else {
                // declared checked exception
                error = new BusinessException(ErrorCode.DECLARED_ERROR, te);
            }
        } catch (IllegalArgumentException e) {
            error = new ServiceException(ErrorCode.PARAM_ERROR, e);
        } catch (IllegalAccessException e) {
            error = new ServiceException(ErrorCode.ACCESS_ERROR, e);
        } catch (TimeoutException e) {
            error = new InvokeException(ErrorCode.TIMEOUT_ERROR, e);
        } catch (InterruptedException e) {
            error = new InvokeException(ErrorCode.INVOKE_ERROR, e);
        } catch (ExecutionException e) {
            error = new BusinessException(ErrorCode.RUNTIME_ERROR, e.getCause());
        }

        CompletableFuture<Object> cf = new CompletableFuture<Object>();
        cf.completeExceptionally(error);
        return cf;
    }

}
