package de.bornemisza.couchdb.entity;

public class SrvRecord implements Comparable<SrvRecord> {

    private final int priority;
    private final int weight;
    private final int port;
    private final String host;

    public SrvRecord(int priority, int weight, int port, String host) {
        this.priority = priority;
        this.weight = weight;
        this.port = port;
        this.host = host.replaceAll("\\\\.$", "");
    }

    public int getPriority() {
        return priority;
    }

    public int getWeight() {
        return weight;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public static SrvRecord fromString(String input) {
        String[] splitted = input.split(" ");
        return new SrvRecord(
                Integer.parseInt(splitted[0]),
                Integer.parseInt(splitted[1]),
                Integer.parseInt(splitted[2]),
                splitted[3]
        );
    }

    @Override
    public String toString() {
        return "SrvRecord{"
                + "priority=" + priority
                + ", weight=" + weight
                + ", port=" + port
                + ", host=" + host
                + "}";
    }

    @Override
    public int compareTo(SrvRecord o) {
        if (getPriority() < o.getPriority()) {
            return -1;
        } else {
            return 1;
        }
    }

}
