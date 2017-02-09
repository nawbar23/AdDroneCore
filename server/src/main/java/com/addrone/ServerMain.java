package com.addrone;


import com.simulator.CommHandlerSimulator;
import com.simulator.TcpPeer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ebarnaw on 2016-12-13.
 */


public class ServerMain {

    private ExecutorService executorService;
    private Server server;


    private enum ServerMode {
        BRIDGE,ADAPTER
    }

    public ServerMain(ServerMode serverMode){
        if(serverMode == ServerMode.ADAPTER){
            startAdapter();
        }
        else if(serverMode == ServerMode.BRIDGE){
            startServer();
        }

    }

    private void startAdapter() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(6666);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            executorService = Executors.newCachedThreadPool();
            server = new Server(6666, executorService, serverSocket);
            executorService.execute(server);
            TcpPeer tcpPeer = new TcpPeer(executorService);
            CommHandlerSimulator commHandlerSimulator = new CommHandlerSimulator(tcpPeer);
            tcpPeer.setListener(commHandlerSimulator);
            tcpPeer.connect("", 6666);
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                System.out.println(executorService.toString());
            } catch (InterruptedException e) {
                System.out.println("Error while waiting for thread, exiting...");
                e.printStackTrace();
                break;
            }
        }
    }

    private void startServer() {
        executorService.execute(server);
}

    public static void main(String[] args) {
        ServerMain serverMain = new ServerMain(ServerMode.ADAPTER);
    }

}
