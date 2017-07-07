package com.addrone;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by efrytom on 2017-01-13.
 */
public class DataStream implements Runnable {

    private DataInputStream input;
    private DataOutputStream output;
    private boolean dataReceived;
    private byte[] parserData;
    private ReentrantLock reentrantLock;
    private ScheduledExecutorService scheduledExecutorService;

    public DataStream(DataInputStream input, DataOutputStream output) {
        this.input = input;
        this.output = output;
        reentrantLock = new ReentrantLock();
    }

    public DataStream(DataInputStream input, DataOutputStream output, ScheduledExecutorService scheduledExecutorService) {
        this.input = input;
        this.output = output;
        this.scheduledExecutorService = scheduledExecutorService;
        reentrantLock = new ReentrantLock();
    }

    public byte[] sendToParser(){
        reentrantLock.lock();
        byte[] bytesArrays = parserData;
        reentrantLock.unlock();
        return bytesArrays;
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
                if(len > 0) {
                    int read = input.read(byteArray, 0, len);
                    output.write(byteArray, 0, read);
                    parserData = byteArray;
                }
                if(Arrays.equals(byteArray, bytes) && dataReceived){
                    input.close();
                    output.close();
                    if(scheduledExecutorService != null){
                        scheduledExecutorService.shutdown();
                    }
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