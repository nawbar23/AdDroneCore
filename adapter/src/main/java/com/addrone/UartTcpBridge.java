package com.addrone;

import com.multicopter.java.CommInterface;

import java.io.IOException;

public class UartTcpBridge {

    boolean keepLoop = false;

    CommInterface uartInterface;
    CommInterface tcpInteface;

    CommInterface.CommInterfaceListener uartListener;
    CommInterface.CommInterfaceListener tcpListener;

    public UartTcpBridge(CommInterface uartInterface, CommInterface tcpInterface) {

        this.uartInterface = uartInterface;
        this.tcpInteface = tcpInterface;

        uartListener = new CommInterface.CommInterfaceListener() {

            @Override
            public void onConnected() {
                System.out.println("Uart: onConnected");
                System.out.println("Connected to board, serial port open\n");
                keepLoop = true;
                tcpInterface.connect();
            }

            @Override
            public void onDisconnected() {
                System.out.println("Uart: onDisconnected");
                keepLoop = false;
            }

            @Override
            public void onError(IOException e) {
                System.out.println("Uart: onError");
                uartInterface.disconnect();
                System.out.println("Unidentified error\n");
            }

            @Override
            public void onDataReceived(byte[] data, int dataSize) {
                tcpInterface.send(data);
            }

        };
        uartInterface.setListener(uartListener);

        tcpListener = new CommInterface.CommInterfaceListener() {

            @Override
            public void onConnected() {
                System.out.println("TCP: onConnected");
                System.out.println("TCP connected\n");
            }

            @Override
            public void onDisconnected()
            {
                System.out.println("TCP: onDisconnected");
                uartInterface.disconnect();
                System.out.println("Serial port disconnected\n");
            }

            @Override
            public void onError(IOException e) {
                System.out.println("TCP: onError");
                tcpInterface.disconnect();
                System.out.println("TCP disconnected");
            }

            @Override
            public void onDataReceived(byte[] data, int dataSize) {
                uartInterface.send(data);
            }
        };

        tcpInterface.setListener(tcpListener);
    }

    public CommInterface getUartInterface() {
        return uartInterface;
    }
    public CommInterface getTcpInteface() {
        return tcpInteface;
    }
}
