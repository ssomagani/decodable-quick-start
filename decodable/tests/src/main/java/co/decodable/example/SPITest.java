package co.decodable.example;

import org.apache.flink.table.factories.DynamicTableFactory;
import java.util.ServiceLoader;

/**
 * A simple test to check if the Service Provider Interface (SPI) is working correctly.
 * This test will attempt to load all DynamicTableFactory implementations from the classpath.
 */
public class SPITest {
    public static void main(String[] args) {
        System.out.println("Testing SPI for DynamicTableFactory...");
        
        // Load all DynamicTableFactory implementations
        ServiceLoader<DynamicTableFactory> loader = ServiceLoader.load(DynamicTableFactory.class);
        
        // Count the number of factories found
        int count = 0;
        for (DynamicTableFactory factory : loader) {
            count++;
            System.out.println("Found factory: " + factory.getClass().getName() + 
                               " with identifier: " + factory.factoryIdentifier());
        }
        
        System.out.println("Total factories found: " + count);
        
        // Check if our custom factories are included
        boolean hasCustomSource = false;
        boolean hasCustomSink = false;
        
        for (DynamicTableFactory factory : loader) {
            if (factory.factoryIdentifier().equals("custom-source")) {
                hasCustomSource = true;
                System.out.println("Found custom-source factory: " + factory.getClass().getName());
            } else if (factory.factoryIdentifier().equals("custom-sink")) {
                hasCustomSink = true;
                System.out.println("Found custom-sink factory: " + factory.getClass().getName());
            }
        }
        
        System.out.println("Has custom-source factory: " + hasCustomSource);
        System.out.println("Has custom-sink factory: " + hasCustomSink);
    }
} 