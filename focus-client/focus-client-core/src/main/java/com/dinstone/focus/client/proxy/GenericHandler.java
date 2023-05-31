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
package com.dinstone.focus.client.proxy;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.dinstone.focus.client.GenericService;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

class GenericHandler implements GenericService {

    private final ServiceConfig serviceConfig;
    private final Handler invokeHandler;

    public GenericHandler(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
        this.invokeHandler = serviceConfig.getHandler();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P> HashMap<String, Object> sync(String methodName, P parameter) throws Exception {
        return sync(HashMap.class, methodName, parameter);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, P> R sync(Class<R> returnType, String methodName, P parameter) throws Exception {
        MethodConfig methodConfig = getMethodConfig(returnType, methodName, parameter);

        Call call = new Call(methodName, parameter);
        call.setGroup(serviceConfig.getGroup());
        call.setService(serviceConfig.getService());
        call.setTimeout(methodConfig.getInvokeTimeout());

        CompletableFuture<Reply> future = invokeHandler.handle(call);

        return (R) future.get(call.getTimeout(), TimeUnit.MILLISECONDS).getResult();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <P> CompletableFuture<HashMap> async(String methodName, P parameter) throws Exception {
        return async(HashMap.class, methodName, parameter);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public <R, P> CompletableFuture<R> async(Class<R> returnType, String methodName, P parameter) throws Exception {
        MethodConfig methodConfig = getMethodConfig(returnType, methodName, parameter);

        Call call = new Call(methodName, parameter);
        call.setGroup(serviceConfig.getGroup());
        call.setService(serviceConfig.getService());
        call.setTimeout(methodConfig.getInvokeTimeout());

        CompletableFuture<Reply> future = invokeHandler.handle(call);

        return (CompletableFuture<R>) future.thenApply(reply -> reply.getResult());
    }

    private <P, R> MethodConfig getMethodConfig(Class<R> returnType, String methodName, P parameter) {
        MethodConfig methodConfig = serviceConfig.getMethodConfig(methodName);
        if (methodConfig == null) {
            methodConfig = new MethodConfig(methodName);
            methodConfig.setParamType(parameter.getClass());
            methodConfig.setReturnType(returnType);
            methodConfig.setAsyncInvoke(true);
            methodConfig.setInvokeTimeout(serviceConfig.getTimeout());
            serviceConfig.addMethodConfig(methodConfig);
        }
        return methodConfig;
    }

}