

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by emergency on 2016-12-27.
 */
public class DroneServer implements Runnable {

    private Socket socket;
    private ServerSocket serverSocket;

    public DroneServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }
    @Override
    public void run() {
            try {
                Socket droneSocket = serverSocket.accept();
                socket = serverSocket.accept();

                DataInputStream droneInput = new DataInputStream(socket.getInputStream());
                DataOutputStream droneOutput = new DataOutputStream(socket.getOutputStream());

                DataInputStream serverInput = new DataInputStream(droneSocket.getInputStream());
                DataOutputStream serverOutput = new DataOutputStream(droneSocket.getOutputStream());

                while(true){
                    int temp = serverInput.read();
                    System.out.println(socket.getLocalPort() + " Czytam z AppServer : " + temp);

                    droneOutput.write(temp);
                    System.out.println(socket.getLocalPort() + " Wysyłam do drona: " + temp);

                    int temp1 = droneInput.read();
                    System.out.println(socket.getLocalPort() + " Odbieram od drona: " + temp1);

                    serverOutput.write(temp1);
                    System.out.println(socket.getLocalPort() + " Wysyłam do AppServer: " + temp1);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}

