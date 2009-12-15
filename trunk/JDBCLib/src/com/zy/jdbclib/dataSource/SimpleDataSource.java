
package com.zy.jdbclib.dataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @version 1.0
 * @since 1.0
 */
public class SimpleDataSource extends AbstractDataSource {

    private String url;

    private String username;

    private String password;

    private Properties connectionProperties;

    public SimpleDataSource() {
    }

    public SimpleDataSource(String url) {
        setUrl(url);
    }

    public SimpleDataSource(String url, String username, String password) {
        setUrl(url);
        setUsername(username);
        setPassword(password);
    }

    public SimpleDataSource(String url, Properties conProps) {
        setUrl(url);
        setConnectionProperties(conProps);
    }

    public void setDriverClassName(String driverClassName) {

        String driverClassNameToUse = driverClassName.trim();
        try {
            Class.forName(driverClassNameToUse);
        } catch (ClassNotFoundException ex) {
            IllegalStateException ise = new IllegalStateException(
                    "Could not load JDBC driver class [" + driverClassNameToUse + "]");
            ise.initCause(ex);
            throw ise;
        }
        logger.info("Loaded JDBC driver: " + driverClassNameToUse);
    }

    protected Connection getConnectionFromDriver(Properties props) throws SQLException {
        String url = getUrl();
        logger.debug("Creating new JDBC DriverManager Connection to [" + url + "]");
        return getConnectionFromDriverManager(url, props);
    }

    protected Connection getConnectionFromDriverManager(String url, Properties props)
            throws SQLException {
        return DriverManager.getConnection(url, props);
    }

    public Connection getConnection() throws SQLException {
        return getConnection(getUsername(), getPassword());
    }

    public Connection getConnection(String username, String password) throws SQLException {
        Properties props = new Properties(getConnectionProperties());
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        return getConnectionFromDriverManager(getUrl(), props);
    }

    public void loadConfigFromFile(File file) throws IOException {
        Properties config = new Properties();
        config.load(new FileInputStream(file));
        setDriverClassName(config.getProperty("DriverClass"));
        setUrl(config.getProperty("Url"));
        setUsername(config.getProperty("user"));
        setPassword(config.getProperty("password"));
        connectionProperties = new Properties(config);
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the connectionProperties
     */
    public Properties getConnectionProperties() {
        return connectionProperties;
    }

    /**
     * @param connectionProperties the connectionProperties to set
     */
    public void setConnectionProperties(Properties connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

}
