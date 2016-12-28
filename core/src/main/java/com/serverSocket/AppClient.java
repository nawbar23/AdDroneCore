import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by emergency on 2016-12-21.
 */
public class AppClient {
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
            try{
                Socket socket = new Socket("localhost", 7777);

                DataInputStream dis = new DataInputStream((socket.getInputStream()));
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                int b;
                while(true){
                    b = sc.nextInt();
                    dos.write(b);
                    System.out.println(dis.read());
                }
//                dos.write(12);
//                System.out.println("Wysyłam: " + 12);
//                int temp = dis.read();
//                System.out.println("Odbieram: " + temp);

            }catch(Exception e){
                e.printStackTrace();
            }
    }
}
