package com.centralcore.modules;

/**
 * pojo para los archivos de configuracion module.json
 *
 * gson deserializa el json en este objeto
 */
public class ModuleConfig {
    public String id;
    public String name;
    public String version;
    public String description;
    public String logoPath;
    public String mainClass;
    public String author;

    public ModuleConfig() {}

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
