package com.addrone;

import com.multicopter.java.CommMessage;
import com.simulator.TcpPeer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ewitklu on 2017-02-07.
 */
public class UartMain {

    public static void main(String[] args) {

//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            public void run() {
//                System.out.println("Running Shutdown Hook");
//            }
//        });

        ExecutorService executorService = Executors.newCachedThreadPool();

        UartComm uart = new UartComm();
        uart.setPort("COM5");

        TcpPeer tcpPeer = new TcpPeer(executorService, false);
        tcpPeer.setIpAddress("localhost");
        tcpPeer.setPort(6666);

        UartTcpBridge bridge = new UartTcpBridge(uart, tcpPeer);

        uart.connect();

        while (bridge.keepLoop == false) {
            try
            {
                Thread.sleep(100);
            }
            catch(InterruptedException e)
            {
                System.out.println("Thread exception, leave transmission loop");
                bridge.keepLoop = false;
            }
        }
    }
}
