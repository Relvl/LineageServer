package net.sf.l2j.commons.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Johnson / 15.07.2017
 */
public class DatabaseConfig {
    /**  */
    @JacksonXmlProperty(localName = "Driver")
    public String driver = "org.postgresql.Driver";
    /**  */
    @JacksonXmlProperty(localName = "URL")
    public String url = "jdbc:postgresql://localhost:5432/newera_login";
    /**  */
    @JacksonXmlProperty(localName = "User")
    public String user = "postgres";
    /**  */
    @JacksonXmlProperty(localName = "Password")
    public String password = "postgres";
    /**  */
    @JacksonXmlProperty(localName = "MaxConnections")
    public Integer maxConnections = 10;
    /**  */
    @JacksonXmlProperty(localName = "MaxIdleTime")
    public Integer maxIdleTime = 0;

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "driver='" + driver + '\'' +
                ", url='" + url + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", maxConnections=" + maxConnections +
                ", maxIdleTime=" + maxIdleTime +
                '}';
    }
}
