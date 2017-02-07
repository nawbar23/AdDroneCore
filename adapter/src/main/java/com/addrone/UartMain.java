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
                uavManager.disconnectApplicationLoop();
            }
            else if (event.getType() == UavEvent.Type.ERROR) {
                System.out.println("ERROR");
                keepUartLoop = false;
                uavManager.disconnectApplicationLoop();
            }
            //TODO Rest of states
        }
    }

    public static void main(String[] args) {

        UartComm uartComm = new UartComm();
        uartComm.uavManager = new UavManager(uartComm, 20.0f, 1.0f);
        uartComm.setListener(uartComm.uavManager.getCommHandler());

        ListenerForUavManager listenerForUavManager = new ListenerForUavManager();

        uartComm.connect("", 5);
        System.out.println(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.ACK).getMessage().toString());

        while(listenerForUavManager.keepUartLoop)
        try {
            Thread.sleep(100);
            listenerForUavManager.handleUavEvent(uartComm.event,uartComm.uavManager);
        }
        catch (Exception e) {
            System.out.println("Something was wrong");
        }
        uartComm.disconnect();
    }
}
