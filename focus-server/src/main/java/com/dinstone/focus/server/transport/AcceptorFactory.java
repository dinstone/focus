package com.dinstone.focus.server.transport;

import com.dinstone.focus.invoker.ServiceInvoker;
import com.dinstone.focus.transport.TransportConfig;
import com.dinstone.photon.AcceptOptions;
import com.dinstone.photon.Acceptor;

public class AcceptorFactory {

    public Acceptor create(TransportConfig transportConfig, final ServiceInvoker invoker) {
        AcceptOptions acceptOptions = new AcceptOptions();
        Acceptor acceptor = new Acceptor(acceptOptions);
        acceptor.setMessageProcessor(new FocusMessageProcessor(invoker));
        return acceptor;
    }

}
