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
package com.dinstone.focus.config;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class MethodInfo {

    private Method method;

    private String methodName;

    private Class<?> paramType;

    private Class<?> returnType;

    private boolean asyncMethod;

    public MethodInfo(Method method, Class<?> paramType) {
        this.method = method;
        this.paramType = paramType;
        this.methodName = method.getName();

        Type type = method.getGenericReturnType();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType.equals(CompletableFuture.class) || rawType.equals(Future.class)) {
                returnType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                asyncMethod = true;
            }
        } else {
            returnType = method.getReturnType();
        }
    }

    public MethodInfo(String methodName, Class<?> paramType, Class<?> returnType) {
        this.methodName = methodName;
        this.paramType = paramType;
        this.returnType = returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getParamType() {
        return paramType;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Class<?> getDeclarClass() {
        return method.getDeclaringClass();
    }

    public Class<?>[] getExceptionTypes() {
        return method.getExceptionTypes();
    }

    public boolean isAsyncMethod() {
        return asyncMethod;
    }

    public MethodInfo setAsyncMethod(boolean asyncMethod) {
        this.asyncMethod = asyncMethod;
        return this;
    }

}
