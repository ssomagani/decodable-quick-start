/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright Decodable, Inc.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package co.decodable.example;

import static co.decodable.example.DataStreamJob.ORDERS;
import static co.decodable.example.DataStreamJob.ORDERS_COMPACT;

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

@SourceStreams(ORDERS)
@SinkStreams(ORDERS_COMPACT)
public class DataStreamJob {

	private static final Logger LOG = LoggerFactory.getLogger(DataStreamJob.class);

	static final String ORDERS = "orders";
	static final String ORDERS_COMPACT = "orders_compact";

	static final ObjectMapper OBJECT_MAPPER = JacksonMapperFactory.createObjectMapper()
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

	public static void main(String[] args) throws Exception {
		
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		DecodableStreamSource<OrdersCDC> source =
				DecodableStreamSource.<OrdersCDC>builder()
					.withStreamName(ORDERS)
					.withDeserializationSchema(new JsonDeserializationSchema<OrdersCDC>(OrdersCDC.class, () -> OBJECT_MAPPER))
					.build();

		DecodableStreamSink<OrdersCDC> sink =
			DecodableStreamSink.<OrdersCDC>builder()
				.withStreamName(ORDERS_COMPACT)
				.withSerializationSchema(new JsonSerializationSchema<>(() -> OBJECT_MAPPER))
				.build();

		DataStream<OrdersCDC> stream =
			env.fromSource(source, WatermarkStrategy.noWatermarks(),
				"[stream-test] Test Stream Source");

		stream
				.<Integer>keyBy(record -> {
					if (record.isDelete()) {
						return record.getBefore().getOrderId();
					} else {
						return record.getAfter().getOrderId();
					}
				})
				.process(new KeyedProcessFunction<Integer, OrdersCDC, OrdersCDC>() {
					private ValueState<OrdersCDC> deleteBuffer;

					@Override
					public void open(Configuration parameters) throws Exception {
						ValueStateDescriptor<OrdersCDC> descriptor = new ValueStateDescriptor<>("delete-buffer", OrdersCDC.class);
						deleteBuffer = getRuntimeContext().getState(descriptor);
					}

					@Override
					public void processElement(OrdersCDC event, Context ctx, Collector<OrdersCDC> out) throws Exception {
						LOG.info("Processing element: {}", event);
						if (event.isDelete()) {
							deleteBuffer.update(event);
							ctx.timerService().registerProcessingTimeTimer(ctx.timestamp() + 1000);
							return;
						} else if (event.isInsert()) {
							// it's an update, clear delete buffer
							deleteBuffer.clear();
						}
						out.collect(event);
					}
			  
					@Override
					public void onTimer(long timestamp, OnTimerContext ctx, Collector<OrdersCDC> out) throws Exception {
						LOG.info("On timer: {}", timestamp);
						OrdersCDC deleteEvent = deleteBuffer.value();
						if (deleteEvent != null) {
							out.collect(deleteEvent);
							deleteBuffer.clear();
						}
					}
				});
			  
		stream.sinkTo(sink)
			.name("[test-output-stream] Test Output Stream Sink");

		env.execute("Test Stream Job");
	}
}