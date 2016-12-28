package com.serverSocket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by emergency on 2016-12-21.
 */
public class Main {
    public static void main(String[] args){
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(new ServerSocketMain(7777, executorService));
        executorService.execute(new ServerSocketMain(6666, executorService));
    }
}
