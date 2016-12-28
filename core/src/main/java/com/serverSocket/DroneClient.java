import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Created by emergency on 2016-12-27.
 */
public class DroneClient {
    public static void main(String[] args) {
            try {
                Socket socket = new Socket("localhost", 6666);

                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                int temp;
                while(true){
                    temp = dis.read();
                    System.out.println("ODbieram: " + temp);
                    temp++;
                    dos.write(temp);
                    System.out.println("Wysyłam: " + temp);
                }
//                int temp = dis.read();
//                System.out.println("odbieram: " + temp);
//
//                temp++;
//
//                System.out.println("wysyłam: " + temp);
//                dos.write(temp);

            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
