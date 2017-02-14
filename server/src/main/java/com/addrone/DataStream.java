package com.addrone;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by efrytom on 2017-01-13.
 */
public class DataStream implements Runnable {

    private DataInputStream input;
    private DataOutputStream output;
    private Parser parser;
    private boolean dataReceived;
    private ExecutorService executorService;

    public DataStream(DataInputStream input, DataOutputStream output) {
        this.input = input;
        this.output = output;
        executorService = Executors.newFixedThreadPool(5);
    }

    @Override
    public void run() {
        byte[] byteArray = new byte[1024];
        byte[] bytes = new byte[1024];
        Arrays.fill(bytes, (byte) 1);
        while(true) {
            try {
                int len = input.available();
                if(len > 0){
                    dataReceived = true;
                }
                if (len > 1024) len = 1024;
                int read = input.read(byteArray, 0, len);
                output.write(byteArray, 0, read);
                executorService.execute(new Parser(byteArray, len));
                if(Arrays.equals(byteArray, bytes) && dataReceived){
                    input.close();
                    output.close();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}