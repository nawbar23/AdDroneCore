package com.addrone;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by efrytom on 2017-01-13.
 */
public class DataStream implements Runnable {

    private Server server;
    private DataInputStream input;
    private DataOutputStream output;
    private byte[] byteArray;

    public DataStream(DataInputStream input, DataOutputStream output, Server server) {
        this.input = input;
        this.output = output;
        this.server = server;
    }

    @Override
    public void run() {
        while(true) {
            try {
                byteArray = new byte[input.available()];
                input.read(byteArray);
                output.write(byteArray);
            } catch (IOException e) {
                e.printStackTrace();
                server.restoreConnection();
            }
        }
    }
}
