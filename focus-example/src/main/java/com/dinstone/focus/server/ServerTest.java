package com.dinstone.focus.server;

import java.io.IOException;

import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.example.DemoServiceImpl;

public class ServerTest {

    public static void main(String[] args) {
        Server server = new ServerBuilder().bind("localhost", 3333).build();
        server.exporting(DemoService.class, new DemoServiceImpl());
        server.start();

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.stop();
    }

}
