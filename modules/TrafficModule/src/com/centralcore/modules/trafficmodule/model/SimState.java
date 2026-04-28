package com.centralcore.modules.trafficmodule.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

//estado completo de la simulacion, actualizado en cada tick
public class SimState {

    private List<TrafficNode>  nodes  = new ArrayList<>();
    private List<TrafficEdge>  edges  = new ArrayList<>();
    private List<TrafficLight> lights = new ArrayList<>();
    private List<SimCar>       cars   = new ArrayList<>();

    //vehiculo de emergencia
    private boolean      evActive     = false;
    private double       evX          = 0;
    private double       evY          = 0;
    private String       evNextNode   = null;
    private List<String> evRoute      = new ArrayList<>();
    private boolean      routeDone    = false;

    //indices para lookups rapidos, se rebuilden cuando cambia el mapa
    private final Map<String, TrafficNode>  nodeMap  = new HashMap<>();
    private final Map<String, TrafficLight> lightMap = new HashMap<>();

    //reconstruye los indices de busqueda rapida tras recibir el mapa
    public void rebuildIndexes() {
        nodeMap.clear();
        lightMap.clear();
        for (TrafficNode n : nodes)  nodeMap.put(n.getId(), n);
        for (TrafficLight l : lights) lightMap.put(l.getNodeId(), l);
    }

    public TrafficNode  findNode(String id)             { return nodeMap.get(id);  }
    public TrafficLight findLightAtNode(String nodeId)  { return lightMap.get(nodeId); }

    //getters / setters

    public List<TrafficNode>  getNodes()            { return nodes;  }
    public void setNodes(List<TrafficNode> n)        { this.nodes = n; }
    public List<TrafficEdge>  getEdges()            { return edges;  }
    public void setEdges(List<TrafficEdge> e)        { this.edges = e; }
    public List<TrafficLight> getLights()           { return lights; }
    public void setLights(List<TrafficLight> l)     { this.lights = l; }
    public List<SimCar>       getCars()             { return cars;   }
    public void setCars(List<SimCar> c)             { this.cars = c; }

    public boolean    isEvActive()                  { return evActive;   }
    public void       setEvActive(boolean b)        { this.evActive = b; }
    public double     getEvX()                      { return evX;        }
    public void       setEvX(double x)              { this.evX = x;      }
    public double     getEvY()                      { return evY;        }
    public void       setEvY(double y)              { this.evY = y;      }
    public String     getEvNextNode()               { return evNextNode; }
    public void       setEvNextNode(String n)       { this.evNextNode = n; }
    public List<String> getEvRoute()                { return evRoute;    }
    public void       setEvRoute(List<String> r)    { this.evRoute = r;  }
    public boolean    isRouteDone()                 { return routeDone;  }
    public void       setRouteDone(boolean b)       { this.routeDone = b; }
}
