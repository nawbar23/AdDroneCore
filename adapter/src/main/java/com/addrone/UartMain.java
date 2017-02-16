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

        UartTcpBridge bridge = new UartTcpBridge(new UartComm(), new TcpPeer(executorService, false));

        bridge.getUartInterface().connect("lokalhost", 5);

        while (bridge.keepLoop == true) {
            //keepLoop changes to state "false" if board is disconnected from serial port
        }
    }
}
