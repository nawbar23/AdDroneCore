package com.addrone;

import com.multicopter.java.CommDispatcher;
import com.multicopter.java.CommMessage;
import com.multicopter.java.data.DebugData;
import com.multicopter.java.data.SignalData;
import com.multicopter.java.events.CommEvent;
import com.multicopter.java.events.MessageEvent;

import java.util.concurrent.ExecutorService;

/**
 * Created by efrytom on 2017-02-08.
 */
public class Parser implements Runnable, CommDispatcher.CommDispatcherListener {
    private CommDispatcher dispatcher;
    private byte[] byteArray;
    private int len;

    public Parser(byte[] byteArray, int len) {
        dispatcher = new CommDispatcher(this);
        this.byteArray = byteArray;
        this.len = len;
    }

    @Override
    public void run() {
        dispatcher.proceedReceiving(byteArray,len);
    }

    @Override
    public void handleCommEvent(CommEvent event) {
        if(event.getType().equals(CommEvent.EventType.MESSAGE_RECEIVED)){
            try {
                CommMessage message = ((MessageEvent) event).getMessage();
                DebugData debugData = new DebugData(message);
                System.out.println("PARSER1111111111 !!!!!!!!!!!!!!!");
                System.out.println(debugData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(event.getType().equals(CommEvent.EventType.SIGNAL_PAYLOAD_RECEIVED)){
            System.out.println("PARSER22222222222 1!!!!!!!!!!!!!!!!!");
        }
        else{
            System.out.println("PARSER33333333 1!!!!!!!!!!!!!!!!!!!");
        }
    }
}
