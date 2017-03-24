package com.addrone;

import com.simulator.TcpPeer;
import com.multicopter.java.UavSimulator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ebarnaw on 2016-12-13.
 */
public class AdapterMain {

    private ExecutorService executorService;
    private TcpPeer tcpPeer;
    private UavSimulator uavSimulator;

    public AdapterMain(){
        start();
    }

    public void start(){
        while(true) {
            executorService = Executors.newCachedThreadPool();
            tcpPeer = new TcpPeer(executorService, true);
            uavSimulator = new UavSimulator(tcpPeer);
            tcpPeer.setListener(uavSimulator);
            tcpPeer.setIpAddress("localhost");
            tcpPeer.setPort(6666);
            tcpPeer.connect();
            executorService.shutdown();
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.out.println("Error while waiting for thread, exiting...");
                e.printStackTrace();
                break;
            }
            System.out.println("\n\n");
        }
    }

    public static void main(String[] args) {
        AdapterMain adapterMain = new AdapterMain();
    }
}