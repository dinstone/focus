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
package com.dinstone.focus.config;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public abstract class AbstractMethodConfig implements MethodConfig {

    protected Method method;

    protected String methodName;

    protected Class<?> paramType;

    protected Class<?> returnType;

    protected boolean asyncInvoke;

    protected int timeoutMillis;

    protected int timeoutRetry;

    public AbstractMethodConfig(Method method, Class<?> paramType) {
        this.method = method;
        this.paramType = paramType;
        this.methodName = method.getName();

        Type type = method.getGenericReturnType();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType.equals(CompletableFuture.class) || rawType.equals(Future.class)) {
                returnType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                asyncInvoke = true;
            } else {
                returnType = method.getReturnType();
            }
        } else {
            returnType = method.getReturnType();
        }
    }

    public AbstractMethodConfig(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    public void setParamType(Class<?> paramType) {
        this.paramType = paramType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    @Override
    public Class<?> getParamType() {
        return paramType;
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public boolean isAsyncInvoke() {
        return asyncInvoke;
    }

    public void setAsyncInvoke(boolean asyncInvoke) {
        this.asyncInvoke = asyncInvoke;
    }

    @Override
    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public int getTimeoutRetry() {
        return timeoutRetry;
    }

    public void setTimeoutRetry(int timeoutRetry) {
        this.timeoutRetry = timeoutRetry;
    }

    @Override
    public String toString() {
        return "MethodConfig [methodName=" + methodName + ", paramType=" + paramType + ", returnType=" + returnType
                + ", asyncInvoke=" + asyncInvoke + ", timeoutMillis=" + timeoutMillis + ", timeoutRetry=" + timeoutRetry
                + "]";
    }

}
