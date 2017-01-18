package com.serverSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by emergency on 2017-01-18.
 */
public class Test {
    public static void main(String[] args) {
        try {
            Socket socket1 = new Socket("localhost", 6666);
            Socket socket2 = new Socket("localhost", 7777);
            DataInputStream input1 = new DataInputStream(socket1.getInputStream());
            DataOutputStream output1 = new DataOutputStream((socket1.getOutputStream()));
            DataInputStream input2 = new DataInputStream(socket2.getInputStream());
            DataOutputStream output2 = new DataOutputStream((socket2.getOutputStream()));

            byte[] temp1 = new byte[128];
            byte[] temp2 = new byte[128];
            while(true){
                output1.write(temp1);
                output2.write(temp2);
                input1.read(temp1);
                input2.read(temp2);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
