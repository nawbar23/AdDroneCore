package com.addrone;

import com.serverSocket.Server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ebarnaw on 2016-12-13.
 */
public class ServerMain {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(new Server(7777, executorService));
        executorService.execute(new Server(6666, executorService,true,7777));
    }
}
