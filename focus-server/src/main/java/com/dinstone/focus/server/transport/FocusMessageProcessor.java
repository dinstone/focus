package com.dinstone.focus.server.transport;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;

import com.dinstone.focus.RpcException;
import com.dinstone.focus.exception.ExcptionHelper;
import com.dinstone.focus.invoker.ServiceInvoker;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.serializer.Serializer;
import com.dinstone.focus.serializer.SerializerManager;
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

    private final ServiceInvoker invoker;

    public FocusMessageProcessor(ServiceInvoker invoker) {
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
            reply = handle((Call) serializer.decode(request.getContent()));
        } catch (Exception e) {
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

    private Reply handle(Call call) {
        Reply reply = null;
        try {

            Object resObj = invoker.invoke(call.getService(), call.getGroup(), call.getMethod(), call.getTimeout(),
                    call.getParams(), call.getParamTypes());
            reply = new Reply(200, resObj);
        } catch (RpcException e) {
            reply = new Reply(e.getCode(), e.getMessage());
        } catch (NoSuchMethodException e) {
            reply = new Reply(405, "unkown method: [" + call.getGroup() + "]" + e.getMessage());
        } catch (IllegalArgumentException | IllegalAccessException e) {
            String message = "illegal access: [" + call.getGroup() + "]" + call.getService() + "." + call.getMethod()
                    + "(): " + e.getMessage();
            reply = new Reply(502, message);
        } catch (InvocationTargetException e) {
            Throwable t = ExcptionHelper.getTargetException(e);
            String message = "service exception: " + call.getGroup() + "]" + call.getService() + "." + call.getMethod()
                    + "(): " + t.getMessage();
            reply = new Reply(500, message);
        } catch (Throwable e) {
            String message = "service exception: " + call.getGroup() + "]" + call.getService() + "." + call.getMethod()
                    + "(): " + e.getMessage();
            reply = new Reply(509, message);
        }
        return reply;
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