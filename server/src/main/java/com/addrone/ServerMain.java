package com.addrone;

import com.simulator.TcpPeer;
import com.skydive.java.UavSimulator;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
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
        BRIDGE,BRIDGE_V2,ADAPTER
    }

    public ServerMain(ServerMode serverMode){
        if(serverMode == ServerMode.ADAPTER){
            startAdapter();
        }
        else if(serverMode == ServerMode.BRIDGE){
            executorService = Executors.newCachedThreadPool();
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
            System.out.println("Starting server as simulator");
            executorService = Executors.newCachedThreadPool();
            server = new Server(6666, executorService, serverSocket);
            executorService.execute(server);
            TcpPeer tcpPeer = new TcpPeer(executorService, false);
            UavSimulator commHandlerSimulator = new UavSimulator(tcpPeer);
            tcpPeer.setListener(commHandlerSimulator);
            tcpPeer.setIpAddress("localhost");
            tcpPeer.setPort(6666);
            tcpPeer.connect();
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
        try {
            System.out.println("Starting server as bridge!");
            ServerSocket serverSocket = new ServerSocket(6666);
            server = new Server(6666, executorService, serverSocket);
            executorService.execute(server);
            System.out.println(serverSocket.getInetAddress());
            System.out.println(serverSocket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final int SERVER_PORT = 9999;

    public static void main(String[] args) {

        int port = SERVER_PORT;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }

        try {
            new ServerBridge().run(port);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}