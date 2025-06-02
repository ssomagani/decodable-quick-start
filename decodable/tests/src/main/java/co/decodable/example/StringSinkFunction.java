package co.decodable.example;

import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;

public class StringSinkFunction implements SinkFunction<RowData> {
    private final String hostname;
    private final int port;

    public StringSinkFunction(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public void invoke(RowData value, Context context) {
        StringData hostData = value.getString(0);
        int portValue = value.getInt(1);
        System.out.printf("Received row: hostname=%s, port=%d%n", hostData.toString(), portValue);
    }
} 