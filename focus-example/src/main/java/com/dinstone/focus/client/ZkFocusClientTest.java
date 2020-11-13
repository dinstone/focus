package com.dinstone.focus.client;

import java.io.IOException;

import com.dinstone.clutch.zookeeper.ZookeeperRegistryConfig;
import com.dinstone.focus.example.DemoService;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.ConnectOptions;

public class ZkFocusClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(FocusClientTest.class);

    public static void main(String[] args) {

        LOG.info("init start");
        ConnectOptions connectOptions = new ConnectOptions();
        connectOptions.setProcessorSize(0);

        ClientOptions option = new ClientOptions().setConnectOptions(connectOptions)
                .setRegistryConfig(new ZookeeperRegistryConfig()).setEndpointCode("com.rpc.demo.client");

        Client client = new Client(option);
        DemoService ds = client.importing(DemoService.class);

        try {
            ds.hello(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOG.info("int end");

        execute(ds, "hot: ");
        execute(ds, "exe: ");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        client.destroy();
    }

    private static void execute(DemoService ds, String tag) {
        int c = 0;
        long st = System.currentTimeMillis();
        int loopCount = 200000;
        while (c < loopCount) {
            ds.hello("dinstoneo", c);
            c++;
        }
        long et = System.currentTimeMillis() - st;
        System.out.println(tag + et + " ms, " + (loopCount * 1000 / et) + " tps");
    }

}
