package com.addrone;

import com.multicopter.java.simulator.CommHandlerSimulator;
import com.multicopter.java.simulator.TcpPeer;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ebarnaw on 2016-12-13.
 */


public class ServerMain {

    private ExecutorService executorService;
    private Server server;

    public void restartTcpPeer() {
        startAdapter();
    }

    private enum ServerMode {
        BRIDGE,ADAPTER
    }

    public ServerMain(ServerMode serverMode, ExecutorService executorService){
        this.executorService = executorService;
        server = new Server(6666, executorService);
        if(serverMode == ServerMode.ADAPTER){
            startAdapter();
        }
        else if(serverMode == ServerMode.BRIDGE){
            startServer();
        }

    }

    private void startAdapter() {
            executorService.execute(server);
            TcpPeer tcpPeer = new TcpPeer(executorService, this);
            CommHandlerSimulator commHandlerSimulator = new CommHandlerSimulator(tcpPeer);
            tcpPeer.setListener(commHandlerSimulator);
            tcpPeer.connect("", 6666);
        }

    private void startServer() {
        executorService.execute(server);
}

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(15);
        ServerMain serverMain = new ServerMain(ServerMode.ADAPTER,executorService);
    }
    //TODO Zmienic wszystko an jeden server socket.

}
