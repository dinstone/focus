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
package com.dinstone.focus.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.dinstone.focus.exception.ExceptionHelper;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.rpc.Attach;
import com.dinstone.focus.rpc.Reply;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.util.ByteStreamUtil;

public class ErrorCodec {

    public Reply decode(Response response) {
        Reply reply = new Reply();
        try {
            reply.attach(Attach.decode(response.getHeaders()));

            ByteArrayInputStream bai = new ByteArrayInputStream(response.getContent());
            String message = ByteStreamUtil.readString(bai);
            String stackTrace = ByteStreamUtil.readString(bai);
            reply.setData(new FocusException(message, stackTrace));
        } catch (IOException e) {
            reply.setData(e);
        }
        return reply;
    }

    public void encode(Response response, Reply reply) {
        try {
            response.setHeaders(Attach.encode(reply.attach()));
        } catch (IOException e) {
            throw new CodecException("encode reply attach error", e);
        }

        try {
            Throwable exception = (Throwable) reply.getData();
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            ByteStreamUtil.writeString(bao, exception.getMessage());
            ByteStreamUtil.writeString(bao, ExceptionHelper.getStackTrace(exception));

            response.setContent(bao.toByteArray());
        } catch (IOException e) {
            // igonre
        }
    }
}
