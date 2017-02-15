package com.addrone;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * Created by efytom on 2017-01-13.
 */
public class Server implements Runnable {

    private final int port;
    private ExecutorService executorService;

    private ServerSocket server;
    private Socket appSocket;
    private Socket deviceSocket;

    private DataInputStream appInput;
    private DataOutputStream appOutput;
    private DataInputStream deviceInput;
    private DataOutputStream deviceOutput;
    private ScheduledExecutorService scheduledExecutorService;


    public Server(int port, ExecutorService executorService, ServerSocket server) {
        this.executorService = executorService;
        this.port = port;
        this.server = server;
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void run() {
        connectWithDevice();
        connectWithApp();
        DataStream dataStream = new DataStream(deviceInput, appOutput, scheduledExecutorService);
        executorService.execute(new DataStream(appInput, deviceOutput));
        Parser parser = new Parser(dataStream);
        executorService.execute(dataStream);
        scheduledExecutorService.scheduleAtFixedRate(parser,500,500,TimeUnit.MILLISECONDS);
        executorService.shutdown();
    }

    private void connectWithApp() {
        try {
            appSocket = server.accept();
            appOutput = new DataOutputStream(appSocket.getOutputStream());
            appInput = new DataInputStream(appSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void connectWithDevice() {
        try {
            deviceSocket = server.accept();
            deviceInput = new DataInputStream(deviceSocket.getInputStream());
            deviceOutput = new DataOutputStream(deviceSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}