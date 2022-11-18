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

/**
 * method level config
 * 
 * @author dinstone
 *
 */
public class MethodConfig {

    private Method method;

    private String methodName;

    private Class<?> paramType;

    private Class<?> returnType;

    private boolean asyncInvoke;

    private int invokeTimeout;

    private int invokeRetry;

    public MethodConfig(Method method, Class<?> paramType) {
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

    public MethodConfig(String methodName) {
        this.methodName = methodName;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getDeclarClass() {
        return method.getDeclaringClass();
    }

    public Class<?>[] getExceptionTypes() {
        return method.getExceptionTypes();
    }

    public String getMethodName() {
        return methodName;
    }

    public void setParamType(Class<?> paramType) {
        this.paramType = paramType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public Class<?> getParamType() {
        return paramType;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public boolean isAsyncInvoke() {
        return asyncInvoke;
    }

    public void setAsyncInvoke(boolean asyncInvoke) {
        this.asyncInvoke = asyncInvoke;
    }

    protected static String description(MethodConfig mi) {
        StringBuilder desc = new StringBuilder();
        desc.append(getTypeName(mi.getReturnType()) + " ");
        desc.append(getTypeName(mi.getDeclarClass()) + ".");
        desc.append(mi.getMethodName() + "(");
        desc.append(getTypeName(mi.getParamType()));
        desc.append(")");
        return desc.toString();
    }

    protected static String getTypeName(Class<?> type) {
        if (type.isArray()) {
            try {
                Class<?> cl = type;
                int dimensions = 0;
                while (cl.isArray()) {
                    dimensions++;
                    cl = cl.getComponentType();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(cl.getName());
                for (int i = 0; i < dimensions; i++) {
                    sb.append("[]");
                }
                return sb.toString();
            } catch (Throwable e) {
            }
        }
        return type.getName();
    }

    public int getInvokeTimeout() {
        return invokeTimeout;
    }

    public void setInvokeTimeout(int invokeTimeout) {
        this.invokeTimeout = invokeTimeout;
    }

    public int getInvokeRetry() {
        return invokeRetry;
    }

    public void setInvokeRetry(int invokeRetry) {
        this.invokeRetry = invokeRetry;
    }

}
