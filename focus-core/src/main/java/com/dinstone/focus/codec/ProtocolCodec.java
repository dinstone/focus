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
package com.dinstone.focus.codec;

import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;

public interface ProtocolCodec {

    Request encode(Call call, Class<?> paramType) throws CodecException;

    Call decode(Request request, Class<?> paramType) throws CodecException;

    Response encode(Reply reply, Class<?> returnType) throws CodecException;

    Reply decode(Response response, Class<?> returnType) throws CodecException;

}