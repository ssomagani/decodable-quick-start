package co.decodable.example;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class StreamingSQLJob {
    public static void main(String[] args) {
        // 1. Set up Flink environments
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);

        // 2. Create a source table using DataGen connector
        String createSource = 
            "CREATE TABLE source_table (" +
            "    id INT," +
            "    name STRING" +
            ") WITH (" +
            "    'connector' = 'values'," +
            "    'data' = '[" +
            "        {\"id\": 1, \"name\": \"Alice\"}," +
            "        {\"id\": 2, \"name\": \"Bob\"}," +
            "        {\"id\": 3, \"name\": \"Carol\"}," +
            "        {\"id\": 4, \"name\": \"Dave\"}," +
            "        {\"id\": 5, \"name\": \"Eve\"}" +
            "    ]'" +
            ")";

        // 3. Create a sink table using the Print connector
        String createSink = 
            "CREATE TABLE sink_table (" +
            "    id INT," +
            "    name STRING" +
            ") WITH (" +
            "    'connector' = 'print'" +
            ")";

        // 4. SQL query: filter even ids
        String insertQuery = 
            "INSERT INTO sink_table " +
            "SELECT id, name FROM source_table WHERE MOD(id, 2) = 0";

        // 5. Execute the statements
        tableEnv.executeSql(createSource);
        tableEnv.executeSql(createSink);
        TableResult result = tableEnv.executeSql(insertQuery);

        // Optional: wait for job to finish (this keeps it running)
        result.getJobClient().ifPresent(job -> {
            try {
                job.getJobExecutionResult().get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
