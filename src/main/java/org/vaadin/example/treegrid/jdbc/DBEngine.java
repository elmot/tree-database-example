package org.vaadin.example.treegrid.jdbc;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by elmot on 4/11/2017.
 */
public class DBEngine {
    private static BasicDataSource dataSource;

    private DBEngine() {
    }

    private static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DBEngine.class) {
                // Standard double check trick to avoid double initialization
                // in case of race conditions
                if (dataSource == null) {
                    dataSource = new BasicDataSource();
                    dataSource.setUrl("jdbc:hsqldb:mem:tododb");
                    dataSource.setUsername("SA");
                    dataSource.setPassword("");
                    try (Connection connection = dataSource.getConnection()) {
                        setupDatabase(connection);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return dataSource;
    }

    private static void setupDatabase(Connection connection) {
        try (
                Statement statement = connection.createStatement();
                InputStream stream = DBEngine.class.getResourceAsStream("PEOPLE.sql");
                Reader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder text = new StringBuilder();
            for (int c; (c = reader.read()) >= 0; ) {
                if (c == ';') {
                    statement.executeQuery(text.toString());
                    text.setLength(0);
                } else {
                    text.append((char) c);
                }

            }
            if (!"".equals(text.toString().trim())) {
                statement.executeQuery(text.toString());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
