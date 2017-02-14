package com.addrone;

import com.multicopter.java.CommInterface;
import com.simulator.TcpPeer;
import com.sun.corba.se.impl.activation.ServerMain;
import com.sun.corba.se.spi.activation.Server;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ewitklu on 2017-02-14.
 */
public class UartTcpBridge extends CommInterface implements SerialPortEventListener, CommInterface.CommInterfaceListener {

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private TcpPeer tcpPeer = new TcpPeer(executorService);
    private UartComm uartComm = new UartComm();
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    @Override
    public void connect(String ipAddress, int port) {
        tcpPeer.connect("localhost",6666);
    }

    @Override
    public void disconnect() {
        tcpPeer.disconnect();
    }

    @Override
    public void send(byte[] data) {

    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onError(IOException e) {

    }

    @Override
    public void onDataReceived(byte[] data, int dataSize) {

    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {

    }
}
