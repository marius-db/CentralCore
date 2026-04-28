package com.centralcore.modules.trafficmodule.model;

public class TrafficLight {

    // estados posibles: "ns_green", "ew_green", "yellow", "red"
    private String id;
    private String nodeId;
    private String state;
    private int    timer;

    public TrafficLight() {}

    public String getId()             { return id;     }
    public void   setId(String id)    { this.id = id;  }
    public String getNodeId()         { return nodeId; }
    public void   setNodeId(String n) { this.nodeId = n; }
    public String getState()          { return state;  }
    public void   setState(String s)  { this.state = s; }
    public int    getTimer()          { return timer;  }
    public void   setTimer(int t)     { this.timer = t; }
}
