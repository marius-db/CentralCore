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

class SecurityTests {

    private Connection testConn;

    @BeforeEach
    void setUp() throws Exception {
        testConn = DriverManager.getConnection("jdbc:h2:mem:securitydb;DB_CLOSE_DELAY=-1", "sa", "");
        Field connField = DatabaseConnection.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(null, testConn);
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
    void authenticate_sqlInjection_returnsNull() {
        //inyeccion sql clasica en campo email no debe bypassear la autenticacion
        UserDAO dao = new UserDAO();
        dao.register("testuser", "test@centralcore.com", "password123");
        User user = dao.authenticate("' OR '1'='1", "anypassword");
        assertNull(user);
    }

    @Test
    void register_sqlInjectionInName_storesLiterally() {
        //payload de inyeccion en nombre debe almacenarse como texto y no alterar el esquema
        UserDAO dao = new UserDAO();
        boolean result = dao.register("Robert'); DROP TABLE users;--", "sqltest@centralcore.com", "password123");
        assertTrue(result);
        User user = dao.authenticate("sqltest@centralcore.com", "password123");
        assertNotNull(user);
        assertEquals("Robert'); DROP TABLE users;--", user.getUsername());
    }

    @Test
    void authenticate_sqlInjectionInPassword_returnsNull() {
        //inyeccion en campo contrasena no produce autenticacion gracias a bcrypt
        UserDAO dao = new UserDAO();
        dao.register("testuser", "test@centralcore.com", "password123");
        User user = dao.authenticate("test@centralcore.com", "' OR '1'='1");
        assertNull(user);
    }
}