package com.addrone;

import com.multicopter.java.*;
import com.multicopter.java.actions.CommHandlerAction;
import com.multicopter.java.actions.FlightLoopAction;
import com.multicopter.java.data.*;
import com.multicopter.java.events.CommEvent;
import com.multicopter.java.events.MessageEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.sqrt;

/**
 * Created by ebarnaw on 2017-01-03.
 */
public class CommHandlerSimulator implements CommInterface.CommInterfaceListener,
        CommDispatcher.CommDispatcherListener {

    private CommInterface commInterface;
    private CommDispatcher dispatcher;

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
        CALIBRATE_ACCEL,
        UPLOAD_CONTROL_SETTINGS,
        DOWNLOAD_CONTROL_SETTINGS
    }

    private enum MagnetometerState{
        MAGNET_CALIBRATION_IDLE,
        MAGNET_CALIBRATION_RUNNING
    }

    private enum Flight_state{
        WAITING_FOR_RUNNING,
        RUNNING
    }

    public CommHandlerSimulator(CommInterface commInterface) {
        this.commInterface = commInterface;
        this.dispatcher = new CommDispatcher(this);

        this.state = State.IDLE;
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
                case UPLOAD_CONTROL_SETTINGS:
                    handleEventUploadControlSettings(event);
                    break;
                case DOWNLOAD_CONTROL_SETTINGS:
                    handleEventDownloadControlSettings(event);
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
                        new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.DATA_INVALID))) {
                    System.out.println("Sending calibration failed, application reports DATA_INVALID, retransmitting...");
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
                    debugTask.stop();
                    send(new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.BREAK_ACK).getMessage());
                    commInterface.disconnect();

                } else if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.START))){
                    System.out.println("Starting magnetometer calibration procedure");
                    debugTask.stop();
                    state = State.CALIBRATE_MAGNET;
                    magnetometerState = MagnetometerState.MAGNET_CALIBRATION_IDLE;
                    send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.ACK).getMessage());

                } else if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATE_ACCEL, SignalData.Parameter.START))){
                    System.out.println("Starting accelerometer calibration procedure");
                    debugTask.stop();
                    state = State.CALIBRATE_ACCEL;
                    send(new SignalData(SignalData.Command.CALIBRATE_ACCEL, SignalData.Parameter.ACK).getMessage());
                    Thread.sleep(500);
                    send(new SignalData(SignalData.Command.CALIBRATE_ACCEL, SignalData.Parameter.DONE).getMessage());
                    sendCalibrationSettings(new CalibrationSettings());

                } else if(event.matchSignalData(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.START))) {
                    System.out.println("Flight loop started");
                    debugTask.stop();
                    send(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.ACK).getMessage());
                    state = State.FLIGHT_LOOP;

                } else if(event.matchSignalData(new SignalData(SignalData.Command.UPLOAD_SETTINGS, SignalData.Parameter.START))){
                    System.out.println("Uploading control settings");
                    state = State.UPLOAD_CONTROL_SETTINGS;
                    uploadStage = UploadControlSettingsStage.INIT;
                    debugTask.stop();
                    send(new SignalData(SignalData.Command.UPLOAD_SETTINGS, SignalData.Parameter.ACK).getMessage());

                } else if(event.matchSignalData(new SignalData(SignalData.Command.DOWNLOAD_SETTINGS, SignalData.Parameter.START))){
                    state = State.DOWNLOAD_CONTROL_SETTINGS;
                    System.out.println("Downloading control settings");
                    debugTask.stop();
                    send(new SignalData(SignalData.Command.DOWNLOAD_SETTINGS, SignalData.Parameter.ACK).getMessage());

                }
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
                                flight_state =Flight_state.WAITING_FOR_RUNNING;
                            }
                    }
                }
                break;
        }
    }

    private void handleEventCalibrationMagnetometer(CommEvent event) throws Exception {
        switch (magnetometerState) {
            case MAGNET_CALIBRATION_IDLE:
                if(event.matchSignalData(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.SKIP))){
                    System.out.println("User breaks calibration");
                    state = State.APP_LOOP;
                    send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.ACK).getMessage());
                    debugTask.start();
                } else if(event.matchSignalData(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.DONE))){
                    send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.DONE).getMessage());
                    sendCalibrationSettings(new CalibrationSettings());
                    magnetometerState = MagnetometerState.MAGNET_CALIBRATION_RUNNING;
                }  else{
                    System.out.println("Calibration failed");
                    state = State.APP_LOOP;
                    send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.FAIL).getMessage());
                    debugTask.start();
                }
                break;
            case MAGNET_CALIBRATION_RUNNING:
                if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.ACK))) {
                    System.out.println("Calibration procedure done successfully.");
                    state = State.APP_LOOP;
                    debugTask.start();
                } else if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.DATA_INVALID))) {
                    System.out.println("Sending calibration failed, application reports DATA_INVALID, retransmitting...");
                    calibrationSettingsSendingFails++;
                    sendCalibrationSettings(new CalibrationSettings());
                } else if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.TIMEOUT))) {
                    System.out.println("Sending calibration failed, application reports TIMEOUT, retransmitting...");
                    calibrationSettingsSendingFails++;
                    sendCalibrationSettings(new CalibrationSettings());
                }
                if (calibrationSettingsSendingFails >= 3) {
                    throw new Exception("Calibration settings procedure failed, max retransmission limit exceeded!");
                }
                break;
        }
    }

    private void handleEventCalibrationAccelerometer(CommEvent event) throws Exception {
        if (event.matchSignalData(
                new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.ACK))) {
            System.out.println("Calibration procedure done successfully.");
            debugTask.start();
            state = State.APP_LOOP;
        } else if (event.matchSignalData(
                new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.DATA_INVALID))) {
            System.out.println("Sending calibration failed, application reports DATA_INVALID, retransmitting...");
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

    private enum UploadControlSettingsStage{
        INIT,
        CONTROL_ACK,
        FINAL
    }
    UploadControlSettingsStage uploadStage;
    private int uploadFails;

    private void handleEventUploadControlSettings(CommEvent event) throws Exception{

        ControlSettings controlSettings = new ControlSettings();
        controlSettings.setRollProp(15);
        controlSettings.setPitchProp(16);
        controlSettings.setYawProp(17);

        //controlSettings.getMessages().forEach(this::send);
        System.out.println("Upload control settings stage @"+uploadStage.toString());
        switch (uploadStage) {

            case INIT:
                System.out.println("Starting Control settings procedure...");
                uploadFails = 0;
                send(new SignalData(SignalData.Command.CONTROL_SETTINGS, SignalData.Parameter.READY).getMessage());
                controlSettings.getMessages().forEach(this::send);

                break;

            case CONTROL_ACK:

                if (event.matchSignalData(
                        new SignalData(SignalData.Command.CONTROL_SETTINGS, SignalData.Parameter.ACK))) {
                    uploadStage = UploadControlSettingsStage.FINAL;
                    System.out.println("Control settings procedure done successfully, waiting for final command");
                } else if (event.matchSignalData(
                        new SignalData(SignalData.Command.CONTROL_SETTINGS, SignalData.Parameter.DATA_INVALID))) {
                    System.out.println("Sending Control settings failed, application reports BAD_CRC, retransmitting...");
                    uploadFails++;
                    controlSettings.getMessages().forEach(this::send);
                } else if (event.matchSignalData(
                        new SignalData(SignalData.Command.CONTROL_SETTINGS, SignalData.Parameter.TIMEOUT))) {
                    System.out.println("Sending Control settings failed, application reports TIMEOUT, retransmitting...");
                    uploadFails++;
                    controlSettings.getMessages().forEach(this::send);
                }
                if (uploadFails >= 3) {
                    uploadStage = UploadControlSettingsStage.FINAL;
                    System.out.println("Control settings procedure failed, max retransmission limit exceeded!");
                }

                break;
            case FINAL:
                debugTask.start();
                state = State.APP_LOOP;
                break;
        }
    }


    private void handleEventDownloadControlSettings(CommEvent event) {
        //TODO #Doing #jeszczeNieZrobione

        if (true){ //if correct data found in internal memmory
            send(new SignalData(SignalData.Command.DOWNLOAD_SETTINGS, SignalData.Parameter.ACK).getMessage());
            //pobrać z internala i wyslac
            System.out.println("Uploading Control Settings...");
        }
        else {
            send(new SignalData(SignalData.Command.DOWNLOAD_SETTINGS, SignalData.Parameter.FAIL).getMessage());
        }
        debugTask.start();
        state = State.APP_LOOP;

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

        @Override
        protected void onStarted() {
            super.onStarted();

        }

        @Override
        protected void onStopped() {
            super.onStopped();

        }
    };
}