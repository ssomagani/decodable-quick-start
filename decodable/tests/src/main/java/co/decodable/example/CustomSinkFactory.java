package co.decodable.example;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DynamicTableFactory;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.types.DataType;

import java.util.HashSet;
import java.util.Set;

public class CustomSinkFactory implements DynamicTableSinkFactory {
    public static final ConfigOption<String> HOSTNAME = ConfigOptions
            .key("hostname")
            .stringType()
            .noDefaultValue();

    public static final ConfigOption<Integer> PORT = ConfigOptions
            .key("port")
            .intType()
            .noDefaultValue();

    @Override
    public String factoryIdentifier() {
        return "custom-sink";
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(HOSTNAME);
        options.add(PORT);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new HashSet<>();
    }

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        return new CustomTableSink(
            context.getCatalogTable().getResolvedSchema().toPhysicalRowDataType(),
            context.getConfiguration()
        );
    }

    private static class CustomTableSink implements DynamicTableSink {
        private final DataType physicalDataType;
        private final ReadableConfig config;

        public CustomTableSink(DataType physicalDataType, ReadableConfig config) {
            this.physicalDataType = physicalDataType;
            this.config = config;
        }

        @Override
        public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
            return requestedMode;
        }

        @Override
        public SinkRuntimeProvider getSinkRuntimeProvider(DynamicTableSink.Context context) {
            String hostname = config.get(HOSTNAME);
            int port = config.get(PORT);

            return SinkFunctionProvider.of(new StringSinkFunction(hostname, port));
        }

        @Override
        public DynamicTableSink copy() {
            return new CustomTableSink(physicalDataType, config);
        }

        @Override
        public String asSummaryString() {
            return "Custom Sink";
        }
    }
} 