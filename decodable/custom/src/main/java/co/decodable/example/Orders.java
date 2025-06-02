package co.decodable.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Orders {
    
    @JsonProperty("order_id")
    private Integer orderId;

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("order_item")
    private String orderItem;

    @JsonProperty("updated_ts")
    private String updatedTs;  // Keep as String for now

    // Default constructor required for Jackson
    public Orders() {}

    // Getters and Setters
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(String orderItem) {
        this.orderItem = orderItem;
    }

    public String getUpdatedTs() {
        return updatedTs;
    }

    public void setUpdatedTs(String updatedTs) {
        this.updatedTs = updatedTs;
    }

    @Override
    public String toString() {
        return "Orders{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", orderItem='" + orderItem + '\'' +
                ", updatedTs='" + updatedTs + '\'' +
                '}';
    }
}

class OrdersCDC {
    @JsonProperty("before")
    private Orders before;

    @JsonProperty("after")
    private Orders after;

    @JsonProperty("op")
    private String op;

    @JsonProperty("ts_ms")
    private Long tsMs;

    public Orders getBefore() {
        return before;
    }

    public void setBefore(Orders before) {
        this.before = before;
    }

    public Orders getAfter() {
        return after;
    }

    public void setAfter(Orders after) {
        this.after = after;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Long getTsMs() {
        return tsMs;
    }

    public void setTsMs(Long tsMs) {
        this.tsMs = tsMs;
    }

    public boolean isDelete() {
        return op != null && op.equals("d");
    }

    public boolean isInsert() {
        return op != null && (op.equals("i") || op.equals("c") || op.equals("u"));
    }

    @Override
    public String toString() {
        return "OrdersCDC{" +
                "before=" + before +
                ", after=" + after +
                ", operation='" + op + '\'' +
                ", timestamp=" + tsMs +
                '}';
    }
}
