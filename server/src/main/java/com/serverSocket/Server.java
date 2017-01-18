package com.serverSocket;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * Created by emergency on 2017-01-13.
 */
public class Server implements Runnable {

    private final int port;
    private ExecutorService executorService;
    private int serverPort;

    private ServerSocket server;
    private Socket serverSocket;
    private Socket deviceSocket;

    private boolean connectorInititor;


    private DataInputStream serverInput;
    private DataOutputStream serverOutput;
    private DataInputStream deviceInput;
    private DataOutputStream deviceOutput;

    public Server(int port, ExecutorService executorService) {
        this.executorService = executorService;
        this.port = port;
        executorService.execute(this);
    }

    public Server(int port, ExecutorService executorService, boolean connectorInititor, int serverPort) {
        this.port = port;
        this.executorService = executorService;
        this.connectorInititor = connectorInititor;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        System.out.println(port);
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        connectWithServerSocket();
        connectWithDevice();
        executorService.execute(new DataPipe(serverInput,deviceOutput,this));
        executorService.execute(new DataPipe(deviceInput,serverOutput,this));

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
            if(connectorInititor){
                serverSocket = new Socket("localhost",serverPort);
                serverInput = new DataInputStream(serverSocket.getInputStream());
                serverOutput = new DataOutputStream(serverSocket.getOutputStream());
            }
            else {
                serverSocket = server.accept();
                serverInput = new DataInputStream(serverSocket.getInputStream());
                serverOutput = new DataOutputStream(serverSocket.getOutputStream());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private void startServerSocket() {
//        try {
//            server = new ServerSocket(port);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
