package com.centralcore.modules;

//mapea el module.json de cada modulo, gson lo deserializa directamente
public class ModuleConfig {
    public String id;
    public String name;
    public String version;
    public String description;
    public String logoPath;
    public String mainClass;
    public String author;

    public ModuleConfig() {
    }

    @Override
    public String toString() {
        return "ModuleConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", logoPath='" + logoPath + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", author='" + author + '\'' +
                '}';
    }
}