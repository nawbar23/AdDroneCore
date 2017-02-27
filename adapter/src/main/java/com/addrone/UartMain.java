package com.addrone;

import com.simulator.TcpPeer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ewitklu on 2017-02-07.
 */
public class UartMain {

    public static void main(String[] args) {
        String port = "COM4";
        if (args.length > 0 && args[0] != null) {
            port = args[0];
        }

        ExecutorService executorService = Executors.newCachedThreadPool();

        UartComm uart = new UartComm();
        uart.setPort(port);

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
            catch(InterruptedException e) {
                System.out.println("Thread exception, leave transmission loop");
                bridge.keepLoop = false;
            }
        }
    }
}
