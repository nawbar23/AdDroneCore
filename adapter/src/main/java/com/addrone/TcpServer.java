package com.addrone;

import com.multicopter.java.CommDispatcher;
import com.multicopter.java.CommInterface;
import com.multicopter.java.CommMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * Created by ebarnaw on 2017-01-03.
 */
public class TcpServer extends CommInterface implements Runnable  {

    private ExecutorService executorService;

    private Socket socket;
    private ServerSocket serverSocket;

    private DataOutputStream outputStream;

    private int port;

    private State state;

    public TcpServer(ExecutorService executorService) {
        super();
        this.executorService = executorService;
        this.state = State.DISCONNECTED;
    }

    private enum State {
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    }

    @Override
    public void connect(String ipAddress, int port) {
//        if (state != State.DISCONNECTED) {
//            throw new Exception("Can not connect when already in working state!");
//        }
        this.port = port;
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
            System.out.println("Sending: 0x" + CommMessage.byteArrayToHexString(data));
            outputStream.write(data, 0, data.length);
        } catch (IOException e) {
            System.out.println("Error while sending: " + e.getMessage());
            disconnect();
        }
    }

    @Override
    public void run() {
        DataInputStream inputStream = null;
        try {
            serverSocket = new ServerSocket(port);

            System.out.println("Server started, waiting for connection");
            socket = serverSocket.accept();

            outputStream = new DataOutputStream(socket.getOutputStream());
            inputStream = new DataInputStream(socket.getInputStream());

            System.out.println("Client connected, running receiving loop!");
            state = State.CONNECTED;
            listener.onConnected();

            while(state != State.DISCONNECTING) {
                byte buffer[] = new byte[1024];
                int dataSize = inputStream.read(buffer, 0, buffer.length);
                if (dataSize != -1) {
                    byte[] tempArray = new byte[dataSize];
                    System.arraycopy(buffer, 0, tempArray, 0, dataSize);
                    listener.onDataReceived(tempArray);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            listener.onError(e);
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

        state = State.DISCONNECTED;
        listener.onDisconnected();
    }
}
