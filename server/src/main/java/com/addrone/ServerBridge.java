package com.addrone;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by nawba on 07.07.2017.
 */
public class ServerBridge {

    private static final int SERVER_PORT = 9999;

    private ExecutorService executorService;

    private ReentrantLock reentrantLock;
    private List<ServerClient> clients;

    public ServerBridge() {
        executorService = Executors.newCachedThreadPool();
        reentrantLock = new ReentrantLock();
        clients = Collections.synchronizedList(new ArrayList<>());
    }

    public void run() throws IOException {
        System.out.println("ServerBridge:start on port: " + String.valueOf(SERVER_PORT));
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                ServerClient client = new ServerClient(socket, this);
                client.initialize();
                clients.add(client);
                System.out.println("New client connected from: " + socket.getInetAddress().toString());
                executorService.execute(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void broadcast(byte b[], int off, int len) {
        System.out.println("ServerBridge:broadcast size: " + String.valueOf(len) +
                " to " + String.valueOf(clients.size()) + " clients");
        for (ServerClient c : clients) {
            System.out.println("aaa");
            try {
                c.send(b, off, len);
            } catch (IOException e) {
                clients.remove(c);
                c.disconnect();
                System.out.println("Failed broadcast data to one of clients, shouting down connection to this guy");
            }
        }
    }
}
