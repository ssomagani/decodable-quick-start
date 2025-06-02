package co.decodable.example;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.table.factories.DynamicTableFactory;
import org.apache.flink.table.factories.Factory;
import org.apache.flink.table.module.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.ServiceLoader;

public class StreamingSQLJob {
    // Queue to hold input strings
    private static final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    // Queue to hold output strings
    private static final BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();
    
    public static void main(String[] args) {
        // Check if SQL query is provided as an argument
        if (args.length < 1) {
            System.err.println("Usage: StreamingSQLJob <sql_query>");
            System.err.println("Example: StreamingSQLJob \"SELECT UPPER(input_string) AS output_string FROM source_table\"");
            System.exit(1);
        }
        
        // Get the SQL query from command line arguments
        String sqlQuery = args[0];
        System.out.println("Using SQL query: " + sqlQuery);
        
        // 1. Set up Flink environments
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        Configuration configuration = new Configuration();
        EnvironmentSettings settings = EnvironmentSettings.newInstance()
            .inStreamingMode()
            .withConfiguration(configuration)
            .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);

        // Register our custom factories
        tableEnv.loadModule("custom", new Module() {
            @Override
            public Set<String> listFunctions() {
                return new HashSet<>();
            }

            public Set<Factory> getFactories() {
                Set<Factory> factories = new HashSet<>();
                factories.add(new CustomSourceFactory());
                factories.add(new CustomSinkFactory());
                return factories;
            }
        });

        // 2. Create a source table using custom source
        String createSource = 
            "CREATE TABLE source_table (" +
            "    input_string STRING" +
            ") WITH (" +
            "    'connector' = 'custom-source'" +
            ")";

        // 3. Create a sink table using custom sink
        String createSink = 
            "CREATE TABLE sink_table (" +
            "    output_string STRING" +
            ") WITH (" +
            "    'connector' = 'custom-sink'" +
            ")";

        // 4. SQL query: use the provided query
        String insertQuery = 
            "INSERT INTO sink_table " + sqlQuery;

        // 5. Execute the statements
        tableEnv.executeSql(createSource);
        tableEnv.executeSql(createSink);
        TableResult result = tableEnv.executeSql(insertQuery);

        // 6. Add some test input
        addInput("Hello World");
        addInput("This is a test");
        addInput("Streaming SQL with Flink");

        // 7. Process outputs
        processOutputs();

        // Optional: wait for job to finish (this keeps it running)
        result.getJobClient().ifPresent(job -> {
            try {
                job.getJobExecutionResult().get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Add a string to the input queue
     * @param input The input string to process
     */
    public static void addInput(String input) {
        try {
            inputQueue.put(input);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Process outputs from the queue
     */
    public static void processOutputs() {
        new Thread(() -> {
            while (true) {
                try {
                    String output = outputQueue.take();
                    System.out.println("Output: " + output);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
    
    /**
     * Custom source function that reads from the input queue
     */
    public static class StringSourceFunction extends RichSourceFunction<RowData> {
        private volatile boolean isRunning = true;
        
        @Override
        public void open(Configuration parameters) {
            // Setup code if needed
        }
        
        @Override
        public void run(SourceContext<RowData> ctx) throws Exception {
            while (isRunning) {
                String input = inputQueue.take();
                ctx.collect(GenericRowData.of(StringData.fromString(input)));
            }
        }
        
        @Override
        public void cancel() {
            isRunning = false;
        }
    }
    
    /**
     * Custom sink function that writes to the output queue
     */
    public static class StringSinkFunction extends RichSinkFunction<RowData> {
        @Override
        public void open(Configuration parameters) {
            // Setup code if needed
        }
        
        @Override
        public void invoke(RowData value, Context context) throws Exception {
            String output = value.getString(0).toString();
            outputQueue.put(output);
        }
    }
}
