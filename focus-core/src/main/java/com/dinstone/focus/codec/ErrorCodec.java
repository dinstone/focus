package com.dinstone.focus.codec;

import java.io.UnsupportedEncodingException;

import com.dinstone.focus.RpcException;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Response;

public class ErrorCodec {

    public void encode(Response response, RpcException exception) {
        Headers headers = response.getHeaders();
        headers.put("error.code", "" + exception.getCode());
        headers.put("error.message", "" + exception.getMessage());

        if (exception.getStack() != null) {
            try {
                response.setContent(exception.getStack().getBytes("utf-8"));
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
        }
    }

    public RpcException decode(Response response) {
        RpcException error = null;
        try {
            Headers headers = response.getHeaders();
            int code = Integer.parseInt(headers.get("error.code"));
            String message = headers.get("error.message");
            error = new RpcException(code, message);
            if (response.getContent() != null) {
                String stack = new String(response.getContent(), "utf-8");
                error = new RpcException(code, message, stack);
            } else {
                error = new RpcException(code, message);
            }
        } catch (Exception e) {
            error = new RpcException(499, "decode exception error", e);
        }
        return error;
    }

}
