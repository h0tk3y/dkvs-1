package ru.ifmo.ctddev.year2012.nets.lab4;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class Config {
    private final Map<Integer, String> addresses;
    private final long timeout;
    private final String anycastAddress;
    private final int anycastPort;

    public static final String CHARSET = "UTF-8";

    public Config(Map<Integer, String> map, int timeout, String anycastAddress, int anycastPort) {
        this.addresses = map;
        this.timeout = timeout;
        this.anycastAddress = anycastAddress;
        this.anycastPort = anycastPort;
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
        String[] parts = addresses.get(id).split("\\|");
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
        String[] parts = addresses.get(id).split("\\|");
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

    public long getTimeout() {
        return timeout;
    }

    public String getAnycastAddress() {
        return anycastAddress;
    }

    public int getAnycastPort() {
        return anycastPort;
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

        String CONFIG_PROPERTIES_NAME = "dkvs.properties";
        String NODE_ADDRESS_PREFIX = "node";

        InputStream inputStream = new FileInputStream("dkvs.properties");
        Properties properties = new Properties();
        properties.load(inputStream);

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

        int anycastPort = Integer.parseInt(properties.getProperty("anycastPort"));

        return new Config(Collections.unmodifiableMap(hm), timeout, properties.getProperty("anycastAddr"), anycastPort);
    }
}


