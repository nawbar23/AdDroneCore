package com.serverSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * Created by emergency on 2016-12-27.
 */
public class DroneServer implements Runnable {

    private ExecutorService executorService;

    private Socket droneSocket;
    private ServerSocket serverSocket;
    private Socket androidServerSocket;

    private DataInputStream androidServerInput;
    private DataOutputStream androidServerOutput;
    private DataOutputStream droneOutput;
    private DataInputStream droneInput;

    public DroneServer(ServerSocket serverSocket, ExecutorService executorService) {
        this.serverSocket = serverSocket;
        this.executorService = executorService;
    }
    @Override
    public void run() {
        if(androidServerSocket == null) {
            connectWithAppServer();
        }
        waitForConnectionWithDrone();
        executorService.execute(new DataPipe(androidServerInput, droneOutput, this));
        executorService.execute(new DataPipe(droneInput, androidServerOutput, this));
    }

    private void connectWithAppServer() {
        try {
            androidServerSocket = serverSocket.accept();
            androidServerInput = new DataInputStream(androidServerSocket.getInputStream());
            androidServerOutput = new DataOutputStream(androidServerSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForConnectionWithDrone() {
        try {
            droneSocket = serverSocket.accept();
            droneInput = new DataInputStream(droneSocket.getInputStream());
            droneOutput = new DataOutputStream(droneSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreConnection() {
        droneSocket = null;
        run();
    }
}

