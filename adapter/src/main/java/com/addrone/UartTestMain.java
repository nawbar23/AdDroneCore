package com.addrone;

import com.multicopter.java.UavEvent;
import com.multicopter.java.UavManager;
import com.multicopter.java.data.ControlData;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ebarnaw on 2016-12-13.
 */
public class UartTestMain implements UavManager.UavManagerListener,
        UavManager.ControlDataSource {

    private static final long TIME_BEFORE_FLIGHT = 3000; // [ms]
    private static final long TIME_IN_FLIGHT = 5000; // [ms]
    private static final long TIME_BEFORE_DISCONNECT = 2000; // [ms]

    public UartTestMain(String portName){
        start(portName);
    }

    private UavManager uavManager;

    private boolean keepConnection;

    public void start(String portName){
        UartComm uart = new UartComm();
        uart.setPort(portName);

        keepConnection = true;

        uavManager = new UavManager(uart, 25.0, 2.0);
        uavManager.registerListener(this);
        uavManager.setControlDataSource(this);

        uart.connect();

        while (keepConnection) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Test done successfully!\n\n");
        System.exit(0);
    }

    @Override
    public void handleUavEvent(UavEvent event, UavManager uavManager) {
        switch (event.getType()) {
            case CONNECTED:
                startWaitForFlightTimer();
                break;

            case DISCONNECTED:
                keepConnection = false;
                break;

            case ERROR:
                keepConnection = false;
                break;

            case MESSAGE:
                System.out.println("Message: " + event.getMessage());
                break;

            case DEBUG_UPDATED:
                break;

            case CALIBRATION_UPDATED:
                break;

            case PING_UPDATED:
                break;

            case FLIGHT_STARTED:
                startFlightTimer();
                break;

            case FLIGHT_ENDED:
                startWaitForDisconnectTimer();
                break;

            default:
                System.out.println(" = = = = = Event not handled!!!");
        }
    }

    @Override
    public ControlData getControlData() {
        return new ControlData();
    }

    private void startWaitForFlightTimer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                uavManager.startFlightLoop();
            }
        }, TIME_BEFORE_FLIGHT);
    }

    private void startFlightTimer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    uavManager.endFlightLoop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, TIME_IN_FLIGHT);
    }

    private void startWaitForDisconnectTimer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                uavManager.disconnectApplicationLoop();
            }
        }, TIME_BEFORE_DISCONNECT);
    }

    public static void main(String[] args) {
        String port = "COM4";
        if (args.length > 0 && args[0] != null) {
            port = args[0];
        }
        UartTestMain uartTestMain = new UartTestMain(port);
    }
}