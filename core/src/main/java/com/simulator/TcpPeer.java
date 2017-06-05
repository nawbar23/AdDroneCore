package com.simulator;

import com.skydive.java.CommInterface;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * Created by ebarnaw on 2017-01-03.
 */
public class TcpPeer extends CommInterface implements Runnable  {

    private ExecutorService executorService;

    private Socket socket;
    private ServerSocket serverSocket;

    private DataOutputStream outputStream;

    private String ipAddress;
    private int port;

    private State state;

    private boolean serverMode;

    public TcpPeer(ExecutorService executorService, boolean serverMode){
        this.executorService = executorService;
        this.state = State.DISCONNECTED;
        this.serverMode = serverMode;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private enum State {
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    }

    @Override
    public void connect() {
        System.out.println("TcpPeer:Connecting to "
                + ipAddress + ":" + String.valueOf(port) + " as "
                + (serverMode ? "server" : "client"));
        state = State.CONNECTING;
        executorService.execute(this);
    }

    @Override
    public void disconnect()  {
            state = State.DISCONNECTING;
    }

    @Override
    public void send(byte[] data) {
        try {
            //System.out.println("Sending: 0x" + CommMessage.byteArrayToHexString(data));
            outputStream.write(data, 0, data.length);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Error while sending: " + e.getMessage());
            disconnect();
        }
    }

    @Override
    public void run() {
        DataInputStream inputStream = null;
        try {
            if (serverMode) {
                serverSocket = new ServerSocket(port);
                System.out.println("Server started, waiting for connection");
                socket = serverSocket.accept();
            } else {
                socket = new Socket(ipAddress, port);
            }

            outputStream = new DataOutputStream(socket.getOutputStream());
            inputStream = new DataInputStream(socket.getInputStream());

            System.out.println("Client connected, running receiving loop!");
            state = State.CONNECTED;
            listener.onConnected();

            byte buffer[] = new byte[1024];
            while (state != State.DISCONNECTING) {
                int len = inputStream.available();
                if (len > 1024) len = 1024;
                if(len > 0) {
                    int dataSize = inputStream.read(buffer, 0, len);
                    listener.onDataReceived(buffer, dataSize);
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
        catch(IOException e){
            e.printStackTrace();
            listener.onError(e);
        }

        if (inputStream != null) {
            try {
                System.out.println("Closing stream");
                byte[] bytes = new byte[1024];
                Arrays.fill(bytes, (byte) 1);
                outputStream.write(bytes);
                inputStream.close();
                outputStream.close();
                socket.close();
                if (socket.isClosed()){
                    System.out.println("Socket closed");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
        state = State.DISCONNECTED;
        listener.onDisconnected();
    }
}
