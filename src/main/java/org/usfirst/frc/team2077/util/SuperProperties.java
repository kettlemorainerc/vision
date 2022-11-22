package org.usfirst.frc.team2077.util;

import java.io.*;
import java.lang.module.*;
import java.net.*;
import java.util.*;
import java.util.function.Function;
import org.slf4j.*;

public class SuperProperties {
    private static final Logger logger = LoggerFactory.getLogger(SuperProperties.class);

    private final Properties from;
    private final String prefix;

    public SuperProperties(Properties from, String prefix) {
        this.from = from;
        this.prefix = prefix;
    }

    public SuperProperties(Properties from) {this(from, "");}
    public SuperProperties(SuperProperties basis, String... prefix) {
        this(basis.from, makePrefix(basis.prefix, prefix));
    }

    public SuperProperties unprefixed() {return new SuperProperties(from);}

    public String get(String key) {
        return get(key, null);
    }

    public void put(String key, Object value) {
        from.put(prefix + key, value);
    }

    public String get(String key, String def) {
        key = prefix + key;
        if(from.containsKey(key)) return from.getProperty(key, def);

        return def;
    }

    public String[] getDelimited(String key, String regex) {
        return get(key).split(regex);
    }

    private <T> T parseValue(String key, T def, Function<String, T> parse) {
        String val = get(key);
        if(val == null || val.isBlank()) return def;

        return parse.apply(val);
    }

    public boolean getBoolean(String key, boolean def) {
        return parseValue(key, def, Boolean::parseBoolean);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public int getInt(String key, int def) {
        return parseValue(key, def, Integer::parseInt);
    }

    public Integer getInteger(String key) {
        return parseValue(key, null, Integer::parseInt);
    }

    public Double getDouble(String key, Double def) {
        return parseValue(key, def, Double::parseDouble);
    }

    public Double getDouble(String key) {
        return parseValue(key, null, Double::parseDouble);
    }

    public Class<?> getReferencedClass(String key) throws ClassNotFoundException {
        String clazz = get(key, "");
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            logger.info("Failed to retrieve \"{}{}\" as a class [value={}]", prefix, key, clazz);
            throw e;
        }
    }

    private static String makePrefix(String a, String... strings) {
        StringBuilder base = new StringBuilder();

        if(a != null && !a.isBlank()) {
            base.append(a);
            if(!a.endsWith(".")) base.append('.');
        }

        for(String s : strings) {
            if(s != null) {
                base.append(s);
                if(!s.endsWith(".")) base.append(".");
            }
        }

        return base.toString();
    }

    private static void findClasses(List<Class<?>> found, String endingWith, File withinFolder, String currentPackage) {
        File[] children;
        if(!withinFolder.exists() || !withinFolder.isDirectory() || (children = withinFolder.listFiles()) == null) return;

        String trueTarget = endingWith + ".class";
        logger.warn("Searching {} for .class files", withinFolder);
        for(File child : children) {
            if(child.isDirectory()) {
                findClasses(found, endingWith, child, currentPackage + "." + child.getName());
            } else if(child.getName().endsWith(trueTarget)) {
                String name = child.getName();
                String clazzName = currentPackage + "." + name.substring(0, name.length() - ".class".length());
                try {
                    Class<?> clazz = Class.forName(clazzName);
                    found.add(clazz);
                } catch (ClassNotFoundException e) {
                    logger.info("Failed to get class as {}", clazzName);
                }
            }
        }
    }
}
