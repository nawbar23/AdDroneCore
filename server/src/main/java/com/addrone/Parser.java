package com.addrone;

import com.multicopter.java.CommDispatcher;
import com.multicopter.java.CommMessage;
import com.multicopter.java.data.DebugData;
import com.multicopter.java.data.SignalData;
import com.multicopter.java.events.CommEvent;
import com.multicopter.java.events.MessageEvent;

import java.lang.reflect.Array;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by efrytom on 2017-02-08.
 */
public class Parser implements Runnable, CommDispatcher.CommDispatcherListener {
    private CommDispatcher dispatcher;
    private byte[] byteArray;
    private int len;
    private DataStream dataStream;
    private Logger logger = Logger.getLogger(Parser.class.getName());

    public Parser(DataStream dataStream) {
        this.dataStream = dataStream;
        dispatcher = new CommDispatcher(this);
    }

    @Override
    public void run() {
        logger.info("weszlo!");
        byteArray = dataStream.sendToParser();
        System.out.println(byteArray.toString());
        logger.info(byteArray.toString());
        len = byteArray.length;
        logger.info(Integer.toString(len));
        dispatcher.proceedReceiving(byteArray,len);
    }

    @Override
    public void handleCommEvent(CommEvent event) {
        if(event.getType().equals(CommEvent.EventType.MESSAGE_RECEIVED)){
            try {
                CommMessage message = ((MessageEvent) event).getMessage();
                DebugData debugData = new DebugData(message);
                //System.out.println("PARSER1111111111 !!!!!!!!!!!!!!!");
                logger.info(debugData.toString());
                //System.out.println(debugData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(event.getType().equals(CommEvent.EventType.SIGNAL_PAYLOAD_RECEIVED)){
            //System.out.println("PARSER22222222222 1!!!!!!!!!!!!!!!!!");
            logger.log(Level.WARNING, "Error");
        }
        else{
           // System.out.println("PARSER33333333 1!!!!!!!!!!!!!!!!!!!");
            logger.log(Level.WARNING, "Error 2");
        }
    }
}
