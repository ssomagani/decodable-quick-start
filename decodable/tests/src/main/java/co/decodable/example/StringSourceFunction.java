package co.decodable.example;

import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;

public class StringSourceFunction implements SourceFunction<RowData> {
    private volatile boolean isRunning = true;
    private final String hostname;
    private final int port;

    public StringSourceFunction(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public void run(SourceContext<RowData> ctx) throws Exception {
        while (isRunning) {
            // Create a sample row with hostname and port
            GenericRowData row = new GenericRowData(2);
            row.setField(0, StringData.fromString(hostname));
            row.setField(1, port);
            
            ctx.collect(row);
            
            // Sleep for a while to avoid flooding
            Thread.sleep(1000);
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
} 