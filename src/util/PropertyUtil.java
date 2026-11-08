package util;

import exception.DataAccessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class PropertyUtil {
    private static final String PROPERTY_FILE = "db.properties";

    private PropertyUtil() {
    }

    public static String getPropertyString() {
        return getRequiredProperty("db.url");
    }

    public static String getRequiredProperty(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        Properties properties = loadProperties();
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new DataAccessException("Missing required database property: " + key);
        }
        return value.trim();
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        String configuredPath = System.getenv("MEDICART_DB_PROPERTIES");
        if (configuredPath != null && !configuredPath.isBlank()) {
            Path externalPath = Path.of(configuredPath);
            if (!Files.isRegularFile(externalPath)) {
                throw new DataAccessException(
                        "Database properties file does not exist: " + externalPath);
            }
            return loadFile(properties, externalPath);
        }

        Path propertyPath = Path.of("resources", PROPERTY_FILE);
        if (Files.isRegularFile(propertyPath)) {
            return loadFile(properties, propertyPath);
        }

        try (InputStream classpathStream =
                     PropertyUtil.class.getClassLoader().getResourceAsStream(PROPERTY_FILE)) {
            if (classpathStream != null) {
                properties.load(classpathStream);
                return properties;
            }
        } catch (IOException exception) {
            throw new DataAccessException("Unable to read database properties", exception);
        }

        throw new DataAccessException(
                "Database properties were not found at resources/db.properties or on the classpath");
    }

    private static Properties loadFile(Properties properties, Path path) {
        try (InputStream fileStream = Files.newInputStream(path)) {
            properties.load(fileStream);
            return properties;
        } catch (IOException exception) {
            throw new DataAccessException("Unable to read database properties", exception);
        }
    }
}
