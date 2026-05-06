package com.centralcore.model;

import javafx.beans.property.*;

public class Licence {
    private final StringProperty module  = new SimpleStringProperty();
    private final StringProperty key     = new SimpleStringProperty();
    private final StringProperty expiry  = new SimpleStringProperty();
    private final BooleanProperty active = new SimpleBooleanProperty();

    public Licence(String module, String key, String expiry, boolean active) {
        this.module.set(module);
        this.key.set(key);
        this.expiry.set(expiry);
        this.active.set(active);
    }

    public StringProperty moduleProperty()  { return module; }
    public StringProperty keyProperty()     { return key; }
    public StringProperty expiryProperty()  { return expiry; }
    public BooleanProperty activeProperty() { return active; }

    public String getModule()  { return module.get(); }
    public String getKey()     { return key.get(); }
    public String getExpiry()  { return expiry.get(); }
    public boolean isActive()  { return active.get(); }
}