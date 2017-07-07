package com.addrone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

/**
 * Created by nawba on 07.07.2017.
 */
public class ServerClient implements Runnable {

    private Socket socket;
    private ServerBridge server;

    private final UUID id = UUID.randomUUID();

    private InputStream input;
    private OutputStream output;

    public ServerClient(Socket socket, ServerBridge server) {
        this.socket = socket;
        this.server = server;
    }

    public void initialize() throws IOException {
        input = socket.getInputStream();
        output = socket.getOutputStream();
    }

    public void send(byte b[], int off, int len) throws IOException {
        output.write(b, off, len);
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UUID getId() {
        return id;
    }

    @Override
    public void run() {
        System.out.println("Running ServerClient forwarding thread");

        byte[] bytes = new byte[1024];
        while(true) {
            try {
                int len = input.available();
                if (len > 1024) len = 1024;
                if(len > 0) {
                    int read = input.read(bytes, 0, len);
                    server.broadcast(bytes, 0, read, id);
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        System.out.println("Ending ServerClient forwarding thread");
    }
}
