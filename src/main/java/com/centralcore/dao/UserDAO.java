package com.centralcore.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

import com.centralcore.db.DatabaseConnection;
import com.centralcore.model.User;

/**
 * objeto de acceso a datos para operaciones de base de datos relacionadas con usuarios
 *
 * gestiona la autenticacion, consulta y creacion de usuarios
 */
public class UserDAO {

    /**
     * autentica un usuario por email y contraseña
     *
     * devuelve el objeto User si las credenciales son validas, null en caso contrario
     *
     * @param email    el email del usuario
     * @param password la contraseña en texto plano a verificar
     */
    public User authenticate(String email, String password) {
        //consulta para encontrar usuario por email
        String sql = "SELECT id, username, email, password_hash, role, active FROM users WHERE email = ? AND active = true";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.err.println("sin conexion a bd para autenticacion");
            return null;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                // comparar la contraseña proporcionada con el hash bcrypt almacenado
                if (BCrypt.checkpw(password, storedHash)) {
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
            System.err.println("fallo la consulta de autenticacion: " + e.getMessage());
        }

        //email o contraseña incorrectos
        return null;
    }

    /**
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
            System.err.println("findById fallo: " + e.getMessage());
        }

        return null;
    }

    /**
     * hashea una contraseña en texto plano usando bcrypt
     *
     * usar esto al crear o actualizar usuarios
     *
     * @param plainPassword la contraseña a hashear
     * @return el hash bcrypt
     */
    public static String hashPassword(String plainPassword) {
        // factor de trabajo bcrypt 12 - buen equilibrio entre seguridad y velocidad
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
}
