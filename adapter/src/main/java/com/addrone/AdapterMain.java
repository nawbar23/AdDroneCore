package com.addrone;

import com.multicopter.java.simulator.CommHandlerSimulator;
import com.multicopter.java.simulator.TcpServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ebarnaw on 2016-12-13.
 */
public class AdapterMain {
    public static void main(String[] args) {
        while (true) {
            ExecutorService executorService = Executors.newCachedThreadPool();
            TcpServer tcpServer = new TcpServer(executorService, true);
            CommHandlerSimulator commHandlerSimulator = new CommHandlerSimulator(tcpServer);

            tcpServer.setListener(commHandlerSimulator);
            tcpServer.connect("", 6666);

            executorService.shutdown();
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.out.println("Error while waiting for thread, exiting...");
                e.printStackTrace();
                break;
            }
        }
    }
}
