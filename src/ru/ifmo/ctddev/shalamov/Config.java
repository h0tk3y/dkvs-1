package ru.ifmo.ctddev.shalamov;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class Config {
    Map<Integer, String> addresses;
    long timeout;

    public static final String CHARSET = "UTF-8";

    public Config(Map map, int timeout) {
        this.addresses = map;
        this.timeout = timeout;
    }


    /**
     * Provides port, listened by node @code{id}
     *
     * @param id
     * @return
     */
    public int port(int id) {
        if (!addresses.containsKey(id))
            return -1;
        String[] parts = addresses.get(id).split(":");
        return Integer.parseInt(parts[1]);
    }

    /**
     * Provides the address of node @code{id}
     *
     * @param id
     * @return
     */
    public String address(int id) {
        if (!addresses.containsKey(id))
            return null;
        String[] parts = addresses.get(id).split(":");
        return parts[0];
    }


    /**
     * number of nodes in configuration.
     *
     * @return
     */
    public int nodesCount() {
        return addresses.size();
    }

    /**
     * Return Leader's ids. just for now - all nodes are leaders.
     *
     * @return
     */
    public List<Integer> ids() {
        return range(0, nodesCount() - 1);
    }

    public static List<Integer> range(int min, int max) {
        List<Integer> list = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            list.add(i);
        }
        return list;
    }

    /**
     * Reads properties from disk and created Config from them.
     *
     * @return
     * @throws IOException
     */
    public static Config readDkvsProperties() throws IOException {

        String CONFIG_PROPERTIES_NAME = "resources/dkvs.properties";
        String NODE_ADDRESS_PREFIX = "node";

        InputStream inputStream = Config.class.getClassLoader().getResourceAsStream(CONFIG_PROPERTIES_NAME);
        Properties properties = new Properties();
        if (inputStream != null) {
            properties.load(inputStream);
        } else {
            throw new FileNotFoundException("property file '" + CONFIG_PROPERTIES_NAME + "' not found in the classpath");
        }


        int timeout = Integer.parseInt(properties.getProperty("timeout"));

        HashMap<Integer, String> hm = new HashMap<>();


        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (!(entry.getKey() instanceof String && entry.getValue() instanceof String))
                continue;
            String k = (String) entry.getKey();
            String v = (String) entry.getValue();
            if (k.startsWith(NODE_ADDRESS_PREFIX)) {
                String[] parts = k.split("\\.");
                int id = Integer.parseInt(parts[1]);
                hm.put(id, v);
            }
        }

        return new Config(Collections.unmodifiableMap(hm), timeout);
    }
}


