package com.dinstone.focus.server.transport;

import java.util.concurrent.Executor;

import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.codec.CodecException;
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.server.ExecutorSelector;
import com.dinstone.photon.ExchangeException;
import com.dinstone.photon.codec.ExceptionCodec;
import com.dinstone.photon.connection.Connection;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;
import com.dinstone.photon.processor.MessageProcessor;
import com.dinstone.photon.util.ExceptionUtil;

public final class FocusProcessor implements MessageProcessor {
    private final ImplementBinding binding;
    private ExecutorSelector selector;

    public FocusProcessor(ImplementBinding binding, ExecutorSelector selector) {
        this.binding = binding;
        this.selector = selector;
    }

    @Override
    public void process(Connection connection, Object msg) {
        if (msg instanceof Request) {
            Executor executor = null;
            Request request = (Request) msg;
            if (selector != null) {
                Headers headers = request.getHeaders();
                String g = headers.get("rpc.call.group");
                String s = headers.get("rpc.call.service");
                String m = headers.get("rpc.call.method");
                executor = selector.select(g, s, m);
            }
            if (executor != null) {
                executor.execute(new Runnable() {

                    @Override
                    public void run() {
                        invoke(binding, connection, request);
                    }
                });
            } else {
                invoke(binding, connection, request);
            }
        }
    }

    private void invoke(final ImplementBinding binding, Connection connection, Request request) {
        if (request.isTimeout()) {
            return;
        }

        ExchangeException exception = null;
        try {
            ProtocolCodec codec = CodecManager.codec(request.getCodec());
            // decode call from request
            Call call = codec.decode(request);

            ServiceConfig config = binding.lookup(call.getService(), call.getGroup());
            if (config == null) {
                throw new NoSuchMethodException("unkown service: " + call.getService() + "[" + call.getGroup() + "]");
            }
            if (!config.hasMethod(call.getMethod())) {
                throw new NoSuchMethodException(
                        "unkown method: " + call.getService() + "[" + call.getGroup() + "]" + call.getMethod());
            }

            // invoke call
            Reply reply = config.getHandler().invoke(call);

            // encode reply to response
            Response response = codec.encode(reply);
            response.setMsgId(request.getMsgId());
            response.setStatus(Status.SUCCESS);
            response.setCodec(codec.codecId());

            // send response with reply
            connection.send(response);
            return;
        } catch (CodecException e) {
            String message = ExceptionUtil.getMessage(e);
            exception = new ExchangeException(101, message, e);
        } catch (IllegalArgumentException e) {
            String message = ExceptionUtil.getMessage(e);
            exception = new ExchangeException(102, message, e);
        } catch (IllegalAccessException e) {
            String message = ExceptionUtil.getMessage(e);
            exception = new ExchangeException(103, message, e);
        } catch (NoSuchMethodException e) {
            String message = ExceptionUtil.getMessage(e);
            exception = new ExchangeException(104, message, e);
        } catch (Throwable e) {
            String message = ExceptionUtil.getMessage(e);
            exception = new ExchangeException(109, message, e);
        }

        if (exception != null) {
            Response response = new Response();
            response.setMsgId(request.getMsgId());
            response.setStatus(Status.FAILURE);
            response.setContent(ExceptionCodec.encode(exception));
            // send response with exception
            connection.send(response);
        }
    }
}