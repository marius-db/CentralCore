package com.centralcore.model;

/**
 * model class representing a system user
 * clase modelo que representa un usuario del sistema
 *
 * maps directly to the 'users' table in the database
 * mapea directamente a la tabla 'users' en la base de datos
 */
public class User {

    private int    id;
    private String username;
    private String email;
    private String passwordHash; // bcrypt hash - never store plain text / hash bcrypt - nunca almacenar texto plano
    private String role;         // e.g. "admin", "operator" / ej. "admin", "operador"
    private boolean active;

    // --- constructors / constructores ---

    public User() {}

    public User(int id, String username, String email, String role, boolean active) {
        this.id       = id;
        this.username = username;
        this.email    = email;
        this.role     = role;
        this.active   = active;
    }

    // --- getters and setters / getters y setters ---

    public int getId()                   { return id; }
    public void setId(int id)            { this.id = id; }

    public String getUsername()                  { return username; }
    public void setUsername(String username)     { this.username = username; }

    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }

    public String getPasswordHash()              { return passwordHash; }
    public void setPasswordHash(String hash)     { this.passwordHash = hash; }

    public String getRole()                      { return role; }
    public void setRole(String role)             { this.role = role; }

    public boolean isActive()                    { return active; }
    public void setActive(boolean active)        { this.active = active; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email + "', role='" + role + "'}";
    }
}
