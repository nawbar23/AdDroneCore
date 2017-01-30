package com.addrone;

import com.multicopter.java.CommDispatcher;
import com.multicopter.java.CommInterface;
import com.multicopter.java.CommMessage;
import com.multicopter.java.data.SignalData;
import com.multicopter.java.events.CommEvent;
import jssc.*;
import java.io.IOException;

public class UartComm extends CommInterface implements CommInterface.CommInterfaceListener {

    private SerialPort serialPort;
    private CommDispatcher commDispatcher;

    public UartComm()
    {
        commDispatcher = new CommDispatcher(new CommDispatcher.CommDispatcherListener() {
            @Override
            public void handleCommEvent(CommEvent event) {
                System.out.println(event.toString());
            }
        });
    }

    @Override
    public void connect(String ipAddress, int port) {

        try
        {
            serialPort = new SerialPort("COM" + Integer.toString(port));
            serialPort.openPort();
            serialPort.setParams(115200,8,1,0);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
            serialPort.addEventListener(new SerialPortEventListener() {
                @Override
                public void serialEvent(SerialPortEvent serialPortEvent) {
                    try {
                        onDataReceived(serialPort.readBytes());
                    }
                    catch (SerialPortException e)
                    {
                        System.out.println("Read problem");
                    }
                }
            });

        }
        catch (SerialPortException e)
        {
            System.out.print("Cannot open port " + serialPort.getPortName() +"\n");
        }
    }

    @Override
    public void disconnect() {
        try
        {
            serialPort.closePort();
        }
        catch(SerialPortException e)
        {
            System.out.println("Cannot close port" + serialPort.getPortName());
        }
    }

    @Override
    public void send(byte[] data) {
        try
        {
            serialPort.writeBytes(data);
        }
        catch (SerialPortException e)
        {
            System.out.println("Send error");
        }
    }

    @Override
    public void onConnected() {
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onError(IOException e) {
    }

    @Override
    public void onDataReceived(byte[] data) {
        try
        {
            System.out.println(CommMessage.byteArrayToHexString(data));
            commDispatcher.proceedReceiving(data);
        }
        catch(Exception e)
        {
            System.out.println("Receive problem");
        }
    }

    private void sendToBoard (CommMessage message)
    {
        send(message.getByteArray());
    }

    public static void main(String[] args) {
        UartComm uartComm = new UartComm();
        uartComm.connect("",5);
        uartComm.sendToBoard(new SignalData(SignalData.Command.START_CMD, SignalData.Parameter.START).getMessage());
    }
}