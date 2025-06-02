package co.decodable.example;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.DataType;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ReadableConfig;

import java.util.HashSet;
import java.util.Set;

public class CustomSourceFactory implements DynamicTableSourceFactory {

    public static final ConfigOption<String> HOSTNAME = ConfigOptions.key("hostname")
            .stringType()
            .noDefaultValue();

    public static final ConfigOption<Integer> PORT = ConfigOptions.key("port")
            .intType()
            .noDefaultValue();

    @Override
    public String factoryIdentifier() {
        return "custom-source";
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(HOSTNAME);
        options.add(PORT);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new HashSet<>();
    }

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        // Get the validated options
        final FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);
        helper.validate();

        final ReadableConfig options = helper.getOptions();
        final String hostname = options.get(HOSTNAME);
        final int port = options.get(PORT);

        // Get the produced data type
        final DataType producedDataType = context.getCatalogTable().getResolvedSchema().toPhysicalRowDataType();

        // Create and return the dynamic table source
        return new CustomTableSource(hostname, port, producedDataType);
    }
}

class CustomTableSource implements ScanTableSource {
    private final String hostname;
    private final int port;
    private final DataType producedDataType;

    public CustomTableSource(String hostname, int port, DataType producedDataType) {
        this.hostname = hostname;
        this.port = port;
        this.producedDataType = producedDataType;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        return ChangelogMode.insertOnly();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext context) {
        final SourceFunction<RowData> sourceFunction = new StringSourceFunction(hostname, port);
        return SourceFunctionProvider.of(sourceFunction, false);
    }

    @Override
    public DynamicTableSource copy() {
        return new CustomTableSource(hostname, port, producedDataType);
    }

    @Override
    public String asSummaryString() {
        return "Custom Source";
    }
} 