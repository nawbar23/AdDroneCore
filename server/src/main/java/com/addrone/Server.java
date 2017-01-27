package com.addrone;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * Created by efytom on 2017-01-13.
 */
public class Server implements Runnable {

    private final int port;
    private ExecutorService executorService;

    private ServerSocket server;
    private Socket appSocket;
    private Socket deviceSocket;
    private Parser deviceParser;
    private Parser appParser;


    private DataInputStream appInput;
    private DataOutputStream appOutput;
    private DataInputStream deviceInput;
    private DataOutputStream deviceOutput;

    public Server(int port, ExecutorService executorService) {
        this.executorService = executorService;
        this.port = port;
    }

    @Override
    public void run() {
        startServerSocket();
        connectWithDevice();
        connectWithApp();
        deviceParser = new Parser(executorService);
        appParser = new Parser(executorService);
        executorService.execute(new DataStream(appInput,deviceOutput,this, appParser));
        executorService.execute(new DataStream(deviceInput, appOutput,this, deviceParser));
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



    public void restoreConnection() {
        System.out.println("Odnawiam polaczenie");
        deviceSocket = null;
        appSocket = null;
        run();
    }

    private void startServerSocket() {
        try {
            if(server == null) {
                server = new ServerSocket(port);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
