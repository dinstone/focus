package com.dinstone.focus.server.transport;

import java.util.concurrent.Executor;

import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.focus.server.processor.RpcMessageProcessor;
import com.dinstone.photon.Acceptor;
import com.dinstone.photon.handler.MessageContext;
import com.dinstone.photon.message.Message;
import com.dinstone.photon.message.Notice;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.processor.MessageProcessor;

public class AcceptorFactory {

    private ServerOptions serverOption;

    public AcceptorFactory(ServerOptions serverOption) {
        this.serverOption = serverOption;
    }

    public Acceptor create(final InvokeHandler invoker) {
        Acceptor acceptor = new Acceptor(serverOption.getAcceptOptions());
        final RpcMessageProcessor rpcProcessor = new RpcMessageProcessor(invoker);
        acceptor.setMessageProcessor(new MessageProcessor() {

            @Override
            public void process(final MessageContext context, final Message message) {
                if (message instanceof Request) {
                    Executor executor = context.getDefaultExecutor();
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            rpcProcessor.process(context, (Request) message);
                        }
                    });
                } else if (message instanceof Notice) {
                    Executor executor = context.getDefaultExecutor();
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            rpcProcessor.process(context, (Notice) message);
                        }
                    });
                }
            }
        });
        return acceptor;
    }

}
