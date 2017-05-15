package com.addrone;

import com.skydive.java.CommInterface;

import java.io.IOException;

public class UartTcpBridge {

    boolean keepLoop = true;

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
//                if(CommMessage.byteArrayToHexString(data) == CommHandlerAction.ActionType.DISCONNECT.toString())
//                    tcpInterface.disconnect();
            }
        };
        uartInterface.setListener(uartListener);

        tcpListener = new CommInterface.CommInterfaceListener() {

            @Override
            public void onConnected() {
                System.out.println("TCP Connected");
            }

            @Override
            public void onDisconnected()
            {
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
            }
        };

        tcpInterface.setListener(tcpListener);
    }
}
