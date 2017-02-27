package com.addrone;

import com.multicopter.java.*;
import com.multicopter.java.actions.CommHandlerAction;
import jssc.*;

import java.io.IOException;

public class UartComm extends CommInterface implements SerialPortEventListener {

    private String port;
    private SerialPort serialPort;

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public void connect() {
        try {
            serialPort = new SerialPort(port);
            serialPort.openPort();
            serialPort.setParams(230400, 8, 1, 0);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
            serialPort.addEventListener(this);
            listener.onConnected();
        } catch (SerialPortException e) {
            System.out.print("Cannot open port: " + serialPort.getPortName() + "- try attach your device again or change open parameters\n");
        }
    }

    @Override
    public void disconnect() {
        try {
            if (serialPort.isOpened()) {
                System.out.println("Serial port disconnected");
                serialPort.closePort();
            }
            listener.onDisconnected();
        } catch (SerialPortException e) {
            System.out.println("Cannot close port: " + serialPort.getPortName() + "\n");
            listener.onError(new IOException(e.getMessage(), e.getCause()));
        }
    }

    @Override
    public void send(byte[] data) {
        if(data.length!=0)
        System.out.println("Uart Sending: 0x" + CommMessage.byteArrayToHexString(data));
        try {
            serialPort.writeBytes(data);
        } catch (SerialPortException e) {
            System.out.println("Send error\n");
            listener.onError(new IOException(e.getMessage(), e.getCause()));
        }
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        try {
            if (0 < serialPort.getInputBufferBytesCount()) {
                byte[] data = serialPort.readBytes();
                listener.onDataReceived(data, data.length);
            }
        } catch (SerialPortException e) {
            System.out.println("Receive error\n");
            listener.onError(new IOException(e.getMessage(), e.getCause()));
        }
    }
}