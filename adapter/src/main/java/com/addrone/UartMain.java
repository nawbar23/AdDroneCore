package com.addrone;

import com.simulator.TcpPeer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ewitklu on 2017-02-07.
 */
public class UartMain {

    public static void main(String[] args) {

        ExecutorService executorService = Executors.newCachedThreadPool();

        UartComm uart = new UartComm();
        uart.setPort("COM4");

        TcpPeer tcpPeer = new TcpPeer(executorService, false);
        tcpPeer.setIpAddress("localhost");
        tcpPeer.setPort(6666);

        UartTcpBridge bridge = new UartTcpBridge(uart, tcpPeer);

        uart.connect();


        while (bridge.keepLoop == true) {
            //keepLoop changes to state "false" if board is disconnected from serial port
        }
    }
}
