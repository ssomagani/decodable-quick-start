package co.decodable.example;

import static co.decodable.example.AddressClusterJob.ADDRESS_CLUSTER;
import static co.decodable.example.AddressClusterJob.ADDRESS_CLUSTER_COMPACT;

import java.util.Map;
import java.util.Properties;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.jackson.JacksonMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.decodable.sdk.pipeline.DecodableStreamSink;
import co.decodable.sdk.pipeline.DecodableStreamSource;
import co.decodable.sdk.pipeline.metadata.SinkStreams;
import co.decodable.sdk.pipeline.metadata.SourceStreams;

@SourceStreams(ADDRESS_CLUSTER)
@SinkStreams(ADDRESS_CLUSTER_COMPACT)
public class AddressClusterJob {

    private static final Logger LOG = LoggerFactory.getLogger(AddressClusterJob.class);

    static final String ADDRESS_CLUSTER = "tron-clustering-raw-unittest-stream-v1";
    static final String ADDRESS_CLUSTER_COMPACT = "tron_clustering_compact_unittest_stream";

    static final ObjectMapper OBJECT_MAPPER = JacksonMapperFactory.createObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private static boolean isDelete(AddressClusterCDC event) {
        return event.getOp() != null && event.getOp().equals("d");
    }

    private static boolean isInsert(AddressClusterCDC event) {
        return event.getOp() != null && (event.getOp().equals("i") || event.getOp().equals("c") || event.getOp().equals("u"));
    }

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DecodableStreamSource<AddressClusterCDC> source =
                DecodableStreamSource.<AddressClusterCDC>builder()
                        .withStreamName(ADDRESS_CLUSTER)
                        .withDeserializationSchema(new JsonDeserializationSchema<>(AddressClusterCDC.class, () -> OBJECT_MAPPER))
                        .build();

                DecodableStreamSink<AddressClusterCDC> sink =
                new DecodableStreamSinkChangeBuilder<AddressClusterCDC>()
                        .withStreamName(ADDRESS_CLUSTER_COMPACT)
                        .withSerializationSchema(new JsonSerializationSchema<>(() -> OBJECT_MAPPER))
                        .withKeySerializationSchema(new JsonSerializationSchema<>(() -> OBJECT_MAPPER))
                        .build();
       
        DataStream<AddressClusterCDC> stream =
                env.fromSource(source, WatermarkStrategy.noWatermarks(),
                        "[stream-test] Address Cluster Stream Source");

        stream
                .<String>keyBy(record -> {
                    if (isDelete(record)) {
                        return record.getBefore().getAddress();
                    } else {
                        return record.getAfter().getAddress();
                    }
                }) 
                .process(new KeyedProcessFunction<String, AddressClusterCDC, AddressClusterCDC>() {
                    private ValueState<AddressClusterCDC> deleteBuffer;
                    private ValueState<Long> timerState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        ValueStateDescriptor<AddressClusterCDC> descriptor = new ValueStateDescriptor<>("delete-buffer", AddressClusterCDC.class);
                        deleteBuffer = getRuntimeContext().getState(descriptor);
                        ValueStateDescriptor<Long> timerDescriptor = new ValueStateDescriptor<>("timer-state", Long.class);
                        timerState = getRuntimeContext().getState(timerDescriptor);
                    }

                    @Override
                    public void processElement(AddressClusterCDC event, Context ctx, Collector<AddressClusterCDC> out) throws Exception {
                        LOG.info("Processing element: {}", event);
						if (isDelete(event)) {
							deleteBuffer.update(event);
                            Long currentTimer = timerState.value();
                            if (currentTimer != null) {
                                ctx.timerService().deleteProcessingTimeTimer(currentTimer);
                                LOG.info("Cancelled existing timer: {}", currentTimer);
                            }
                            long newTimer = ctx.timerService().currentProcessingTime() + 1000;
                            ctx.timerService().registerProcessingTimeTimer(newTimer);
                            timerState.update(newTimer);
							return;
						} else if (isInsert(event)) {
                            if(deleteBuffer.value() != null) {
                                event.setOp("u");
                                AddressCluster before = new AddressCluster();
                                before.setAddress(event.getAfter().getAddress());
                                event.setBefore(before);
                            }
                            Long currentTimer = timerState.value();
                            if (currentTimer != null) {
                                ctx.timerService().deleteProcessingTimeTimer(currentTimer);
                                LOG.info("Cancelled timer on insert: {}", currentTimer);
                            }
							deleteBuffer.clear();
                            timerState.clear();
							out.collect(event);
						}
                    }

                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<AddressClusterCDC> out) throws Exception { 
						AddressClusterCDC deleteEvent = deleteBuffer.value();
						if (deleteEvent != null) {
                            LOG.info("Collecting delete event onTimer: {}", deleteEvent);
							out.collect(deleteEvent);
							deleteBuffer.clear();
                            timerState.clear();
						} else {
                            LOG.info("No delete event found in buffer at timer: {}", timestamp);
                        }
                    }
                }).sinkTo(sink)
                .name("[address-cluster-output-stream] Address Cluster Output Stream Sink");

        env.execute("Address Cluster Stream Job");
    }

    private static Properties toProperties(Map<String, String> map) {
        Properties p = new Properties();
        p.putAll(map);
        return p;
      }
} 