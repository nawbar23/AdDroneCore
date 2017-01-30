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
    private int serverPort;

    private ServerSocket server;
    private Socket serverSocket;
    private Socket deviceSocket;

    private boolean connectorInitiator;


    private DataInputStream serverInput;
    private DataOutputStream serverOutput;
    private DataInputStream deviceInput;
    private DataOutputStream deviceOutput;

    public Server(int port, ExecutorService executorService) {
        this.executorService = executorService;
        this.port = port;
    }

    public Server(int port, ExecutorService executorService, boolean connectorInitiator, int serverPort) {
        this.port = port;
        this.executorService = executorService;
        this.connectorInitiator = connectorInitiator;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        startServerSocket();
        connectWithServerSocket();
        connectWithDevice();
        System.out.println(executorService.toString());
        executorService.execute(new DataStream(serverInput,deviceOutput,this));
        executorService.execute(new DataStream(deviceInput,serverOutput,this));

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

    private void connectWithServerSocket() {
        try {
            if(serverSocket == null) {
                if (connectorInitiator) {
                    serverSocket = new Socket("localhost", serverPort);
                    serverInput = new DataInputStream(serverSocket.getInputStream());
                    serverOutput = new DataOutputStream(serverSocket.getOutputStream());
                } else {
                    serverSocket = server.accept();
                    serverInput = new DataInputStream(serverSocket.getInputStream());
                    serverOutput = new DataOutputStream(serverSocket.getOutputStream());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreConnection() {
        System.out.println("Odnawiam polaczenie");
        deviceSocket = null;
        run();
    }

    private void startServerSocket() {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
