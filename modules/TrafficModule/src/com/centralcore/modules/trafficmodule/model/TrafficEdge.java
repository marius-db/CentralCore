package com.centralcore.modules.trafficmodule.model;

public class TrafficEdge {

    private String id;
    private String from;
    private String to;
    private int    lanes;
    private boolean main;
    private String name;
    private double density; // 0.0 - 1.0, actualizado por el simulador

    public TrafficEdge() {}

    public String  getId()              { return id;      }
    public void    setId(String id)     { this.id = id;   }
    public String  getFrom()            { return from;    }
    public void    setFrom(String f)    { this.from = f;  }
    public String  getTo()              { return to;      }
    public void    setTo(String t)      { this.to = t;    }
    public int     getLanes()           { return lanes;   }
    public void    setLanes(int l)      { this.lanes = l; }
    public boolean isMain()             { return main;    }
    public void    setMain(boolean m)   { this.main = m;  }
    public String  getName()            { return name;    }
    public void    setName(String n)    { this.name = n;  }
    public double  getDensity()         { return density; }
    public void    setDensity(double d) { this.density = d; }
}
