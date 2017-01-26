package com.addrone;

import com.multicopter.java.simulator.CommHandlerSimulator;
import com.multicopter.java.simulator.TcpServer;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ebarnaw on 2016-12-13.
 */


public class ServerMain {

    private ExecutorService executorService;

    private enum ServerMode {
        BRIDGE,ADAPTER
    }

    public ServerMain(ServerMode serverMode, ExecutorService executorService){
        this.executorService = executorService;
        if(serverMode == ServerMode.ADAPTER){
            startAdapter();
        }
        else if(serverMode == ServerMode.BRIDGE){
            startServer();
        }

    }

    private void startAdapter() {
            TcpServer tcpServer = new TcpServer(executorService);
            CommHandlerSimulator commHandlerSimulator = new CommHandlerSimulator(tcpServer);
            executorService.execute(new Server(7777, executorService));
            executorService.execute(new Server(6666, executorService,true,7777));
            tcpServer.setListener(commHandlerSimulator);
            tcpServer.connect("", 6666);
//            try {
//                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
//            } catch (InterruptedException e) {
//                System.out.println("Error while waiting for thread, exiting...");
//                e.printStackTrace();
//                break;
//            }
        }

    private void startServer() {
        executorService.execute(new Server(7777, executorService));
        executorService.execute(new Server(6666, executorService,true,7777));
}

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(15);
        ServerMain serverMain = new ServerMain(ServerMode.ADAPTER,executorService);
    }
    //TODO obsluga bledow, parser, zastanowic sie na udp jak czas pozwoli

}
