package com.addrone;

import com.skydive.java.CommInterface;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class UartTcpBridge {

    boolean keepLoop = true;

    CommInterface uartInterface;
    CommInterface tapInterface;

    CommInterface.CommInterfaceListener uartListener;
    CommInterface.CommInterfaceListener tcpListener;

    Timer timer;
    boolean started = false;
    long lastReception = System.currentTimeMillis();

    public UartTcpBridge(CommInterface uartInterface, CommInterface tcpInterface) {

        this.uartInterface = uartInterface;
        this.tapInterface = tcpInterface;

        uartListener = new CommInterface.CommInterfaceListener() {
            @Override
            public void onConnected() {
                System.out.println("Connected to board, serial port opened\n");
                keepLoop = false;
                tcpInterface.connect();
            }

            @Override
            public void onDisconnected() {
                System.out.println("Disconnected from board");
                keepLoop = true;
            }

            @Override
            public void onError(IOException e) {
                System.out.println("Uart: onError");
                uartInterface.disconnect();
                System.out.println("Unidentified error\n");
            }

            @Override
            public void onDataReceived(byte[] data, int dataSize) {
                byte[] tempData = new byte[dataSize];
                System.arraycopy(data,0,tempData,0,dataSize);
                tcpInterface.send(tempData);
                lastReception = System.currentTimeMillis();
            }
        };
        uartInterface.setListener(uartListener);

        tcpListener = new CommInterface.CommInterfaceListener() {
            @Override
            public void onConnected() {
                System.out.println("TCP Connected");
            }

            @Override
            public void onDisconnected() {
                System.out.println("TCP Disconnected");
                uartInterface.disconnect();
            }

            @Override
            public void onError(IOException e) {
                System.out.println("TCP: onError");
                tcpInterface.disconnect();
            }

            @Override
            public void onDataReceived(byte[] data, int dataSize) {
                byte[] tempData = new byte[dataSize];
                System.arraycopy(data,0,tempData,0,dataSize);
                uartInterface.send(tempData);
                started = true;
                lastReception = System.currentTimeMillis();
            }
        };
        tcpInterface.setListener(tcpListener);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (started && (System.currentTimeMillis() - lastReception) > 1000) {
                    System.out.println("Timeout waiting for data reception, restarting bridge");
                    tcpInterface.disconnect();
                }
            }
        }, 1000, 500);
    }

    public void close() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
