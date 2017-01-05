package com.addrone;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ebarnaw on 2016-12-13.
 */
public class AdapterMain {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        TcpServer tcpServer = new TcpServer(executorService);

        CommHandlerSimulator commHandlerSimulator = new CommHandlerSimulator(tcpServer);

        tcpServer.setListener(commHandlerSimulator);
        tcpServer.connect("", 6666);
    }
}
