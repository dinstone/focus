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
package com.dinstone.focus.client;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public interface GenericService {

    /**
     * generic service sync invoke. only support JSON codec.
     *
     * @param <R>
     * @param <P>
     * @param returnType
     *            method return type, only support ( Object<->HashMap, String, java basic type)
     * @param methodName
     *            method name
     * @param parameter
     *            parameter object
     * 
     * @return return object
     *
     * @throws Exception
     */
    <R, P> R sync(Class<R> returnType, String methodName, P parameter) throws Exception;

    /**
     * generic service sync invoke. only support JSON codec.
     *
     * @param <P>
     * @param methodName
     *            method name
     * @param parameter
     *            parameter object
     * 
     * @return return Map object
     *
     * @throws Exception
     */
    <P> HashMap<String, Object> sync(String methodName, P parameter) throws Exception;

    /**
     * generic service async invoke. only support JSON codec.
     *
     * @param <R>
     * @param <P>
     * @param returnType
     *            method return type, only support ( Object<->HashMap, String, java basic type)
     * @param methodName
     *            method name
     * @param parameter
     *            parameter object
     * 
     * @return return future
     *
     * @throws Exception
     */
    <R, P> CompletableFuture<R> async(Class<R> returnType, String methodName, P parameter) throws Exception;

    /**
     * generic service async invoke. only support JSON codec.
     *
     * @param <P>
     * @param returnType
     *            method return type, only support ( Object<->HashMap, String, java basic type)
     * @param methodName
     *            method name
     * @param parameter
     *            parameter object
     * 
     * @return return map future
     *
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    <P> CompletableFuture<HashMap> async(String methodName, P parameter) throws Exception;

}
