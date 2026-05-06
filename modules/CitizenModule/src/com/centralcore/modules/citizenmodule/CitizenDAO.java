package com.centralcore.modules.citizenmodule;

import com.centralcore.db.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//acceso a datos para ciudadanos y sus documentos
//reutiliza la conexion h2 del core via DatabaseConnection
public class CitizenDAO {

    //crea las tablas del modulo si no existen
    public static void initSchema() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        try (Statement stmt = conn.createStatement()) {

            //extiende la tabla ciudadanos existente si le faltan columnas nuevas
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS ciudadanos (
                            id              INT          AUTO_INCREMENT PRIMARY KEY,
                            dni             VARCHAR(20)  NOT NULL UNIQUE,
                            nombre          VARCHAR(100) NOT NULL,
                            apellidos       VARCHAR(150) NOT NULL,
                            fecha_nac       DATE         NOT NULL,
                            lugar_nac       VARCHAR(100),
                            nacionalidad    VARCHAR(80),
                            sexo            CHAR(1)      NOT NULL DEFAULT 'M',
                            direccion       VARCHAR(255),
                            municipio       VARCHAR(100),
                            codigo_postal   VARCHAR(10),
                            telefono        VARCHAR(20),
                            email           VARCHAR(150),
                            estado_civil    VARCHAR(30),
                            activo          BOOLEAN      NOT NULL DEFAULT TRUE,
                            created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            //columnas que el schema original del core no tenia
            //ALTER IF NOT EXISTS es h2-safe
            String[] newCols = {
                    "ALTER TABLE ciudadanos ADD COLUMN IF NOT EXISTS lugar_nac VARCHAR(100)",
                    "ALTER TABLE ciudadanos ADD COLUMN IF NOT EXISTS nacionalidad VARCHAR(80)",
                    "ALTER TABLE ciudadanos ADD COLUMN IF NOT EXISTS codigo_postal VARCHAR(10)",
                    "ALTER TABLE ciudadanos ADD COLUMN IF NOT EXISTS estado_civil VARCHAR(30)",
                    "ALTER TABLE ciudadanos ADD COLUMN IF NOT EXISTS sexo CHAR(1) NOT NULL DEFAULT 'M'"
            };
            for (String sql : newCols) {
                try {
                    stmt.execute(sql);
                } catch (SQLException ignored) {
                }
            }

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS ciudadano_documentos (
                            id              INT          AUTO_INCREMENT PRIMARY KEY,
                            ciudadano_id    INT          NOT NULL,
                            tipo_documento  VARCHAR(80)  NOT NULL,
                            nombre_archivo  VARCHAR(255) NOT NULL,
                            ruta_archivo    VARCHAR(512) NOT NULL,
                            subido_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (ciudadano_id) REFERENCES ciudadanos(id) ON DELETE CASCADE
                        )
                    """);

            System.out.println("citizen module schema ready");

        } catch (SQLException e) {
            System.err.println("error initializing citizen schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //ciudadanos

    public List<Citizen> getAll() {
        List<Citizen> list = new ArrayList<>();
        String sql = "SELECT * FROM ciudadanos ORDER BY apellidos, nombre";
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return list;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("error fetching citizens: " + e.getMessage());
        }
        return list;
    }

    public List<Citizen> search(String query) {
        List<Citizen> list = new ArrayList<>();
        String q = "%" + query.toLowerCase() + "%";
        String sql = """
                    SELECT * FROM ciudadanos
                    WHERE LOWER(nombre) LIKE ? OR LOWER(apellidos) LIKE ? OR LOWER(dni) LIKE ?
                       OR LOWER(email) LIKE ? OR LOWER(municipio) LIKE ?
                    ORDER BY apellidos, nombre
                """;
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return list;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setString(i, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("error searching citizens: " + e.getMessage());
        }
        return list;
    }

    public void insert(Citizen c) throws SQLException {
        String sql = """
                    INSERT INTO ciudadanos
                    (dni, nombre, apellidos, fecha_nac, lugar_nac, nacionalidad, sexo,
                     direccion, municipio, codigo_postal, telefono, email, estado_civil, activo)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        Connection conn = DatabaseConnection.getConnection();
        assert conn != null;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, c);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
        }
    }

    public void update(Citizen c) throws SQLException {
        String sql = """
                    UPDATE ciudadanos SET
                        dni=?, nombre=?, apellidos=?, fecha_nac=?, lugar_nac=?, nacionalidad=?,
                        sexo=?, direccion=?, municipio=?, codigo_postal=?, telefono=?,
                        email=?, estado_civil=?, activo=?, updated_at=CURRENT_TIMESTAMP
                    WHERE id=?
                """;
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, c);
            ps.setInt(15, c.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ciudadanos WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    //documentos

    public List<CitizenDocument> getDocuments(int citizenId) {
        List<CitizenDocument> list = new ArrayList<>();
        String sql = "SELECT * FROM ciudadano_documentos WHERE ciudadano_id=? ORDER BY subido_en DESC";
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return list;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, citizenId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapDoc(rs));
            }
        } catch (SQLException e) {
            System.err.println("error fetching documents: " + e.getMessage());
        }
        return list;
    }

    public void insertDocument(CitizenDocument doc) throws SQLException {
        String sql = """
                    INSERT INTO ciudadano_documentos (ciudadano_id, tipo_documento, nombre_archivo, ruta_archivo)
                    VALUES (?,?,?,?)
                """;
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, doc.getCitizenId());
            ps.setString(2, doc.getTipoDocumento());
            ps.setString(3, doc.getNombreArchivo());
            ps.setString(4, doc.getRutaArchivo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) doc.setId(keys.getInt(1));
            }
        }
    }

    public void deleteDocument(int docId) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ciudadano_documentos WHERE id=?")) {
            ps.setInt(1, docId);
            ps.executeUpdate();
        }
    }

    //mappers

    private Citizen map(ResultSet rs) throws SQLException {
        Citizen c = new Citizen();
        c.setId(rs.getInt("id"));
        c.setDni(rs.getString("dni"));
        c.setNombre(rs.getString("nombre"));
        c.setApellidos(rs.getString("apellidos"));
        Date fechaNac = rs.getDate("fecha_nac");
        if (fechaNac != null) c.setFechaNacimiento(fechaNac.toLocalDate());
        c.setLugarNacimiento(rs.getString("lugar_nac"));
        c.setNacionalidad(rs.getString("nacionalidad"));
        c.setSexo(rs.getString("sexo"));
        c.setDireccion(rs.getString("direccion"));
        c.setMunicipio(rs.getString("municipio"));
        c.setCodigoPostal(rs.getString("codigo_postal"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setEstadoCivil(rs.getString("estado_civil"));
        c.setActivo(rs.getBoolean("activo"));
        return c;
    }

    private CitizenDocument mapDoc(ResultSet rs) throws SQLException {
        CitizenDocument d = new CitizenDocument();
        d.setId(rs.getInt("id"));
        d.setCitizenId(rs.getInt("ciudadano_id"));
        d.setTipoDocumento(rs.getString("tipo_documento"));
        d.setNombreArchivo(rs.getString("nombre_archivo"));
        d.setRutaArchivo(rs.getString("ruta_archivo"));
        Timestamp ts = rs.getTimestamp("subido_en");
        if (ts != null) d.setSubidoEn(ts.toLocalDateTime());
        return d;
    }

    private void bind(PreparedStatement ps, Citizen c) throws SQLException {
        ps.setString(1, c.getDni());
        ps.setString(2, c.getNombre());
        ps.setString(3, c.getApellidos());
        ps.setDate(4, c.getFechaNacimiento() != null ? Date.valueOf(c.getFechaNacimiento()) : null);
        ps.setString(5, c.getLugarNacimiento());
        ps.setString(6, c.getNacionalidad());
        ps.setString(7, c.getSexo() != null ? c.getSexo() : "M");
        ps.setString(8, c.getDireccion());
        ps.setString(9, c.getMunicipio());
        ps.setString(10, c.getCodigoPostal());
        ps.setString(11, c.getTelefono());
        ps.setString(12, c.getEmail());
        ps.setString(13, c.getEstadoCivil());
        ps.setBoolean(14, c.isActivo());
    }
}