package com.serverSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by emergency on 2017-01-13.
 */
public class DataPipe implements Runnable {

    private Server server;
    private DataInputStream input;
    private DataOutputStream output;
    //todo zmienic na server
    private AppServer appServer;
    //todo
    private DroneServer droneServer;

    public DataPipe( DataInputStream input, DataOutputStream output, AppServer appServer) {
        this.input = input;
        this.output = output;
        this.appServer = appServer;
    }

    //todo
    public DataPipe(DataInputStream input, DataOutputStream output, DroneServer droneServer) {
        this.input = input;
        this.output = output;
        this.droneServer = droneServer;
    }

    public DataPipe(DataInputStream input, DataOutputStream output, Server server) {
        this.input = input;
        this.output = output;
        this.server = server;
    }

    @Override
    public void run() {
        byte[] temp = new byte[128];
        while(true) {
            try {
                input.read(temp);
                output.write(temp);
            } catch (IOException e) {
                e.printStackTrace();
                if(droneServer == null)
                    appServer.restoreConnection();
                else{droneServer.restoreConnection();}
            }
        }
    }
}
