package com.addrone;

import com.multicopter.java.UavEvent;
import com.multicopter.java.UavManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by ewitklu on 2017-02-07.
 */
public class UartMain {

    public static void main(String[] args) {

        boolean keepUartLoop = true;
        UartComm uartComm = new UartComm();

        Socket socket;
        DataOutputStream socketOutput;
        DataInputStream socketInput;

        uartComm.connect("", 5);

        if (uartComm.serialPort.isOpened()) {
            while (keepUartLoop == true) {
            }
        }
    }
}
