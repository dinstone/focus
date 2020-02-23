package com.dinstone.focus.server.processor;

import java.util.concurrent.Executor;

import com.dinstone.focus.RpcException;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;
import com.dinstone.focus.serializer.Serializer;
import com.dinstone.focus.serializer.SerializerManager;
import com.dinstone.focus.server.transport.AcceptorFactory;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.handler.MessageContext;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Message;
import com.dinstone.photon.message.Notice;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Status;
import com.dinstone.photon.processor.MessageProcessor;

public final class FocusMessageProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptorFactory.class);

    private final InvokeHandler invoker;

    public FocusMessageProcessor(InvokeHandler invoker) {
        this.invoker = invoker;
    }

    public void process(MessageContext context, Notice notice) {
        LOG.info("notice is {}", notice.getContent());
    }

    public void process(MessageContext context, Request request) {
        String sname = request.getHeaders().get("serializer");
        Serializer<?> serializer = SerializerManager.getInstance().find(sname);

        Reply reply = null;
        try {
            reply = invoker.invoke((Call) serializer.decode(request.getContent()));
        } catch (Throwable e) {
            reply = new Reply(500, e.getMessage());
        }

        Response response = new Response();
        response.setId(request.getId());
        response.setHeaders(new Headers());
        try {
            Serializer<Reply> s = SerializerManager.getInstance().find(Reply.class);
            response.getHeaders().put("serializer", s.name());
            response.setStatus(Status.SUCCESS);
            byte[] content = s.encode(reply);
            response.setContent(content);
        } catch (Exception e) {
            Serializer<RpcException> se = SerializerManager.getInstance().find(RpcException.class);
            response.getHeaders().put("serializer", se.name());
            response.setStatus(Status.ERROR);
            try {
                byte[] content = se.encode(new RpcException(500, e.getMessage()));
                response.setContent(content);
            } catch (Exception e1) {
                //
            }
        }

        context.getConnection().write(response);
    }

    @Override
    public void process(final MessageContext context, final Message message) {
        if (message instanceof Request) {
            Executor executor = context.getDefaultExecutor();
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    process(context, (Request) message);
                }
            });
        }

        if (message instanceof Notice) {
            Executor executor = context.getDefaultExecutor();
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    process(context, (Notice) message);
                }
            });
        }
    }
}