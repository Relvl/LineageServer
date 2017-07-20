package net.sf.l2j.commons.config;

import net.sf.l2j.commons.serialize.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * @author Johnson / 15.07.2017
 */
public class Config {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    /**  */
    @ConfigElement(fileName = "./config/network.xml", doNotSaveDinamically = true)
    public NetworkConfig network = new NetworkConfig();

    /**  */
    @ConfigElement(fileName = "./config/database.xml", doNotSaveDinamically = true)
    public DatabaseConfig database = new DatabaseConfig();

    /**  */
    @ConfigElement(fileName = "./config/database_new.xml", doNotSaveDinamically = true)
    public DatabaseConfig database_new = new DatabaseConfig();

    /**  */
    @ConfigElement(fileName = "./config/mmocore.xml", doNotSaveDinamically = true)
    public MMOCoreConfigImpl mmocore = new MMOCoreConfigImpl();

    public final Config load() {
        for (Field field : getClass().getFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ConfigElement.class)) {
                ConfigElement configElement = field.getAnnotation(ConfigElement.class);
                try {
                    Object config = Serializer.MAPPER.readValue(new File(configElement.fileName()), field.getType());
                    field.set(this, config);
                }
                catch (IllegalAccessException | IOException e) {
                    LOGGER.error("Failed to load config {}, rewrited.", field.getName(), e);
                    try {
                        Serializer.MAPPER.writeValue(new File(configElement.fileName()), field.get(this));
                    }
                    catch (IOException | IllegalAccessException e1) {
                        LOGGER.error("Failed to save default config {}.", field.getName(), e1);
                    }
                }
            }
        }
        LOGGER.debug("Configuration loaded {}.", this);

        return this;
    }

    public final void save(boolean force) {
        for (Field field : getClass().getFields()) {
            try {
                field.setAccessible(true);
                if (field.isAnnotationPresent(ConfigElement.class)) {
                    ConfigElement configElement = field.getAnnotation(ConfigElement.class);
                    if (force || !configElement.doNotSaveDinamically()) {
                        Serializer.MAPPER.writeValue(new File(configElement.fileName()), field.get(this));
                    }
                }
            }
            catch (IllegalAccessException | IOException e) {
                LOGGER.error("Failed to save config {}", field.getName(), e);
            }
        }
        LOGGER.debug("Configuration saved.");
    }

    @Override
    public String toString() {
        return "Config{" +
                "network=" + network +
                ", database=" + database +
                ", mmocore=" + mmocore +
                '}';
    }
}
