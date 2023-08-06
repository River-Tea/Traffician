package org.rivertea.traffician;

import org.opencv.core.Mat;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class TrafficianServer extends NanoHTTPD {
    private static final Integer PORT = 8443;
    private Mat cameraData;
    private Map<String, String> trafficData;

    public TrafficianServer() {
        super("0.0.0.0", PORT);
    }

    public Mat getCameraData() {
        return cameraData;
    }

    public void setCameraData(Mat cameraData) {
        this.cameraData = cameraData;
    }

    public Map<String, String> getTrafficData() {
        return trafficData;
    }

    public void setTrafficData(Map<String, String> trafficData) {
        this.trafficData = trafficData;
    }

    @Override
    public Response serve(IHTTPSession session) {
        return super.serve(session);
    }
}
