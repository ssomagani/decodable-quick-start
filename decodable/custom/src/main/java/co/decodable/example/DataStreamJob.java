/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright Decodable, Inc.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package co.decodable.example;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.SimpleCounter;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.decodable.sdk.pipeline.DecodableStreamSink;
import co.decodable.sdk.pipeline.DecodableStreamSource;
import co.decodable.sdk.pipeline.metadata.SinkStreams;
import co.decodable.sdk.pipeline.metadata.SourceStreams;

import static co.decodable.example.DataStreamJob.RAW_STREAM_STRING;
import static co.decodable.example.DataStreamJob.TEST_OUTPUT_STREAM_STRING;

@SourceStreams(RAW_STREAM_STRING)
@SinkStreams(TEST_OUTPUT_STREAM_STRING)
public class DataStreamJob {

	static final String RAW_STREAM_STRING = "raw_stream";
	static final String TEST_OUTPUT_STREAM_STRING = "test_output_stream";

	public static void main(String[] args) throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		DecodableStreamSource<String> source =
				DecodableStreamSource.<String>builder()
					.withStreamName(RAW_STREAM_STRING)
					.withDeserializationSchema(new SimpleStringSchema())
					.build();

		DecodableStreamSink<String> sink =
			DecodableStreamSink.<String>builder()
				.withStreamName(TEST_OUTPUT_STREAM_STRING)
				.withSerializationSchema(new SimpleStringSchema())
				.build();

		DataStream<String> stream =
			env.fromSource(source, WatermarkStrategy.noWatermarks(),
 				"[stream-test] Test Stream Source")
				.map(new NameConverter());

		stream.sinkTo(sink)
			.name("[test-output-stream] Test Output Stream Sink");

		env.execute("Test Stream Job");
	}

	public static class NameConverter extends RichMapFunction<String, String> {

		private static final long serialVersionUID = 1L;

		private transient ObjectMapper mapper;
		private Counter recordsProcessed;

		@Override
		public void open(Configuration parameters) throws Exception {
			mapper = new ObjectMapper();
			recordsProcessed = getRuntimeContext()
				.getMetricGroup()
				.addGroup("DecodableMetrics")
				.counter("recordsProcessed", new SimpleCounter());
		}

		@Override
		public String map(String value) throws Exception {
			return value;
		}
	}
}