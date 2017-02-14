package com.simulator;

import com.multicopter.java.*;
import com.multicopter.java.data.*;
import com.multicopter.java.events.CommEvent;
import com.multicopter.java.events.MessageEvent;
import com.multicopter.java.events.SignalPayloadEvent;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ebarnaw on 2017-01-03.
 */
public class CommHandlerSimulator implements CommInterface.CommInterfaceListener,
        CommDispatcher.CommDispatcherListener {

    private CommInterface commInterface;
    private CommDispatcher dispatcher;

    private State state;
    private FlightLoopStage flightLoopStage;
    private MagnetometerStage magnetometerState;
    private UploadControlSettingsStage uploadStage;
    private UploadRouteStage uploadRouteState;
    private ConnectionStage connectionStage;

    private CalibrationSettings calibrationSettings = getStartCalibrationSettings();
    private ControlSettings controlSettings = getStartControlSettings();
    private RouteContainer routeContainer = getStartRouteContainer();

    private DebugData debugDataToSend = getStartDebugData();
    private Lock debugDataLock = new ReentrantLock();

    private int sendingProcedureFails;
    private int uploadFails;
    private int uploadRouteFails;

    private enum State {
        IDLE,
        CONNECTING_APP_LOOP,
        APP_LOOP,
        FLIGHT_LOOP,
        CALIBRATE_MAGNET,
        CALIBRATE_ACCEL,
        UPLOAD_CONTROL_SETTINGS,
        DOWNLOAD_CONTROL_SETTINGS,
        UPLOAD_ROUTE_CONTAINER,
        DOWNLOAD_ROUTE_CONTAINER
    }

    private enum ConnectionStage {
        INITIAL_COMMAND,
        CALIBRATION_ACK,
        FINAL_COMMAND
    }

    private enum FlightLoopStage {
        CONTROLS_ACK,
        ROUTE_ACK,
        FINAL_COMMAND,
        RUNNING
    }

    private enum MagnetometerStage {
        USER_COMMAND,
        CALIBRATION_ACK
    }

    private enum UploadControlSettingsStage {
        INIT,
        UPLOAD_PROCEDURE,
        FINAL
    }

    private enum UploadRouteStage {
        IDLE,
        RUNNING
    }

    public CommHandlerSimulator(CommInterface commInterface) {
        this.commInterface = commInterface;
        this.dispatcher = new CommDispatcher(this);

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
                case CALIBRATE_ACCEL:
                    handleEventCalibrationAccelerometer(event);
                    break;
                case CALIBRATE_MAGNET:
                    handleEventCalibrationMagnetometer(event);
                    break;
                case UPLOAD_CONTROL_SETTINGS:
                    handleEventUploadControlSettings(event);
                    break;
                case DOWNLOAD_CONTROL_SETTINGS:
                    handleEventDownloadControlSettings(event);
                    break;
                case UPLOAD_ROUTE_CONTAINER:
                    handleEventUploadRouteContainer(event);
                    break;
                case DOWNLOAD_ROUTE_CONTAINER:
                    handleEventDownloadRouteContainer(event);
                    break;
                default:
                    System.out.println("Error state!");
            }
        } catch (Exception e) {
            System.out.println("Error while handling event! " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEventConnectingAppLoop(CommEvent event) throws Exception {
        System.out.println("Connecting app loop @ " + connectionStage.toString());
        switch (connectionStage) {
            case INITIAL_COMMAND:
                if (event.matchSignalData(new SignalData(SignalData.Command.START_CMD, SignalData.Parameter.START))) {
                    System.out.println("Start command received, staring calibration procedure");
                    send(new SignalData(SignalData.Command.START_CMD, SignalData.Parameter.ACK).getMessage());
                    // simulate calibration process (sleep 0.5s)
                    Thread.sleep(500);
                    send(new SignalData(SignalData.Command.CALIBRATION_SETTINGS, SignalData.Parameter.READY).getMessage());
                    startSignalPayloadSending(calibrationSettings);
                    connectionStage = ConnectionStage.CALIBRATION_ACK;
                }
                break;

            case CALIBRATION_ACK:
                if (handleSignalPayloadAck(calibrationSettings, event)) {
                    System.out.println("Calibration procedure done successfully, waiting for final command");
                    connectionStage = ConnectionStage.FINAL_COMMAND;
                }
                break;

            case FINAL_COMMAND:
                if (event.matchSignalData(new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.START))) {
                    System.out.println("ApplicationLoop started");
                    state = State.APP_LOOP;
                    send(new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.ACK).getMessage());
                    debugTask.start();
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
                    debugTask.stop();
                    send(new SignalData(SignalData.Command.APP_LOOP, SignalData.Parameter.BREAK_ACK).getMessage());
                    commInterface.disconnect();

                } else if(event.matchSignalData(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.START))) {
                    System.out.println("FlightLoop initiated");
                    debugTask.stop();
                    state = State.FLIGHT_LOOP;
                    flightLoopStage = FlightLoopStage.CONTROLS_ACK;
                    send(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.ACK).getMessage());
                    startSignalPayloadSending(controlSettings);

                } else if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATE_ACCEL, SignalData.Parameter.START))){
                    System.out.println("Starting accelerometer calibration procedure");
                    debugTask.stop();
                    state = State.CALIBRATE_ACCEL;
                    send(new SignalData(SignalData.Command.CALIBRATE_ACCEL, SignalData.Parameter.ACK).getMessage());
                    // simulate calibration process (sleep 0.5s)
                    Thread.sleep(500);
                    send(new SignalData(SignalData.Command.CALIBRATE_ACCEL, SignalData.Parameter.DONE).getMessage());
                    startSignalPayloadSending(calibrationSettings);

                } else if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.START))){
                    System.out.println("Starting magnetometer calibration procedure");
                    debugTask.stop();
                    state = State.CALIBRATE_MAGNET;
                    magnetometerState = MagnetometerStage.USER_COMMAND;
                    send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.ACK).getMessage());

                } else if(event.matchSignalData(new SignalData(SignalData.Command.UPLOAD_SETTINGS, SignalData.Parameter.START))){
                    System.out.println("Uploading ControlSettings");
                    uploadFails = 0;
                    state = State.UPLOAD_CONTROL_SETTINGS;
                    uploadStage = UploadControlSettingsStage.INIT;
                    debugTask.stop();
                    send(new SignalData(SignalData.Command.UPLOAD_SETTINGS, SignalData.Parameter.ACK).getMessage());

                } else if(event.matchSignalData(new SignalData(SignalData.Command.DOWNLOAD_SETTINGS, SignalData.Parameter.START))){
                    System.out.println("ControlSettings download, sending to client");
                    state = State.DOWNLOAD_CONTROL_SETTINGS;
                    debugTask.stop();
                    send(new SignalData(SignalData.Command.DOWNLOAD_SETTINGS, SignalData.Parameter.ACK).getMessage());
                    startSignalPayloadSending(controlSettings);

                } else if(event.matchSignalData(new SignalData(SignalData.Command.UPLOAD_ROUTE, SignalData.Parameter.START))){
                    System.out.println("Starting RouteContainer upload procedure");
                    state = State.UPLOAD_ROUTE_CONTAINER;
                    uploadRouteState = UploadRouteStage.IDLE;
                    debugTask.stop();
                    send(new SignalData(SignalData.Command.UPLOAD_ROUTE, SignalData.Parameter.ACK).getMessage());

                } else if(event.matchSignalData(new SignalData(SignalData.Command.DOWNLOAD_ROUTE, SignalData.Parameter.START))){
                    System.out.println("RouteContainer download, sending to client");
                    state = State.DOWNLOAD_ROUTE_CONTAINER;
                    debugTask.stop();
                    send(new SignalData(SignalData.Command.DOWNLOAD_ROUTE, SignalData.Parameter.ACK).getMessage());
                    startSignalPayloadSending(routeContainer);
                }
            }
            // TODO here handle rest of messages that can start actions (flight loop, calibrations...)
            // TODO for example any action starts with SignalData with command - action name and parameter START
            // TODO event.matchSignalData(new SignalData(SignalData.Command.???ACTION???, SignalData.Parameter.START)
        }
    }

    private void handleEventFlightLoop(CommEvent event) throws Exception {
        switch (flightLoopStage) {
            case CONTROLS_ACK:
                if (handleSignalPayloadAck(controlSettings, event))
                {
                    System.out.println("Flight ControlSettings sent successfully");
                    flightLoopStage = FlightLoopStage.ROUTE_ACK;
                    send(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.VIA_ROUTE_ALLOWED).getMessage());
                    startSignalPayloadSending(routeContainer);
                }
                break;

            case ROUTE_ACK:
                if (handleSignalPayloadAck(routeContainer, event))
                {
                    System.out.println("Flight RouteContainer sent successfully");
                    flightLoopStage = FlightLoopStage.FINAL_COMMAND;
                }
                break;

            case FINAL_COMMAND:
                if (event.matchSignalData(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.READY))) {
                    System.out.println("FlightLoop initialization done");
                    flightLoopStage = FlightLoopStage.RUNNING;
                    debugTask.start();
                }
                break;

            case RUNNING:
                if (event.getType() == CommEvent.EventType.MESSAGE_RECEIVED) {
                    CommMessage message = ((MessageEvent) event).getMessage();
                    switch (message.getType()) {
                        case SIGNAL:
                            SignalData signalData = new SignalData(message);
                            if (signalData.getCommand() == SignalData.Command.PING_VALUE) {
                                System.out.println("Ping message received, responding with pong");
                                send(new SignalData(SignalData.Command.PING_VALUE, signalData.getParameterValue()).getMessage());
                            }
                            break;

                        case AUTOPILOT:
                            System.out.println("AutopilotData received");
                            AutopilotData autopilotData = new AutopilotData(message);
                            System.out.println(autopilotData.toString());
                            send(autopilotData.getMessage());
                            break;

                        case CONTROL:
                            System.out.println("ControlData received");
                            ControlData controlData = new ControlData(message);
                            System.out.println(controlData.toString());
                            updateDebugData(controlData);
                            if (controlData.getCommand() == ControlData.ControllerCommand.STOP) {
                                System.out.println("Stop command received, leaving flight loop");
                                send(new SignalData(SignalData.Command.FLIGHT_LOOP, SignalData.Parameter.BREAK_ACK).getMessage());
                                state = State.APP_LOOP;
                            }
                    }
                }
                break;
        }
    }

    private void handleEventCalibrationAccelerometer(CommEvent event) throws Exception {
        if (handleSignalPayloadAck(calibrationSettings, event)) {
            System.out.println("Accelerometer calibration procedure done successfully.");
            debugTask.start();
            state = State.APP_LOOP;
        }
    }

    private void handleEventCalibrationMagnetometer(CommEvent event) throws Exception {
        switch (magnetometerState) {
            case USER_COMMAND:
                if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.SKIP))){
                    System.out.println("User skips magnetometer calibration");
                    state = State.APP_LOOP;
                    send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.ACK).getMessage());
                    debugTask.start();
                } else if (event.matchSignalData(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.DONE))){
                    System.out.println("Magnetometer calibration done");
                    // simulate calibration computation (sleep 0.5s)
                    Thread.sleep(100);
                    send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.DONE).getMessage());
                    startSignalPayloadSending(calibrationSettings);
                    magnetometerState = MagnetometerStage.CALIBRATION_ACK;
                } else {
                    System.out.println("Calibration failed");
                    state = State.APP_LOOP;
                    send(new SignalData(SignalData.Command.CALIBRATE_MAGNET, SignalData.Parameter.FAIL).getMessage());
                    debugTask.start();
                }
                break;

            case CALIBRATION_ACK:
                if (handleSignalPayloadAck(calibrationSettings, event)) {
                    System.out.println("Magnetometer calibration procedure done successfully.");
                    state = State.APP_LOOP;
                    debugTask.start();
                }
                break;
        }
    }

    private void handleEventUploadControlSettings(CommEvent event) throws Exception{
        System.out.println("Upload control settings stage @"+uploadStage.toString());
        System.out.println("Starting Upload Control settings procedure...");

        if (event.getType() == CommEvent.EventType.SIGNAL_PAYLOAD_RECEIVED
                && ((SignalPayloadEvent)event).getDataType() == SignalData.Command.CONTROL_SETTINGS_DATA) {

            SignalPayloadEvent signalEvent = (SignalPayloadEvent) event;
            ControlSettings controlSettings  = (ControlSettings) signalEvent.getData();

            if (uploadFails == 3){
                state = State.APP_LOOP;
                send(new SignalData(SignalData.Command.CONTROL_SETTINGS, SignalData.Parameter.TIMEOUT).getMessage());
                System.out.println("Upload control Settings Timeout");
                debugTask.start();
            } else if (controlSettings.isValid()) {
                state = State.APP_LOOP;
                System.out.println("Control settings received");
                send(new SignalData(SignalData.Command.CONTROL_SETTINGS, SignalData.Parameter.ACK).getMessage());
                this.controlSettings = controlSettings;
                debugTask.start();
            } else {
                System.out.println("Control settings received but the data is invalid, responding with DATA_INVALID");
                send(new SignalData(SignalData.Command.CONTROL_SETTINGS, SignalData.Parameter.DATA_INVALID).getMessage());
                uploadFails++;
            }
        } else {
            System.out.println("Unexpected event received at state " + state.toString());
            state = State.APP_LOOP;
            debugTask.start();
        }
    }

    private void handleEventDownloadControlSettings(CommEvent event) throws Exception {
        // Download is from user perspective, here data is send to application
        if (handleSignalPayloadAck(controlSettings, event)) {
            System.out.println("Control settings download procedure done successfully.");
            debugTask.start();
            state = State.APP_LOOP;
        }
    }

    private void handleEventUploadRouteContainer(CommEvent event) throws Exception {
        switch(uploadRouteState) {
            case IDLE:
                System.out.println("Starting RouteContainer upload procedure...");
                uploadRouteFails = 0;
                uploadRouteState = UploadRouteStage.RUNNING;
                break;
            case RUNNING:
                if (event.getType() == CommEvent.EventType.SIGNAL_PAYLOAD_RECEIVED
                        && ((SignalPayloadEvent) event).getDataType() == SignalData.Command.ROUTE_CONTAINER_DATA) {
                    SignalPayloadEvent signalEvent = (SignalPayloadEvent) event;
                    routeContainer = (RouteContainer) signalEvent.getData();
                    if(routeContainer.isValid()){
                        System.out.println("Route Container upload procedure done successfully");
                        send(new SignalData(SignalData.Command.ROUTE_CONTAINER, SignalData.Parameter.ACK).getMessage());
                        debugTask.start();
                        state = State.APP_LOOP;
                    } else {
                        System.out.println("Uploading Route Container procedure failed, application reports DATA_INVALID, retransmitting...");
                        send(new SignalData(SignalData.Command.ROUTE_CONTAINER, SignalData.Parameter.DATA_INVALID).getMessage());
                        uploadRouteFails++;
                    }
                }
                if (uploadRouteFails >= 3) {
                    System.out.println("Uploading Route Container procedure failed, application reports TIMEOUT, retransmitting...");
                    send(new SignalData(SignalData.Command.ROUTE_CONTAINER, SignalData.Parameter.TIMEOUT).getMessage());
                }
                break;
        }
    }

    private void handleEventDownloadRouteContainer(CommEvent event) throws Exception {
        // Download is from user perspective, here data is send to application
        if (handleSignalPayloadAck(routeContainer, event)) {
            System.out.println("Route container download procedure done successfully.");
            debugTask.start();
            state = State.APP_LOOP;
        }
    }

    private void send (final CommMessage message) {
        commInterface.send(message.getByteArray());
    }

    private void send(final SignalPayloadData data) {
        data.getMessages().forEach(this::send);
    }

    private void startSignalPayloadSending(final SignalPayloadData data) {
        sendingProcedureFails = 0;
        send(data);
    }

    private boolean handleSignalPayloadAck(final SignalPayloadData data, CommEvent event) throws Exception {
        if (event.matchSignalData(new SignalData(data.getDataCommand(), SignalData.Parameter.ACK))) {
            return true;

        } else if (event.matchSignalData(new SignalData(data.getDataCommand(), SignalData.Parameter.DATA_INVALID))) {
            System.out.println("Sending " + data.getDataCommand().toString() + ", client reports DATA_INVALID, retransmitting...");
            sendingProcedureFails++;
            send(data);

        } else if (event.matchSignalData(new SignalData(data.getDataCommand(), SignalData.Parameter.TIMEOUT))) {
            System.out.println("Sending " + data.getDataCommand().toString() + ", client reports TIMEOUT, retransmitting...");
            sendingProcedureFails++;
            send(data);

        } else {
            throw new Exception("Unexpected message received at " + data.getDataCommand().toString() + " sending procedure!");
        }

        if (sendingProcedureFails >= 3) {
            throw new Exception("Sending " + data.getDataCommand().toString() + " procedure failed, max retransmission limit exceeded!");
        }
        return false;
    }

    private float getRandN() {
        return (float)(Math.random() - 0.5) * 2.0f;
    }

    private CalibrationSettings getStartCalibrationSettings() {
        CalibrationSettings result = new CalibrationSettings();
        result.setBoardTypeValue(CalibrationSettings.BoardType.TYPE_BASIC_V3.getValue());
        result.setGyroOffset(new float[]{getRandN() * 400.0f, getRandN() * 400.f, getRandN() * 200.0f});
        result.setFlagState(CalibrationSettings.FlagId.IS_GPS_CONNECTED, true);
        result.setCrc();
        return result;
    }

    private ControlSettings getStartControlSettings() {
        ControlSettings result = new ControlSettings();

        result.setCrc();
        return result;
    }

    private RouteContainer getStartRouteContainer() {
        RouteContainer result = new RouteContainer();

        result.setCrc();
        return result;
    }

    private DebugData getStartDebugData() {
        DebugData result = new DebugData();
        result.setRoll((float)Math.toRadians(getRandN() * 30.0f));
        result.setPitch((float)Math.toRadians(getRandN()));
        result.setYaw((float)Math.toRadians(getRandN() * 50.0f));
        result.setLatitude(50.034f + getRandN() / 1000.0f);
        result.setLongitude(19.940f + getRandN() / 1000.0f);
        result.setRelativeAltitude(getRandN() * 30.0f);
        result.setVLoc(getRandN() * 2.0f);
        result.setControllerState(DebugData.ControllerState.APPLICATION_LOOP);
        result.setFLagState(DebugData.FlagId.GPS_FIX, true);
        result.setFLagState(DebugData.FlagId.GPS_FIX_3D, true);
        return result;
    }

    private float time = (float)(Math.random() * 2);

    private void simulateSensors() {
        debugDataLock.lock();
        time += 1.0f / 25;

        debugDataToSend.setRoll(debugDataToSend.getRoll()
                + (float)Math.sin(0.53 * time) / 240.0f
                + (float)Math.sin(1 * time) / 190.0f
                + (float)Math.sin(5 * time) / 210.0f
                + (float)Math.sin(2 * time + 1) / 310.0f);
        debugDataToSend.setPitch(debugDataToSend.getPitch()
                + (float)Math.sin(0.48 * time) / 330.0f
                + (float)Math.sin(1.2 * time) / 220.0f
                + (float)Math.sin(4 * time) / 320.0f
                + (float)Math.sin(3 * time + 1) / 410.0f);
        debugDataToSend.setYaw(debugDataToSend.getYaw()
                + (float)Math.sin(0.24 * time) / 190.0f
                + (float)Math.sin(0.6 * time + getRandN()) / 50.0f
                + (float)Math.sin(5 * time + getRandN()) / 90.0f
                + (float)Math.sin(3 * time + 1) / 150.0f);

        debugDataToSend.setLatitude(debugDataToSend.getLatitude()
                + (float)Math.sin(0.1 * time) /     250000.0f
                + (float)Math.sin(0.23 * time) /    280000.0f
                + (float)Math.sin(0.38 * time) /    300000.0f
                + (float)Math.sin(2 * time) /       340000.0f
                + (float)Math.sin(3 * time) /       370000.0f
                + (float)Math.sin(1 * time) /       250000.0f
                + getRandN() / 1000000.0f);
        debugDataToSend.setLongitude(debugDataToSend.getLongitude()
                + (float)Math.sin(0.22 * time) /    260000.0f
                + (float)Math.sin(0.56 * time) /    300000.0f
                + (float)Math.sin(0.8 * time) /     330000.0f
                + (float)Math.sin(2.1 * time) /     350000.0f
                + (float)Math.sin(3 * time) /       370000.0f
                + (float)Math.sin(1.5 * time) /     260000.0f
                + getRandN() / 1000000.0f);

        debugDataToSend.setVLoc(debugDataToSend.getVLoc()
                + (float)Math.sin(0.44 * time) / 120.0f
                + (float)Math.sin(0.9 * time + getRandN()) / 20.0f
                + (float)Math.sin(5 * time + getRandN()) / 60.0f
                + (float)Math.sin(1 * time + 1) / 110.0f);
        debugDataToSend.setRelativeAltitude(debugDataToSend.getRelativeAltitude()
                + (float)Math.sin(0.74 * time) / 50.0f
                + (float)Math.sin(0.8 * time + getRandN()) / 10.0f
                + (float)Math.sin(5 * time + getRandN()) / 70.0f
                + (float)Math.sin(2 * time + 1) / 110.0f);
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
        debugTask.stop();
    }

    @Override
    public void onError(IOException e) {
        System.out.println("CommHandlerSimulator : onError : " + e.getMessage());
    }

    @Override
    public void onDataReceived(final byte[] data, final int dataSize) {
        dispatcher.proceedReceiving(data, dataSize);
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