package com.dinstone.focus.server.processor;

import com.dinstone.focus.RpcException;
import com.dinstone.focus.codec.Codec;
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Reply;
import com.dinstone.focus.server.transport.AcceptorFactory;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.handler.MessageContext;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Notice;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Status;

public final class RpcMessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptorFactory.class);

    private final InvokeHandler invoker;

    public RpcMessageProcessor(InvokeHandler invoker) {
        this.invoker = invoker;
    }

    public void process(MessageContext context, Notice notice) {
        LOG.info("notice is {}", notice.getContent());
    }

    public void process(MessageContext context, Request request) {
        String cname = request.getHeaders().get("rpc.codec");
        Codec codec = CodecManager.find(cname);

        RpcException exception = null;
        try {
            Reply reply = invoker.invoke(codec.decode(request));

            Response response = new Response();
            response.setId(request.getId());
            response.setStatus(Status.SUCCESS);

            Headers headers = new Headers();
            headers.put("rpc.codec", cname);
            response.setHeaders(headers);

            codec.encode(response, reply);

            context.getConnection().write(response);
        } catch (RpcException e) {
            exception = e;
        } catch (Throwable e) {
            exception = new RpcException(500, "service exception", e);
        }

        if (exception != null) {
            Response response = new Response();
            response.setId(request.getId());
            response.setStatus(Status.ERROR);

            Headers headers = new Headers();
            response.setHeaders(headers);

            CodecManager.getErrorCodec().encode(response, exception);

            context.getConnection().write(response);
        }
    }

}