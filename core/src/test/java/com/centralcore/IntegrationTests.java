package com.centralcore;

import com.centralcore.dao.UserDAO;
import com.centralcore.db.DatabaseConnection;
import com.centralcore.db.SchemaInitializer;
import com.centralcore.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTests {

    //conexion h2 en memoria, se crea y destruye en cada test
    private Connection testConn;

    @BeforeEach
    void setUp() throws Exception {
        //base de datos en memoria, cada test arranca desde cero
        testConn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");

        //inyectar la conexion de test en el singleton via reflexion
        Field connField = DatabaseConnection.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(null, testConn);

        //inicializar el schema del core sobre la conexion de test
        SchemaInitializer.initialize(testConn);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testConn != null && !testConn.isClosed()) {
            testConn.createStatement().execute("DROP ALL OBJECTS");
            testConn.close();
        }
    }

    @Test
    void connection_testConnection_returnsTrue() {
        //la conexion activa debe responder como valida
        assertTrue(DatabaseConnection.testConnection());
    }

    @Test
    void connection_pingConnection_returnsTrue() {
        //el ping sobre una conexion abierta debe devolver true
        assertTrue(DatabaseConnection.pingConnection());
    }

    @Test
    void user_register_returnsTrue() {
        //registrar un usuario nuevo debe devolver true
        UserDAO dao = new UserDAO();
        assertTrue(dao.register("testuser", "test@centralcore.com", "password123"));
    }

    @Test
    void user_authenticate_validCredentials_returnsUser() {
        //autenticar con credenciales correctas debe devolver el usuario
        UserDAO dao = new UserDAO();
        dao.register("testuser", "test@centralcore.com", "password123");
        User user = dao.authenticate("test@centralcore.com", "password123");
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
    }

    @Test
    void user_authenticate_wrongPassword_returnsNull() {
        //autenticar con contrasena incorrecta debe devolver null
        UserDAO dao = new UserDAO();
        dao.register("testuser", "test@centralcore.com", "password123");
        User user = dao.authenticate("test@centralcore.com", "wrongpassword");
        assertNull(user);
    }

    @Test
    void user_findById_returnsCorrectUser() {
        //buscar un usuario por id debe devolver el registro correcto
        UserDAO dao = new UserDAO();
        dao.register("testuser", "test@centralcore.com", "password123");
        User registered = dao.authenticate("test@centralcore.com", "password123");
        User found = dao.findById(registered.getId());
        assertNotNull(found);
        assertEquals("test@centralcore.com", found.getEmail());
    }
}