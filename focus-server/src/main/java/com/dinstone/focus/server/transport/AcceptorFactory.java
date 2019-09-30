package com.dinstone.focus.server.transport;

import com.dinstone.focus.invoke.ServiceInvoker;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.focus.server.processor.FocusMessageProcessor;
import com.dinstone.photon.Acceptor;

public class AcceptorFactory {

    private ServerOptions serverOption;

    public AcceptorFactory(ServerOptions serverOption) {
        this.serverOption = serverOption;
    }

    public Acceptor create(final ServiceInvoker invoker) {
        Acceptor acceptor = new Acceptor(serverOption.getAcceptOptions());
        acceptor.setMessageProcessor(new FocusMessageProcessor(invoker));
        return acceptor;
    }

}
