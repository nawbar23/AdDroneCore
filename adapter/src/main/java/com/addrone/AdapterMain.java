package com.addrone;

import com.multicopter.java.simulator.CommHandlerSimulator;
import com.multicopter.java.simulator.TcpPeer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ebarnaw on 2016-12-13.
 */
public class AdapterMain {

    private ExecutorService executorService;
    private TcpPeer tcpPeer;
    private CommHandlerSimulator commHandlerSimulator;

    public AdapterMain(ExecutorService executorService){
        this.executorService = executorService;
        start();
    }

    public void start(){
        while(true) {
            tcpPeer = new TcpPeer(executorService, true, this);
            commHandlerSimulator = new CommHandlerSimulator(tcpPeer);
            tcpPeer.setListener(commHandlerSimulator);
            tcpPeer.connect("", 6666);
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


    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        AdapterMain adapterMain = new AdapterMain(executorService);
    }

    public void restartTcpPeer() {
        start();
    }
}
