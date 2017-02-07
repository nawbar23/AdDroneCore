package com.addrone;

import com.multicopter.java.CommDispatcher;
import com.multicopter.java.CommInterface;
import com.multicopter.java.CommMessage;
import com.multicopter.java.UavManager;
import com.multicopter.java.data.CalibrationSettings;
import com.multicopter.java.data.SignalData;
import com.multicopter.java.events.CommEvent;
import jssc.*;
import java.io.IOException;
import java.io.InterruptedIOException;

public class UartComm extends CommInterface {

    private SerialPort serialPort;
    private CommDispatcher commDispatcher;
    private static UavManager uavManager;

    public UartComm() {
//        commDispatcher = new CommDispatcher(new CommDispatcher.CommDispatcherListener() {
//            @Override
//            public void handleCommEvent(CommEvent event) {
//                System.out.println(event.toString());
//            }
//        });
    }

    @Override
    public void connect(String ipAddress, int port) {

        try {
            serialPort = new SerialPort("COM" + Integer.toString(port));
            serialPort.openPort();
            serialPort.setParams(115200, 8, 1, 0);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
        } catch (SerialPortException e) {
            System.out.print("Cannot open port: " + serialPort.getPortName() + "- try again or change open parameters\n");
        }
    }

    @Override
    public void disconnect() {
        try {
            serialPort.closePort();
        } catch (SerialPortException e) {
            System.out.println("Cannot close port: " + serialPort.getPortName() + "\n");
        }
    }

    @Override
    public void send(byte[] data) {
        try {
            serialPort.writeBytes(data);
        } catch (SerialPortException e) {
            System.out.println("Send error - verify your data\n");
        }
    }

    public static void main(String[] args) {

        UartComm uartComm = new UartComm();
        uartComm.connect("", 5);

        uavManager = new UavManager(uartComm, 20.0f, 1.0f);
        uartComm.setListener(uavManager.getCommHandler());

        //uartComm.send(new SignalData(SignalData.Command.START_CMD, SignalData.Parameter.START).getMessage().getByteArray());
        while (true) {
            try {
                System.out.println(uavManager);
            } catch (Exception e) {
                System.out.println("Something was wrong");
            }
        }
    }
}