package com.dinstone.focus.client;

import java.io.IOException;

import com.dinstone.focus.example.DemoService;

public class ClientTest {

    public static void main(String[] args) {
        ClientOptions option = new ClientOptions().connect("localhost", 3333);
        Client client = new Client(option);
        DemoService ds = client.importing(DemoService.class);
        System.out.println(ds.hello("dinstoneo"));

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        client.destroy();
    }

}
