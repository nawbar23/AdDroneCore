package com.addrone;

import com.simulator.TcpPeer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ewitklu on 2017-02-07.
 */
public class UartMain {

    public static void main(String[] args) {
        String port = "COM7";
        if (args.length > 0 && args[0] != null) {
            port = args[0];
        }

        String serverAddress = "localhost";
        if (args.length > 1 && args[1] != null) {
            serverAddress = args[1];
        }

        ExecutorService executorService = Executors.newCachedThreadPool();

        while (true) {
            UartComm uart = new UartComm();
            uart.setPort(port);

            TcpPeer tcpPeer = new TcpPeer(executorService, true);
            tcpPeer.setIpAddress(serverAddress);
            tcpPeer.setPort(6666);

            UartTcpBridge bridge = new UartTcpBridge(uart, tcpPeer);

            uart.connect();


            while (bridge.keepLoop == false) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.out.println("Thread exception, leave transmission loop");
                    bridge.keepLoop = false;
                }
            }

            bridge.close();

            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            System.out.println("\nRerunning adapter\n");
        }
    }
}
