package com.serverSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * Created by emergency on 2016-12-21.
 */
public class AppServer implements Runnable {

    private ExecutorService executorService;

    private ServerSocket serverSocket;
    private Socket androidAppSocket;
    private Socket droneServerSocket;

    private DataInputStream droneServerInput;
    private DataOutputStream droneServerOutput;
    private DataInputStream androidAppInput;
    private DataOutputStream androidAppOutput;

    public AppServer(ServerSocket serverSocket, ExecutorService executorService) {
        this.serverSocket = serverSocket;
        this.executorService = executorService;
    }
    @Override
    public void run(){
        if(droneServerSocket == null) {
            connectWithDroneServer();
        }
        waitForConnectionWithApp();
        executorService.execute(new DataPipe(droneServerInput, androidAppOutput, this));
        executorService.execute(new DataPipe(androidAppInput, droneServerOutput, this));
    }

    private void waitForConnectionWithApp() {
        try {
            androidAppSocket = serverSocket.accept();
            androidAppInput = new DataInputStream(androidAppSocket.getInputStream());
            androidAppOutput = new DataOutputStream(androidAppSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectWithDroneServer() {
        try {
                //todo narazie na sztywno
                droneServerSocket = new Socket("localhost", 6666);
                droneServerInput = new DataInputStream((droneServerSocket.getInputStream()));
                droneServerOutput = new DataOutputStream(droneServerSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
        }
    }

    public void restoreConnection() {
        System.out.println("Proboje ponowic");
        droneServerSocket = null;
        run();
    }

    //todo
    private void restoreConnectionWithDevice() {

    }

    //todo
    private void restoreConnectionWithServer() {

    }
}
