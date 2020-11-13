package com.dinstone.focus.server;

import java.io.IOException;

import com.dinstone.clutch.zookeeper.ZookeeperRegistryConfig;
import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.example.DemoServiceImpl;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class ZkFocusServerTest {
    private static final Logger LOG = LoggerFactory.getLogger(FocusServerTest.class);

    public static void main(String[] args) {
        // setting registry config
        ZookeeperRegistryConfig registryConfig = new ZookeeperRegistryConfig().setZookeeperNodes("localhost:2181");
        ServerOptions setEndpointCode = new ServerOptions().setRegistryConfig(registryConfig).listen("localhost", 3333)
                .setEndpointCode("com.rpc.demo.server");
        Server server = new Server(setEndpointCode);
        server.exporting(DemoService.class, new DemoServiceImpl());
        // server.start();
        LOG.info("server start");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.destroy();
        LOG.info("server stop");
    }

}
