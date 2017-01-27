package com.addrone;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * Created by efrytom on 2017-01-13.
 */
public class DataStream implements Runnable {

    private Server server;
    private DataInputStream input;
    private DataOutputStream output;
    private Parser parser;
    private byte[] byteArray;

//    public DataStream(DataInputStream input, DataOutputStream output, Server server) {
//        this.input = input;
//        this.output = output;
//        this.server = server;
//    }

    public DataStream(DataInputStream input, DataOutputStream output, Server server, Parser parser) {
        this.input = input;
        this.output = output;
        this.server = server;
        this.parser = parser;
    }

    @Override
    public void run() {
        while(true) {
            try {
                byteArray = new byte[input.available()];
                //parser.parse(byteArray);
                input.read(byteArray);
                output.write(byteArray);
            } catch (IOException e) {
                e.printStackTrace();
                server.restoreConnection();
            }
        }
    }
}
