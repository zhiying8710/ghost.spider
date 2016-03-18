package me.binge.ghost.spiders.utils;

import java.util.Properties;

public class Config {

    private static final Properties props = new Properties();

    static {
        try {
            props.load(ClassLoader.getSystemResourceAsStream("config.properties"));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

}
