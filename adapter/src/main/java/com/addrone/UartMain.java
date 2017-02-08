package com.addrone;

import com.multicopter.java.UavEvent;
import com.multicopter.java.UavManager;
import com.multicopter.java.data.SignalData;


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

     while (listenerForUavManager.keepUartLoop){
            try {
                Thread.sleep(5000);
                uavManager.disconnectApplicationLoop();
            } catch (Exception e) {
                System.out.println("Something was wrong");
            }
        }
        uartComm.disconnect();
    }
}
