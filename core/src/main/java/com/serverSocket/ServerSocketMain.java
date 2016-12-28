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
                Thread.sleep(1000);
                ServerSocket serverSocket = new ServerSocket(port);

                System.out.println("Działam na porcie " + port);

                if(port == 7777){
                    executorService.execute(new AppServer(serverSocket));
                }
                else{
                    executorService.execute(new DroneServer(serverSocket));
                }
            }catch(Exception e){
                e.printStackTrace();
            }
    }
}
