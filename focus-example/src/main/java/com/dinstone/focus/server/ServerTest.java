package com.dinstone.focus.server;

import java.io.IOException;

import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.example.DemoServiceImpl;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class ServerTest {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        Server server = new Server(new ServerOptions().bind("localhost", 3333));
        server.exporting(DemoService.class, new DemoServiceImpl());

        long t = System.currentTimeMillis();
        server.start();
        LOG.info("server start take's {}ms", System.currentTimeMillis() - t);
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.stop();
    }

}
