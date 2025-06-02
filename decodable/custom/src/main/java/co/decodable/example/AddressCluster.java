package co.decodable.example;

public class AddressCluster {
    private String address;
    private String cluster;

    // Default constructor required for Jackson
    public AddressCluster() {}

    public AddressCluster(String address, String cluster) {
        this.address = address;
        this.cluster = cluster;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    @Override
    public String toString() {
        return "AddressCluster{" +
                "address='" + address + '\'' +
                ", cluster='" + cluster + '\'' +
                '}';
    }
}

class AddressClusterCDC {
    private AddressCluster before;
    private AddressCluster after;
    private String op;

    public AddressCluster getBefore() {
        return before;
    }

    public void setBefore(AddressCluster before) {
        this.before = before;
    }

    public AddressCluster getAfter() {
        return after;
    }

    public void setAfter(AddressCluster after) {
        this.after = after;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    @Override
    public String toString() {
        return "AddressClusterCDC{" +
                "before=" + before +
                ", after=" + after +
                ", operation='" + op + '\'' +
                '}';
    }
} 