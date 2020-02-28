package com.dinstone.focus.client;

import java.io.IOException;

import com.dinstone.focus.example.DemoService;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class ClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(ClientTest.class);

    public static void main(String[] args) {

        LOG.info("init start");
        ClientOptions option = new ClientOptions().connect("localhost", 3333);
        Client client = new Client(option);
        DemoService ds = client.importing(DemoService.class);

        LOG.info("int end");

        int c = 0;
        while (c < 200) {
            System.out.println(ds.hello("dinstoneo"));
            
            c++;
        }
        
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        client.destroy();
    }

}
