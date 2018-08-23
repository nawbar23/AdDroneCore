package com.addrone;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by nawba on 07.07.2017.
 */
public class ServerBridge {

    private ExecutorService executorService;
    private List<ServerClient> clients;

    public ServerBridge() {
        executorService = Executors.newCachedThreadPool();
        clients = Collections.synchronizedList(new LinkedList<>());
    }

    public void run(int port) throws IOException {
        System.out.println("ServerBridge:start on port: " + String.valueOf(port));
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                ServerClient client = new ServerClient(socket, this);
                client.initialize();
                clients.add(client);
                System.out.println("New client connected from: \"" + socket.getInetAddress().toString()
                        + "\" with id: " + client.getId().toString());
                executorService.execute(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcast(final byte b[], final int off, final int len, final UUID source) {
        System.out.println("ServerBridge:broadcast size: " + String.valueOf(len)
                + " to " + String.valueOf(clients.size()) + " clients");
        ArrayList<ServerClient> toRemove = new ArrayList<>();
        for (ServerClient c : clients) {
            if (c.getId().equals(source)) continue; // do not forward message to source itself
            try {
                c.send(b, off, len);
            } catch (IOException e) {
                System.out.println("Failed broadcast data to one of clients, shouting down connection to this guy");
                toRemove.add(c);
            }
        }
        for (ServerClient c : toRemove) {
            clients.remove(c);
            c.disconnect();
        }
    }
}
