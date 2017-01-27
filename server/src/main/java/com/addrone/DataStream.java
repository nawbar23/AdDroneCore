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
        byte[] byteArray = new byte[1024];
        while(true) {
            try {
                //parser.parse(byteArray);
                input.read(byteArray);
                output.write(byteArray);
                int len = input.available();
                if (len > 1024) len = 1024;
                int read = input.read(byteArray, 0, len);
                output.write(byteArray, 0, read);
            } catch (IOException e) {
                e.printStackTrace();
                server.restoreConnection();
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
