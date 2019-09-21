package com.dinstone.focus.client;

import com.dinstone.focus.endpoint.EndpointOption;
import com.dinstone.photon.ConnectOptions;

public class ClientEndpointOption extends EndpointOption {

    private ConnectOptions connectOptions;

    public ConnectOptions getConnectOptions() {
        return connectOptions;
    }

    public void setConnectOptions(ConnectOptions connectOptions) {
        this.connectOptions = connectOptions;
    }

}
