package com.addrone;

import com.multicopter.java.UavEvent;
import com.multicopter.java.UavManager;

/**
 * Created by ewitklu on 2017-02-07.
 */
public class UartMain {

    public static class ListenerForUavManager implements UavManager.UavManagerListener {
        public boolean keepUartLoop = true;
        @Override
        public void handleUavEvent(UavEvent event, UavManager uavManager) {
            if (event.getType() == UavEvent.Type.DISCONNECTED) {
                System.out.println("DISCONNECTED");
                keepUartLoop = false;
            }
            else if (event.getType() == UavEvent.Type.ERROR) {
                System.out.println("ERROR");
                keepUartLoop = false;
            }
            else if(event.getType() == UavEvent.Type.CONNECTED)
            {
                System.out.println("CONNECTED");
            }
            else if(event.getType() == UavEvent.Type.FLIGHT_STARTED)
            {
                System.out.println("FLIGHT STARTED");
            }
            else if(event.getType() == UavEvent.Type.FLIGHT_ENDED)
            {
                System.out.println("FLIGHT ENDED");
            }
            else if(event.getType() == UavEvent.Type.MAGNETOMETER_CALIBRATION_STARTED)
            {
                System.out.println("MAGNETOMETER CALIBRATION STARTED");
            }
            else if(event.getType() == UavEvent.Type.MAGNETOMETER_CALIBRATION_STARTED)
            {
                System.out.println("MAGNETOMETER CALIBRATION STARTED");
            }
        }
    }

    public static void main(String[] args) {

        UartComm uartComm = new UartComm();
        UavManager uavManager;
        uavManager = new UavManager(uartComm, 20.0f, 1.0f);
        uartComm.setListener(uavManager.getCommHandler());

        ListenerForUavManager listenerForUavManager = new ListenerForUavManager();
        uavManager.registerListener(listenerForUavManager);

        uartComm.connect("", 5);
        if (uartComm.serialPort.isOpened()) {
            while (listenerForUavManager.keepUartLoop == true) {
            }
            if (uartComm.event.getType() == UavEvent.Type.CONNECTED)
                uartComm.disconnect();
        }
    }
}
