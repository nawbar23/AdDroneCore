

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by emergency on 2016-12-21.
 */
public class AppServer implements Runnable {
    private Socket socket;
    private ServerSocket serverSocket;

    public AppServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }
    @Override
    public void run(){
            try{
                Socket appSocket = new Socket("localhost", 6666);
                socket = serverSocket.accept();

                DataInputStream droneInput = new DataInputStream((appSocket.getInputStream()));
                DataOutputStream droneOutput = new DataOutputStream(appSocket.getOutputStream());

                DataInputStream serverInput = new DataInputStream(socket.getInputStream());
                DataOutputStream serverOutput = new DataOutputStream(socket.getOutputStream());

                while(true) {
                    int temp = serverInput.read();
                    System.out.println(socket.getLocalPort() + " Czytam z apki: " + temp);

                    droneOutput.write(temp);
                    System.out.println(socket.getLocalPort() + " Wysyłam do DroneServer: " + temp);

                    int temp1 = droneInput.read();
                    System.out.println(socket.getLocalPort() + " Odbieram od DroneServer: " + temp1);

                    serverOutput.write(temp1);
                    System.out.println(socket.getLocalPort() + " Wysyłam do apki: " + temp1);
                }

            }catch(Exception e){
                e.printStackTrace();
            }
    }
}
