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

    public DataStream(DataInputStream input, DataOutputStream output, Server server) {
        this.input = input;
        this.output = output;
        this.server = server;
    }

    @Override
    public void run() {
        byte[] byteArray = new byte[1024];
        while(true) {
            try {
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
