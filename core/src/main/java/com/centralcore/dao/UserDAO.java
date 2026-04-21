package com.centralcore.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

import com.centralcore.db.DatabaseConnection;
import com.centralcore.model.User;

public class UserDAO {

    public User authenticate(String email, String password) {
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

                if (BCrypt.checkpw(password, storedHash)) {
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

        return null;
    }

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

    //factor de trabajo 12, subir esto si el hardware lo aguanta
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
}