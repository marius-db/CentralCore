package com.centralcore.modules.trafficmodule.model;

public class SimCar {

    private String id;
    private double x;
    private double y;
    private String nodeA;
    private String nodeB;
    private double progress;
    private int    lane;

    public SimCar() {}

    public String getId()               { return id;       }
    public void   setId(String id)      { this.id = id;    }
    public double getX()                { return x;        }
    public void   setX(double x)        { this.x = x;      }
    public double getY()                { return y;        }
    public void   setY(double y)        { this.y = y;      }
    public String getNodeA()            { return nodeA;    }
    public void   setNodeA(String n)    { this.nodeA = n;  }
    public String getNodeB()            { return nodeB;    }
    public void   setNodeB(String n)    { this.nodeB = n;  }
    public double getProgress()         { return progress; }
    public void   setProgress(double p) { this.progress = p; }
    public int    getLane()             { return lane;     }
    public void   setLane(int l)        { this.lane = l;   }
}
