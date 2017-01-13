package com.serverSocket;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;

/**
 * Created by emergency on 2016-12-21.
 */
public class ServerSocketMain implements Runnable {
    private final int port;
    private ExecutorService executorService;

    public ServerSocketMain(int port, ExecutorService executorService) {
        this.port = port;
        this.executorService = executorService;
    }

    @Override
    public void run(){
            try{
                ServerSocket serverSocket = new ServerSocket(port);
                if(port == 7777){
                    executorService.execute(new AppServer(serverSocket, executorService));
                }
                else{
                    executorService.execute(new DroneServer(serverSocket, executorService));
                }
            }catch(Exception e){
                e.printStackTrace();
            }
    }
}
