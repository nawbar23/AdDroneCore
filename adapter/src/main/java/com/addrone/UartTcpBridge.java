package com.addrone;

import com.multicopter.java.CommInterface;
import com.simulator.TcpPeer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ewitklu on 2017-02-14.
 */
public class UartTcpBridge extends CommInterface implements CommInterface.CommInterfaceListener{

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private TcpPeer tcpPeer = new TcpPeer(executorService, false);

    @Override
    public void connect(String ipAddress, int port) {
        tcpPeer.connect("localhost", 6666);
    }

    @Override
    public void disconnect() {
        tcpPeer.disconnect();
    }

    @Override
    public void send(byte[] data) {
        tcpPeer.send(data);
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
}
