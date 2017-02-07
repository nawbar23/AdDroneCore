package com.addrone;

import com.multicopter.java.*;
import com.multicopter.java.data.CalibrationSettings;
import com.multicopter.java.data.SignalData;
import com.multicopter.java.events.CommEvent;
import jssc.*;
import java.io.IOException;
import java.io.InterruptedIOException;

public class UartComm extends CommInterface {

    protected UavManager uavManager;
    protected UavEvent event;
    protected SerialPort serialPort;

    @Override
    public void connect(String ipAddress, int port) {
        try {
            serialPort = new SerialPort("COM" + Integer.toString(port));
            serialPort.openPort();
            serialPort.setParams(115200, 8, 1, 0);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
            listener.onConnected();
            event = new UavEvent(UavEvent.Type.CONNECTED);
        } catch (SerialPortException e) {
            System.out.print("Cannot open port: " + serialPort.getPortName() + "- try again or change open parameters\n");
            listener.onError(new IOException(e.getMessage(), e.getCause()));
            event = new UavEvent(UavEvent.Type.ERROR);
        }
    }

    @Override
    public void disconnect() {
        try {
            serialPort.closePort();
            listener.onDisconnected();
            event = new UavEvent(UavEvent.Type.DISCONNECTED);
        } catch (SerialPortException e) {
            System.out.println("Cannot close port: " + serialPort.getPortName() + "\n");
            listener.onError(new IOException(e.getMessage(), e.getCause()));
            event = new UavEvent(UavEvent.Type.ERROR);
        }
    }

    @Override
    public void send(byte[] data) {
        System.out.println(CommMessage.byteArrayToHexString(data));
        try {
            serialPort.writeBytes(data);
        } catch (SerialPortException e) {
            System.out.println("Send error - verify your data\n");
            listener.onError(new IOException(e.getMessage(), e.getCause()));
        }
    }
}