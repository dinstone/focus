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
package com.dinstone.focus.endpoint;

import java.util.concurrent.Future;

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
    <R, P> Future<R> async(Class<R> returnType, String methodName, P parameter) throws Exception;

}
