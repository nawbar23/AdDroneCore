package com.addrone;

import com.multicopter.java.*;
import com.multicopter.java.actions.CommHandlerAction;
import com.multicopter.java.actions.FlightLoopAction;
import com.multicopter.java.data.CalibrationSettings;
import com.multicopter.java.data.ControlData;
import com.multicopter.java.data.DebugData;
import com.multicopter.java.data.SignalData;
import com.multicopter.java.events.CommEvent;
import com.multicopter.java.events.MessageEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ebarnaw on 2017-01-03.
 */
public class CommHandlerSimulator implements CommInterface.CommInterfaceListener,
        CommDispatcher.CommDispatcherListener {

    private CommInterface commInterface;
    private CommDispatcher dispatcher;

    private List<CommTask> runningTasks;

    private State state;
    private MagnetometerState magnetometerState;
    private Flight_state flight_state;

    private DebugData debugDataToSend = getStartDebugData();
    private Lock debugDataLock = new ReentrantLock();

    private int calibrationSettingsSendingFails;

    private enum State {
        IDLE,
        CONNECTING_APP_LOOP,
        APP_LOOP,
        FLIGHT_LOOP,
        CALIBRATE_MAGNET,
        CALIBRATE_ACCEL
    }

    private enum MagnetometerState{
        MAGNET_CALIBRATION_IDLE,
        MAGNET_CALIBRATION_SKIPPED,
        MAGNET_CALIBRATION_DONE
    }

    private enum Flight_state{
        WAITING_FOR_RUNNING,
        RUNNING
    }

    public CommHandlerSimulator(CommInterface commInterface) {
        this.commInterface = commInterface;
        this.dispatcher = new CommDispatcher(this);
        this.runningTasks = new ArrayList<>();

        this.state = State.IDLE;
        this.magnetometerState = MagnetometerState.MAGNET_CALIBRATION_IDLE;
        this.flight_state = Flight_state.WAITING_FOR_RUNNING;
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
                case CALIBRATE_MAGNET:
                    handleEventCalibrationMagnetometer(event);
                    break;
                case CALIBRATE_ACCEL:
                handleEventCalibrationAccelerometer(event);
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
                    calibrationSettingsSendingFails = 0;
                }
                break;

            case CALIBRATION_ACK:
                if (event.matchSignalData(
                        new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.ACK))) {
                    connectionStage = ConnectionStage.FINAL_COMMAND;
                    System.out.println("Calibration procedure done successfully, waiting for final command");
                } else if (event.matchSignalData(
                        new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.BAD_CRC))) {
                    System.out.println("Sending calibration failed, application reports BAD_CRC, retransmitting...");
                    calibrationSettingsSendingFails++;
                    sendCalibrationSettings(new CalibrationSettings());
                } else if (event.matchSignalData(
                        new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.TIMEOUT))) {
                    System.out.println("Sending calibration failed, application reports TIMEOUT, retransmitting...");
                    calibrationSettingsSendingFails++;
                    sendCalibrationSettings(new CalibrationSettings());
                }
                if (calibrationSettingsSendingFails >= 3) {
                    throw new Exception("Calibration settings procedure failed, max retransmission limit exceeded!");
                }
                break;

            case FINAL_COMMAND:
                if (event.matchSignalData(
                        new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.START))) {
                    send(new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.ACK).getMessage());
                    state = State.APP_LOOP;
                    // starting debug task
                    debugTask.start();
                    runningTasks.add(debugTask);
                    System.out.println("App loop started");
                }
                break;
        }
    }

    private void handleEventAppLoop(CommEvent event) throws Exception {
        if (event.getType() == CommEvent.EventType.MESSAGE_RECEIVED) {
            CommMessage msg = ((MessageEvent) event).getMessage();

            if (msg.getType() == CommMessage.MessageType.SIGNAL) {
                SignalData signalMsg = new SignalData(msg);
                if (signalMsg.getCommand() == SignalData.Command.PING_VALUE) {
                    System.out.println("Ping message received, responding with pong");
                    send(new SignalData(SignalData.Command.PING_VALUE, signalMsg.getParameterValue()).getMessage());

                } else if (event.matchSignalData(new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.BREAK))) {
                    System.out.println("Disconnect message received, leaving app loop and disconnecting");
                    // stop all running tasks
                    runningTasks.forEach(CommTask::stop);
                    send(new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.BREAK_ACK).getMessage());
                    commInterface.disconnect();
                } else if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.START))){
                    System.out.println("Starting magnetometer calibration procedure");
                    send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.ACK).getMessage());
                    state = State.CALIBRATE_MAGNET;
                } else if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATE_ACCEL, SignalData.Parameter.START))){
                    System.out.println("Starting accelerometer calibration procedure");
                    runningTasks.forEach(CommTask::stop);
                    send(new SignalData(SignalData.Command.CALIBRATE_ACCEL, SignalData.Parameter.ACK).getMessage());
                    Thread.sleep(500);
                    send(new SignalData(SignalData.Command.CALIBRATE_ACCEL, SignalData.Parameter.DONE).getMessage());
                    sendCalibrationSettings(new CalibrationSettings());
                    state = State.CALIBRATE_ACCEL;
                } else if(event.matchSignalData(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.START))) {
                    System.out.println("Flight loop started");
                    runningTasks.forEach(CommTask::stop);
                    send(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.ACK).getMessage());
                    state = State.FLIGHT_LOOP;
                }
                // TODO handle flight loop starting procedure
            }
            // TODO here handle rest of messages that can start actions (flight loop, calibrations...)
            // TODO for example any action starts with SignalData with command - action name and parameter START
            // TODO event.matchSignalData(new SignalData(SignalData.Command.???ACTION???, SignalData.Parameter.START)
            // TODO example of handling FLIGHT_LOOP start above
        }
    }

    private void handleEventFlightLoop(CommEvent event) throws Exception {
            switch (flight_state) {
                case WAITING_FOR_RUNNING:
                    if (event.matchSignalData(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.READY))) {
                        debugTask.start();
                        runningTasks.add(debugTask);
                        flight_state = Flight_state.RUNNING;
                        System.out.println("Flight loop ready");
                    }
                    break;
                case RUNNING:
                    if (event.getType() == CommEvent.EventType.MESSAGE_RECEIVED) {
                        CommMessage message = ((MessageEvent) event).getMessage();
                        switch (message.getType()) {
                            case AUTOPILOT:
                                System.out.println("Autopilot mode on");
                                // Autopilot
                                break;
                            case CONTROL:
                                System.out.println("Control data received");
                                ControlData controlData = new ControlData(message);
                                updateDebugData(controlData);
                                System.out.println(controlData.toString());
                                if (controlData.getCommand() == ControlData.ControllerCommand.STOP) {
                                    send(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.BREAK_ACK).getMessage());
                                    System.out.println("I want make you happy");
                                    state = State.APP_LOOP;
                                }
                        }
                    }
                    break;
            }
    }

    private void handleEventCalibrationMagnetometer(CommEvent event) throws Exception {
        switch (magnetometerState) {
            case MAGNET_CALIBRATION_IDLE:
                if (event.matchSignalData(
                        new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.SKIP))){
                    magnetometerState = MagnetometerState.MAGNET_CALIBRATION_SKIPPED;
                } else if (event.matchSignalData
                        (new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.DONE))){
                    magnetometerState = MagnetometerState.MAGNET_CALIBRATION_DONE;
                }
                break;
            case MAGNET_CALIBRATION_SKIPPED:
                System.out.println("User breaks calibration");
                send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.ACK).getMessage());
                state = State.APP_LOOP;
                break;
            case MAGNET_CALIBRATION_DONE:
                if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATION_SETTINGS_DATA, SignalData.Parameter.DATA_ACK))) {
                    System.out.println("Calibration finished");
                    if (event.matchSignalData(
                            new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.ACK))) {
                        System.out.println("Calibration procedure done successfully.");
                        send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.DONE).getMessage());
                    } else if (event.matchSignalData(
                            new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.BAD_CRC))) {
                        System.out.println("Sending calibration failed, application reports BAD_CRC, retransmitting...");
                        calibrationSettingsSendingFails++;
                        sendCalibrationSettings(new CalibrationSettings());
                    } else if (event.matchSignalData(
                            new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.TIMEOUT))) {
                        System.out.println("Sending calibration failed, application reports TIMEOUT, retransmitting...");
                        calibrationSettingsSendingFails++;
                        sendCalibrationSettings(new CalibrationSettings());
                    }
                    if (calibrationSettingsSendingFails >= 3) {
                        throw new Exception("Calibration settings procedure failed, max retransmission limit exceeded!");
                    }
                    state = State.APP_LOOP;
                } else {
                    System.out.println("Calibration failed");
                    send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.FAIL).getMessage());
                    state = State.APP_LOOP;
                }
                break;
        }
    }

    private void handleEventCalibrationAccelerometer(CommEvent event) throws Exception {
        if (event.matchSignalData(
                new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.ACK))) {
            System.out.println("Calibration procedure done successfully.");
            debugTask.start();
            runningTasks.add(debugTask);
            state = State.APP_LOOP;
        } else if (event.matchSignalData(
                new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.BAD_CRC))) {
            System.out.println("Sending calibration failed, application reports BAD_CRC, retransmitting...");
            calibrationSettingsSendingFails++;
            sendCalibrationSettings(new CalibrationSettings());
        } else if (event.matchSignalData(
                new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.TIMEOUT))) {
            System.out.println("Sending calibration failed, application reports TIMEOUT, retransmitting...");
            calibrationSettingsSendingFails++;
            sendCalibrationSettings(new CalibrationSettings());
        }
        if (calibrationSettingsSendingFails >= 3) {
            throw new Exception("Calibration settings procedure failed, max retransmission limit exceeded!");
        }
    }

    private void send (CommMessage message) {
        commInterface.send(message.getByteArray());
    }

    private void sendCalibrationSettings(CalibrationSettings calibrationSettings) {
        calibrationSettings.getMessages().forEach(this::send);
    }

    private DebugData getStartDebugData() {
        DebugData result = new DebugData();
        result.setRoll((float)Math.toRadians(23.0));
        result.setPitch((float)Math.toRadians(13.0));
        result.setYaw((float)Math.toRadians(53.0));
        result.setLatitude(50.034f);
        result.setLongitude(19.940f);
        result.setRelativeAltitude(24.2f);
        result.setVLoc(3.2f);
        result.setControllerState(DebugData.ControllerState.APPLICATION_LOOP);
        result.setFLagState(DebugData.FlagId.GPS_FIX_3D, true);
        return result;
    }

    private void simulateSensors() {
        debugDataLock.lock();
        // TODO simulate changing of debug data parameters
        debugDataToSend.setLatitude(debugDataToSend.getLatitude() + (float)Math.random()/10000.0f - 0.00003f);
        debugDataToSend.setLongitude(debugDataToSend.getLongitude() + (float)Math.random()/10000.0f + 0.00003f);
        debugDataLock.unlock();
    }

    private void updateDebugData(ControlData controlData) {
        debugDataLock.lock();
        debugDataToSend.setControllerState(
                DebugData.ControllerState.getControllerState(controlData.getCommand().getValue()));
        debugDataToSend.setSolverMode(controlData.getMode());
        debugDataLock.unlock();
    }

    private DebugData getDebugDataToSend() {
        debugDataLock.lock();
        DebugData result = new DebugData(debugDataToSend);
        debugDataLock.unlock();
        return result;
    }

    private CommTask debugTask = new CommTask(25) {
        @Override
        protected String getTaskName() {
            return "debug_task";
        }

        @Override
        protected void task() {
            simulateSensors();
            DebugData debugData = getDebugDataToSend();
            System.out.println("Debug: " + debugData.toString());
            send(debugData.getMessage());
        }
    };
}
