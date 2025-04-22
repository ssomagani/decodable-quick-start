/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright Decodable, Inc.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package co.decodable.example;

import static org.assertj.core.api.Assertions.assertThat;

import co.decodable.sdk.pipeline.testing.PipelineTestContext;
import co.decodable.sdk.pipeline.testing.StreamRecord;
import co.decodable.sdk.pipeline.testing.TestEnvironment;
import co.decodable.sdk.pipeline.testing.PipelineTestContext.ThrowingConsumer;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;

@Testcontainers
public class JobTests {

	static final String RAW_STREAM_STRING = "raw_stream";
	static final String TEST_OUTPUT_STREAM_STRING = "test_output_stream";

  @Container
  public RedpandaContainer broker =
      new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.2");

  @DisplayName("Test Example Jobs for DataStream and Table API")
  @ParameterizedTest(name = "[{index}] running test for {0}")
  @MethodSource("provideJobEntryPoints")
  public void shouldPassThroughData(String jobName, ThrowingConsumer<String[]> mainMethod) throws Exception {
    TestEnvironment testEnvironment =
        TestEnvironment.builder()
            .withBootstrapServers(broker.getBootstrapServers())
            .withStreams(RAW_STREAM_STRING, TEST_OUTPUT_STREAM_STRING)
            .build();

    try (PipelineTestContext ctx = new PipelineTestContext(testEnvironment)) {
      String testValue = "test data";

      // given
      ctx.stream(RAW_STREAM_STRING).add(new StreamRecord<>(testValue));

      ctx.runJobAsync(mainMethod);

      StreamRecord<String> result =
          ctx.stream(TEST_OUTPUT_STREAM_STRING).takeOne().get(30, TimeUnit.SECONDS);

      // then
      assertThat(result.value()).isEqualTo(testValue);
    }
  }

  static Stream<Arguments> provideJobEntryPoints() {
    return Stream.of(
      Arguments.of(DataStreamJob.class.getSimpleName(),(ThrowingConsumer<String[]>)DataStreamJob::main)
    );
  }
}