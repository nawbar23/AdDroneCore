package com.addrone;

import com.skydive.java.CommDispatcher;
import com.skydive.java.events.CommEvent;



/**
 * Created by efrytom on 2017-02-08.
 */
public class AppDataParser implements CommDispatcher.CommDispatcherListener, Runnable {

    private CommDispatcher dispatcher;

    public AppDataParser(){
        dispatcher = new CommDispatcher(this);
    }

    @Override
    public void handleCommEvent(CommEvent event) {

        //parsers logic.
    }

    @Override
    public void run() {

    }
}
