package com.centralcore.dao;

import com.centralcore.db.DatabaseConnection;
import com.centralcore.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * data access object for user-related database operations
 * objeto de acceso a datos para operaciones de base de datos relacionadas con usuarios
 *
 * handles authentication, fetching and creating users
 * gestiona la autenticacion, consulta y creacion de usuarios
 */
public class UserDAO {

    /**
     * authenticates a user by email and password
     * autentica un usuario por email y contraseña
     *
     * returns the User object if credentials are valid, null otherwise
     * devuelve el objeto User si las credenciales son validas, null en caso contrario
     *
     * @param email    the user's email / el email del usuario
     * @param password the plain text password to check / la contraseña en texto plano a verificar
     */
    public User authenticate(String email, String password) {
        // query to find user by email / consulta para encontrar usuario por email
        String sql = "SELECT id, username, email, password_hash, role, active FROM users WHERE email = ? AND active = 1";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.err.println("no db connection for authentication / sin conexion a bd para autenticacion");
            return null;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                // compare the provided password against the stored bcrypt hash
                // comparar la contraseña proporcionada con el hash bcrypt almacenado
                if (BCrypt.checkpw(password, storedHash)) {
                    // password matches - build and return the user object
                    // la contraseña coincide - construir y devolver el objeto usuario
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setPasswordHash(storedHash);
                    user.setRole(rs.getString("role"));
                    user.setActive(rs.getBoolean("active"));
                    return user;
                }
            }

        } catch (SQLException e) {
            System.err.println("authentication query failed / fallo la consulta de autenticacion: " + e.getMessage());
        }

        // wrong email or password / email o contraseña incorrectos
        return null;
    }

    /**
     * finds a user by their id
     * encuentra un usuario por su id
     */
    public User findById(int id) {
        String sql = "SELECT id, username, email, role, active FROM users WHERE id = ?";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("role"),
                    rs.getBoolean("active")
                );
            }

        } catch (SQLException e) {
            System.err.println("findById failed / findById fallo: " + e.getMessage());
        }

        return null;
    }

    /**
     * hashes a plain text password using bcrypt
     * hashea una contraseña en texto plano usando bcrypt
     *
     * use this when creating or updating users
     * usar esto al crear o actualizar usuarios
     *
     * @param plainPassword the password to hash / la contraseña a hashear
     * @return the bcrypt hash / el hash bcrypt
     */
    public static String hashPassword(String plainPassword) {
        // bcrypt work factor 12 - good balance of security and speed
        // factor de trabajo bcrypt 12 - buen equilibrio entre seguridad y velocidad
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
}
