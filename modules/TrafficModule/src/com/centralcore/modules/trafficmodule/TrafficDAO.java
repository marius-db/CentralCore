package com.centralcore.modules.trafficmodule;

import com.centralcore.db.DatabaseConnection;
import com.centralcore.modules.trafficmodule.model.Incident;
import com.centralcore.modules.trafficmodule.model.IncidentUpdate;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//acceso a datos para incidentes de trafico y su historial de actualizaciones
public class TrafficDAO {

    //crea las tablas del modulo si no existen
    public static void initSchema() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        try (Statement stmt = conn.createStatement()) {

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS traffic_incidents (
                            id          INT           AUTO_INCREMENT PRIMARY KEY,
                            tipo        VARCHAR(80)   NOT NULL,
                            descripcion VARCHAR(1000),
                            map_x       DECIMAL(10,3) NOT NULL DEFAULT 0,
                            map_y       DECIMAL(10,3) NOT NULL DEFAULT 0,
                            estado      VARCHAR(40)   NOT NULL DEFAULT 'Abierto',
                            created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            closed_at   TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS traffic_incident_updates (
                            id          INT           AUTO_INCREMENT PRIMARY KEY,
                            incident_id INT           NOT NULL,
                            estado      VARCHAR(40)   NOT NULL,
                            nota        VARCHAR(1000),
                            created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (incident_id) REFERENCES traffic_incidents(id) ON DELETE CASCADE
                        )
                    """);

            System.out.println("traffic module schema ready");

        } catch (SQLException e) {
            System.err.println("error al inicializar schema de trafico: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //incidentes activos (sin cerrar)

    public List<Incident> getActiveIncidents() {
        return queryIncidents("WHERE closed_at IS NULL ORDER BY created_at DESC");
    }

    //incidentes cerrados para el historial

    public List<Incident> getClosedIncidents() {
        return queryIncidents("WHERE closed_at IS NOT NULL ORDER BY closed_at DESC");
    }

    private List<Incident> queryIncidents(String whereClause) {
        List<Incident> list = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return list;

        String sql = "SELECT id, tipo, descripcion, map_x, map_y, estado, created_at, closed_at " +
                "FROM traffic_incidents " + whereClause;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("error al consultar incidentes: " + e.getMessage());
        }
        return list;
    }

    public void insertIncident(Incident i) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        String sql = "INSERT INTO traffic_incidents (tipo, descripcion, map_x, map_y, estado) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, i.getTipo());
            ps.setString(2, i.getDescripcion());
            ps.setDouble(3, i.getMapX());
            ps.setDouble(4, i.getMapY());
            ps.setString(5, i.getEstado() != null ? i.getEstado() : "Abierto");
            ps.executeUpdate();

            //recuperar id generado
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) i.setId(keys.getInt(1));
            }

            //crear la primera entrada del historial
            IncidentUpdate initial = new IncidentUpdate();
            initial.setIncidentId(i.getId());
            initial.setEstado(i.getEstado() != null ? i.getEstado() : "Abierto");
            initial.setNota("Incidente creado");
            insertUpdate(initial);

        } catch (SQLException e) {
            System.err.println("error al insertar incidente: " + e.getMessage());
        }
    }

    public void addUpdate(int incidentId, String newEstado, String nota) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        //actualizar estado del incidente
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE traffic_incidents SET estado = ? WHERE id = ?")) {
            ps.setString(1, newEstado);
            ps.setInt(2, incidentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("error al actualizar estado del incidente: " + e.getMessage());
        }

        //registrar la actualizacion en el historial
        IncidentUpdate u = new IncidentUpdate();
        u.setIncidentId(incidentId);
        u.setEstado(newEstado);
        u.setNota(nota);
        insertUpdate(u);
    }

    public void closeIncident(int incidentId) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE traffic_incidents SET estado = 'Cerrado', closed_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setInt(1, incidentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("error al cerrar incidente: " + e.getMessage());
        }

        //ultima entrada del historial
        IncidentUpdate u = new IncidentUpdate();
        u.setIncidentId(incidentId);
        u.setEstado("Cerrado");
        u.setNota("Incidente resuelto y cerrado");
        insertUpdate(u);
    }

    public void insertUpdate(IncidentUpdate u) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        String sql = "INSERT INTO traffic_incident_updates (incident_id, estado, nota) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, u.getIncidentId());
            ps.setString(2, u.getEstado());
            ps.setString(3, u.getNota());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) u.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("error al insertar actualizacion: " + e.getMessage());
        }
    }

    public List<IncidentUpdate> getUpdates(int incidentId) {
        List<IncidentUpdate> list = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return list;

        String sql = "SELECT id, incident_id, estado, nota, created_at " +
                "FROM traffic_incident_updates WHERE incident_id = ? ORDER BY created_at ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, incidentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IncidentUpdate u = new IncidentUpdate();
                    u.setId(rs.getInt("id"));
                    u.setIncidentId(rs.getInt("incident_id"));
                    u.setEstado(rs.getString("estado"));
                    u.setNota(rs.getString("nota"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            System.err.println("error al consultar actualizaciones: " + e.getMessage());
        }
        return list;
    }

    //helpers
    private Incident mapRow(ResultSet rs) throws SQLException {
        Incident i = new Incident();
        i.setId(rs.getInt("id"));
        i.setTipo(rs.getString("tipo"));
        i.setDescripcion(rs.getString("descripcion"));
        i.setMapX(rs.getDouble("map_x"));
        i.setMapY(rs.getDouble("map_y"));
        i.setEstado(rs.getString("estado"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) i.setCreatedAt(ca.toLocalDateTime());
        Timestamp cl = rs.getTimestamp("closed_at");
        if (cl != null) i.setClosedAt(cl.toLocalDateTime());
        return i;
    }
}
