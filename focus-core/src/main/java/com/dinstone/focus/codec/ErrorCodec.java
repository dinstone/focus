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

import com.dinstone.focus.FocusException;
import com.dinstone.focus.utils.ByteStreamUtil;
import com.dinstone.photon.message.Response;

public class ErrorCodec {

    public void encode(Response response, FocusException exception) {
        try {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            ByteStreamUtil.writeString(bao, "" + exception.getCode());
            ByteStreamUtil.writeString(bao, exception.getMessage());
            ByteStreamUtil.writeString(bao, exception.getStack());

            response.setContent(bao.toByteArray());
        } catch (IOException e) {
            // igonre
        }
    }

    public FocusException decode(Response response) {
        FocusException error = null;
        try {
            byte[] encoded = response.getContent();
            if (encoded != null) {
                ByteArrayInputStream bai = new ByteArrayInputStream(encoded);
                int code = Integer.parseInt(ByteStreamUtil.readString(bai));
                String message = ByteStreamUtil.readString(bai);
                String stack = ByteStreamUtil.readString(bai);
                error = new FocusException(code, message, stack);
            } else {
                error = new FocusException(599, "unkown exception");
            }
        } catch (Exception e) {
            error = new FocusException(499, "decode exception error", e);
        }
        return error;
    }
}
