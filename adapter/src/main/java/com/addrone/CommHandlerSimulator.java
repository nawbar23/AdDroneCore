package com.addrone;

import com.multicopter.java.CommDispatcher;
import com.multicopter.java.CommInterface;
import com.multicopter.java.CommMessage;
import com.multicopter.java.CommTask;
import com.multicopter.java.data.CalibrationSettings;
import com.multicopter.java.data.SignalData;
import com.multicopter.java.events.CommEvent;
import com.multicopter.java.events.MessageEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ebarnaw on 2017-01-03.
 */
public class CommHandlerSimulator implements CommInterface.CommInterfaceListener,
        CommDispatcher.CommDispatcherListener {

    private CommInterface commInterface;
    private CommDispatcher dispatcher;

    private List<CommTask> runningTasks;

    private State state;

    private enum State {
        IDLE,
        CONNECTING_APP_LOOP,
        APP_LOOP,
        FLIGHT_LOOP
    }

    public CommHandlerSimulator(CommInterface commInterface) {
        this.commInterface = commInterface;
        this.dispatcher = new CommDispatcher(this);
        this.runningTasks = new ArrayList<>();

        this.state = State.IDLE;
    }

    @Override
    public void handleCommEvent(CommEvent event) {
        try {
            System.out.println("CommHandlerSimulator : handling event : " + event.toString() + " @ " + state.toString());
            switch (state) {
                case CONNECTING_APP_LOOP:
                    handleEventConnectingAppLoop(event);
                    break;
                case APP_LOOP:
                    handleEventAppLoop(event);
                    break;
                case FLIGHT_LOOP:
                    handleEventFlightLoop(event);
                    break;
                default:
                    System.out.println("Error state!");
            }
        } catch (Exception e) {
            System.out.println("Error while handling event! " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected() {
        System.out.println("CommHandlerSimulator : onConnected");
        dispatcher.reset();
        state = State.CONNECTING_APP_LOOP;
        connectionStage = ConnectionStage.INITIAL_COMMAND;
    }

    @Override
    public void onDisconnected() {
        System.out.println("CommHandlerSimulator : onDisconnected");
    }

    @Override
    public void onError(IOException e) {
        System.out.println("CommHandlerSimulator : onError : " + e.getMessage());
    }

    @Override
    public void onDataReceived(byte[] data) {
        dispatcher.proceedReceiving(data);
    }

    public enum ConnectionStage {
        INITIAL_COMMAND,
        CALIBRATION_ACK,
        FINAL_COMMAND
    }

    private ConnectionStage connectionStage;

    private void handleEventConnectingAppLoop(CommEvent event) throws Exception {
        System.out.println("Connecting app loop @ " + connectionStage.toString());
        switch (connectionStage) {
            case INITIAL_COMMAND:
                if (event.matchSignalData(
                        new SignalData(SignalData.Command.START_CMD, SignalData.Parameter.START))) {
                    System.out.println("Start command received, staring calibration procedure");
                    send(new SignalData(SignalData.Command.START_CMD, SignalData.Parameter.ACK).getMessage());

                    // simulate calibration process (sleep 0.5s)
                    Thread.sleep(500);
                    send(new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.READY).getMessage());
                    sendCalibrationSettings(new CalibrationSettings());
                    connectionStage = ConnectionStage.CALIBRATION_ACK;
                }
                break;

            case CALIBRATION_ACK:
                if (event.matchSignalData(
                        new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.ACK))) {
                    connectionStage = ConnectionStage.FINAL_COMMAND;
                    System.out.println("Calibration procedure done successfully, waiting for final command");
                }
                break;

            case FINAL_COMMAND:
                if (event.matchSignalData(
                        new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.START))) {
                    send(new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.ACK).getMessage());
                    state = State.APP_LOOP;
                    // TODO start comm synchronous task with 20Hz interval for DebugData sending
                    // TODO it has to be added to runningTasks list (class field)
                    System.out.println("App loop started");
                }
                break;
        }
    }

    private void handleEventAppLoop(CommEvent event) throws Exception {
        if (event.getType() == CommEvent.EventType.MESSAGE_RECEIVED){
            CommMessage msg = ((MessageEvent)event).getMessage();

            if (msg.getType() == CommMessage.MessageType.SIGNAL) {
                SignalData signalMsg = new SignalData(msg);
                if (signalMsg.getCommand() == SignalData.Command.PING_VALUE) {
                    System.out.println("Ping message received, responding with pong");
                    send(new SignalData(SignalData.Command.PING_VALUE, signalMsg.getParameterValue()).getMessage());
                } else if (event.matchSignalData(new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.BREAK))) {
                    System.out.println("Disconnect message received, leaving app loop and disconnecting");
                    send(new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.BREAK_ACK).getMessage());
                    commInterface.disconnect();
                } else if (event.matchSignalData(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.START))) {
                    System.out.println("Starting flight loop!");
                    // TODO handle flight loop starting procedure
                }
                // TODO here handle rest of messages that can start actions (flight loop, calibrations...)
                // TODO for example any action starts with SignalData with command - action name and parameter START
                // TODO event.matchSignalData(new SignalData(SignalData.Command.???ACTION???, SignalData.Parameter.START)
                // TODO example of handling FLIGHT_LOOP start above
            }
        }
    }

    private void handleEventFlightLoop(CommEvent event) throws Exception {

    }

    private void send (CommMessage message) {
        commInterface.send(message.getByteArray());
    }

    private void sendCalibrationSettings(CalibrationSettings calibrationSettings) {
        ArrayList<CommMessage> messages = calibrationSettings.getMessages();
        for (CommMessage message : messages) {
            send(message);
        }
    }
}
