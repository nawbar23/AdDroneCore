package com.addrone;

import com.addrone.test.Test2;
import com.multicopter.java.UavEvent;

/**
 * Created by ebarnaw on 2016-12-13.
 */
public class ServerMain {
    public static void main(String[] args) {
        System.out.println("asdasdas asda sda");
        Test.test();
        Test2.test();

        UavEvent event = new UavEvent(UavEvent.Type.CONNECTED);
    }
}
