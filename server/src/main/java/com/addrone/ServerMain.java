package com.addrone;

import com.serverSocket.ServerSocketMain;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ebarnaw on 2016-12-13.
 */
public class ServerMain {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(new ServerSocketMain(7777, executorService));
        executorService.execute(new ServerSocketMain(6666, executorService));
    }
}
