package com.addrone;

import com.skydive.java.CommDispatcher;
import com.skydive.java.CommMessage;
import com.skydive.java.data.DebugData;
import com.skydive.java.events.CommEvent;
import com.skydive.java.events.MessageEvent;
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
        //session = ServerMain.sessionFactory.openSession();
        this.dataStream = dataStream;
        dispatcher = new CommDispatcher(this);
    }

    @Override
    public void run() {
        byteArray = dataStream.sendToParser();
        len = byteArray.length;
        dispatcher.proceedReceiving(byteArray,len);
    }

    @Override
    public void handleCommEvent(CommEvent event) {
        if(event.getType().equals(CommEvent.EventType.MESSAGE_RECEIVED)){
            try {
                CommMessage message = ((MessageEvent) event).getMessage();
                DebugData debugData = new DebugData(message);
                logger.info(debugData.toString());
                // todo: create mapping
//                Transaction transaction = session.beginTransaction();
//                session.save(debugData);
//                transaction.commit();
//                session.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(event.getType().equals(CommEvent.EventType.SIGNAL_PAYLOAD_RECEIVED)){
            logger.log(Level.WARNING, "Error");
        }
        else{
            logger.log(Level.WARNING, "Error 2");
        }
    }
}
