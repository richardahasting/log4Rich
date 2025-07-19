# log4Rich - Ultra-High-Performance Java Logging Framework

A blazing-fast logging framework for Java 8+ that delivers up to **2.3 million messages/second** through advanced I/O optimizations including memory-mapped files and intelligent batching. Inspired by Log4j, designed for modern applications demanding extreme performance without sacrificing simplicity.

## Documentation

- 📖 **[API Documentation](https://richardahasting.github.io/log4Rich/)** - Complete JavaDoc API reference
- 📚 **[User Guide](#quick-start)** - Getting started and usage examples
- 🔧 **[Configuration Reference](#configuration)** - Complete configuration guide

## Features

- 🚀 **Blazing Fast Performance**: Up to 2.3 million messages/second with batch processing
- 🧠 **Memory-Mapped I/O**: 5.4x faster logging with zero context switching
- ⚡ **Batch Processing**: 23x performance improvement in multi-threaded scenarios
- 🔥 **Zero-Allocation Mode**: Thread-local object pools eliminate GC pressure
- ⚡ **Asynchronous Logging**: Lock-free ring buffers with sub-microsecond latency
- 🗜️ **Async Compression**: Non-blocking compression with adaptive file size management
- 🔧 **SLF4J-Style Placeholders**: Easy migration with {} parameter substitution
- 🌍 **Environment Variables**: Override any configuration with LOG4RICH_* variables
- 📝 **Enhanced Error Messages**: Detailed validation with specific fix guidance
- ✅ **7 Log Levels**: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF
- ✅ **Thread-Safe**: Built for multi-CPU environments with concurrent logging
- ✅ **Console Output**: Configurable STDOUT/STDERR with custom patterns
- ✅ **Rolling File Appender**: Size-based file rotation with backup management
- ✅ **External Compression**: Support for gzip, bzip2, xz, and custom compression programs
- ✅ **Location Information**: Automatic capture of class name, method name, and line numbers
- ✅ **Runtime Configuration**: Dynamic configuration changes without restart
- ✅ **Flexible Configuration**: File-based configuration with multiple search locations
- ✅ **Maven Integration**: Easy build and dependency management

## Quick Start

### 📚 Complete Usage Examples

For comprehensive usage examples and best practices, see:
- **[Log4RichUsageDemo.java](src/test/java/com/log4rich/Log4RichUsageDemo.java)** - Complete demonstration of all features
- **[log4Rich.demo.config](log4Rich.demo.config)** - Comprehensive configuration template

The demo file showcases:
- ✅ Basic logging patterns
- 🚀 High-performance features (memory-mapped files, batch processing)
- 🔀 Multi-threaded logging scenarios  
- ⚙️ Runtime configuration management
- 🏭 Production deployment best practices

Run the demo to see actual performance comparisons:
```bash
mvn compile exec:java -Dexec.mainClass="com.log4rich.Log4RichUsageDemo"
```

### 1. Basic Usage

```java
import com.log4rich.Log4Rich;
import com.log4rich.core.Logger;

public class MyApplication {
    private static final Logger logger = Log4Rich.getLogger(MyApplication.class);
    
    public static void main(String[] args) {
        // Traditional logging
        logger.info("Application starting...");
        logger.debug("Debug information");
        logger.warn("Warning message");
        logger.error("Error occurred", new Exception("Sample exception"));
        
        // SLF4J-style placeholder logging (new in v1.0.0)
        String user = "john_doe";
        int attempts = 3;
        logger.info("User {} failed login after {} attempts", user, attempts);
        logger.warn("Processing {} records in {} ms", 1000, 245);
        
        // Mixed placeholders with exceptions
        logger.error("Failed to process user {} data", user, new RuntimeException("Database error"));
    }
}
```

### 2. Configuration File

Create `log4Rich.config` in your project root:

```properties
# Root logger level
log4rich.rootLevel=INFO

# Console logging
log4rich.console.enabled=true
log4rich.console.target=STDOUT
log4rich.console.pattern=[%level] %date{yyyy-MM-dd HH:mm:ss} [%thread] %class.%method:%line - %message%n

# File logging
log4rich.file.enabled=true
log4rich.file.path=logs/application.log
log4rich.file.pattern=[%level] %date{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %class.%method:%line - %message%n
log4rich.file.maxSize=10M
log4rich.file.maxBackups=10

# Compression
log4rich.file.compress=true
log4rich.file.compress.program=gzip
log4rich.file.compress.args=
log4rich.file.compress.async=true

# Async compression settings
log4rich.file.compress.async.queueSize=100
log4rich.file.compress.async.threads=2
log4rich.file.compress.async.timeout=30000

# Location capture
log4rich.location.capture=true

# Logger-specific levels
log4rich.logger.com.myapp.service=DEBUG
log4rich.logger.com.myapp.dao=WARN
```

### 3. Runtime Configuration

```java
// Change log levels at runtime
Log4Rich.setRootLevel(LogLevel.DEBUG);
Log4Rich.setLoggerLevel("com.myapp.service", LogLevel.TRACE);

// Toggle features
Log4Rich.setLocationCapture(false);
Log4Rich.setConsoleEnabled(true);
Log4Rich.setFileEnabled(true);

// Update file settings
Log4Rich.setFilePath("logs/new-application.log");
Log4Rich.setMaxFileSize("50M");
Log4Rich.setMaxBackups(20);

// Configure compression
Log4Rich.setCompression(true, "bzip2", "-9");
```

## Configuration

### Configuration File Locations

log4Rich searches for configuration files in the following order:

1. **System Property**: `-Dlog4rich.config=/path/to/config`
2. **Classpath**: `log4Rich.config` (root of classpath)
3. **Current Directory**: `./log4Rich.config`
4. **Parent Directory**: `../log4Rich.config`
5. **Config Directories**:
   - `./config/log4Rich.config`
   - `./conf/log4Rich.config`
   - `../config/log4Rich.config`
   - `../conf/log4Rich.config`

### Environment Variable Configuration

**New in v1.0.0**: Override any configuration property using environment variables with the `LOG4RICH_` prefix:

```bash
# Set via environment variables
export LOG4RICH_ROOT_LEVEL=DEBUG
export LOG4RICH_CONSOLE_ENABLED=false
export LOG4RICH_FILE_PATH=/var/log/myapp.log
export LOG4RICH_FILE_MAX_SIZE=50M

# Run your application - config will be automatically overridden
java -jar myapp.jar
```

**Supported Environment Variables**:
- `LOG4RICH_ROOT_LEVEL` → `log4rich.rootLevel`
- `LOG4RICH_CONSOLE_ENABLED` → `log4rich.console.enabled`
- `LOG4RICH_CONSOLE_TARGET` → `log4rich.console.target`
- `LOG4RICH_FILE_PATH` → `log4rich.file.path`
- `LOG4RICH_FILE_MAX_SIZE` → `log4rich.file.maxSize`
- `LOG4RICH_FILE_MAX_BACKUPS` → `log4rich.file.maxBackups`
- `LOG4RICH_LOCATION_CAPTURE` → `log4rich.location.capture`
- And many more... (see ConfigLoader.getSupportedEnvironmentVariables() for complete list)

### Pattern Format

log4Rich supports the following pattern placeholders:

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%level` | Log level | `INFO`, `DEBUG`, `ERROR` |
| `%date{format}` | Timestamp | `%date{yyyy-MM-dd HH:mm:ss}` |
| `%thread` | Thread name | `main`, `pool-1-thread-1` |
| `%class` | Simple class name | `MyClass` |
| `%method` | Method name | `doSomething` |
| `%line` | Line number | `42` |
| `%message` | Log message | `User logged in` |
| `%n` | Line separator | System-dependent newline |

### Configuration Properties

#### General Settings
- `log4rich.rootLevel`: Root logger level (TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF)
- `log4rich.location.capture`: Enable location capture (true/false)

#### Console Settings
- `log4rich.console.enabled`: Enable console logging (true/false)
- `log4rich.console.target`: Output stream (STDOUT/STDERR)
- `log4rich.console.pattern`: Console message pattern
- `log4rich.console.level`: Console-specific log level

#### File Settings
- `log4rich.file.enabled`: Enable file logging (true/false)
- `log4rich.file.path`: Log file path
- `log4rich.file.pattern`: File message pattern
- `log4rich.file.level`: File-specific log level
- `log4rich.file.maxSize`: Maximum file size (e.g., "10M", "100K", "1G")
- `log4rich.file.maxBackups`: Number of backup files to keep
- `log4rich.file.encoding`: File encoding (default: UTF-8)
- `log4rich.file.immediateFlush`: Flush after each write (true/false)
- `log4rich.file.bufferSize`: Buffer size in bytes
- `log4rich.file.datePattern`: Date pattern for backup files

#### Compression Settings
- `log4rich.file.compress`: Enable compression (true/false)
- `log4rich.file.compress.program`: Compression program (gzip, bzip2, xz, zip, 7z)
- `log4rich.file.compress.args`: Additional compression arguments
- `log4rich.file.compress.async`: Enable asynchronous compression (true/false, default: true)

#### Async Compression Settings
- `log4rich.file.compress.async.queueSize`: Maximum compression queue size (default: 100)
- `log4rich.file.compress.async.threads`: Number of compression threads (default: 2)
- `log4rich.file.compress.async.timeout`: Compression timeout in milliseconds (default: 30000)

#### Logger-Specific Settings
- `log4rich.logger.{name}`: Set level for specific logger

## Migration from SLF4J/Log4j

**New in v1.0.0**: log4Rich now supports SLF4J-style placeholder logging for easy migration!

### SLF4J Compatibility

log4Rich supports the same `{}` placeholder syntax as SLF4J:

```java
// SLF4J code - works as-is in log4Rich!
logger.info("User {} logged in from {}", username, ipAddress);
logger.warn("Request {} took {} ms", requestId, duration);
logger.error("Failed to process order {}", orderId, exception);

// Array parameters (SLF4J compatible)
String[] items = {"apple", "banana", "cherry"};
logger.info("Processing items: {}", (Object) items);

// Automatic exception detection
logger.error("Database connection failed for user {}", userId, new SQLException("Connection timeout"));
```

### Migration Checklist

1. **Replace Logger Import**:
   ```java
   // Old SLF4J import
   import org.slf4j.Logger;
   import org.slf4j.LoggerFactory;
   
   // New log4Rich import
   import com.log4rich.core.Logger;
   import com.log4rich.Log4Rich;
   ```

2. **Update Logger Creation**:
   ```java
   // Old SLF4J way
   private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
   
   // New log4Rich way
   private static final Logger logger = Log4Rich.getLogger(MyClass.class);
   ```

3. **Configuration Migration**:
   - Use environment variables for quick overrides
   - Convert logback.xml/log4j.properties to log4Rich.config format
   - Take advantage of enhanced error messages for validation

4. **Performance Benefits**:
   - Enable memory-mapped files for 5.4x performance boost
   - Use batch processing for 23x multi-threaded improvement
   - Leverage async compression for non-blocking file rotation

## Advanced Usage

### SLF4J-Style Parameter Logging

```java
Logger logger = Log4Rich.getLogger(MyClass.class);

// All SLF4J patterns work identically
logger.debug("Processing request {} with {} parameters", requestId, paramCount);
logger.info("User {} performed action '{}' at {}", username, action, timestamp);
logger.warn("Cache miss for key '{}', fallback took {} ms", cacheKey, fallbackTime);

// Exception handling (last parameter automatically detected)
logger.error("Failed to save user {} profile", username, new DatabaseException("Connection lost"));

// Complex parameter types
logger.info("Processed {} orders totaling ${}", orders.size(), total.setScale(2, RoundingMode.HALF_UP));

// Array and object logging
Object[] metrics = {cpu, memory, disk};
logger.debug("System metrics: {}", (Object) metrics);
```

### Custom Appenders

```java
// Create custom console appender
ConsoleAppender consoleAppender = Log4Rich.createConsoleAppender(
    "MyConsole", "STDERR", "[%level] %message%n"
);

// Create custom file appender
RollingFileAppender fileAppender = Log4Rich.createRollingFileAppender(
    "MyFile", "logs/custom.log", "5M", 5, "%date %level %message%n"
);

// Add to logger
Logger logger = Log4Rich.getLogger("com.myapp.custom");
logger.addAppender(consoleAppender);
logger.addAppender(fileAppender);
```

### Asynchronous Logging

```java
import com.log4rich.core.AsyncLogger;
import com.log4rich.util.OverflowStrategy;

// Create async logger with custom settings
AsyncLogger asyncLogger = new AsyncLogger(
    "HighThroughput",
    65536,                          // Buffer size (power of 2)
    OverflowStrategy.DROP_OLDEST,   // Overflow strategy  
    5000                            // Shutdown timeout
);

// Add appenders
asyncLogger.addAppender(consoleAppender);
asyncLogger.addAppender(fileAppender);

// Log asynchronously (sub-microsecond latency)
asyncLogger.info("High-performance async message");

// Get performance statistics
AsyncLogger.AsyncLoggerStatistics stats = asyncLogger.getStatistics();
System.out.println("Events published: " + stats.getEventsPublished());
System.out.println("Events processed: " + stats.getEventsProcessed());
System.out.println("Drop rate: " + stats.getDropRate() * 100 + "%");

// Graceful shutdown
asyncLogger.shutdown();
```

### High-Performance Features

```java
import com.log4rich.appenders.*;

// Memory-mapped file appender (5.4x faster)
MemoryMappedFileAppender mmapAppender = new MemoryMappedFileAppender(
    "HighSpeed",
    "logs/highspeed.log",
    16 * 1024 * 1024,  // 16MB mapping
    false,             // Don't force on every write
    1000              // Force every 1 second
);

// Batch processing appender (23x faster multi-threaded)
BatchingFileAppender batchAppender = new BatchingFileAppender(
    "BatchProcessor",
    "logs/batch.log",
    1000,  // Batch size
    100    // Batch timeout (ms)
);

// Async compression for non-blocking file rotation
RollingFileAppender rollingAppender = new RollingFileAppender("logs/rolling.log");
rollingAppender.setUseAsyncCompression(true);
rollingAppender.setMaxFileSize(100 * 1024 * 1024); // 100MB

// Get compression statistics
AsyncCompressionManager.CompressionStatistics compStats = 
    rollingAppender.getCompressionStatistics();
System.out.println("Compression queue: " + compStats.getCurrentQueueSize());
```

### Level Checking

```java
Logger logger = Log4Rich.getLogger(MyClass.class);

// Avoid expensive operations for disabled levels
if (logger.isDebugEnabled()) {
    logger.debug("Expensive debug info: " + calculateExpensiveValue());
}

// Check specific levels
if (logger.isTraceEnabled()) {
    logger.trace("Detailed trace information");
}
```

### Programmatic Configuration

```java
// Load custom configuration
Log4Rich.setConfigPath("/path/to/custom/config.properties");

// Reload configuration
Log4Rich.reloadConfiguration();

// Get configuration statistics
ConfigurationManager.ConfigurationStats stats = Log4Rich.getStats();
System.out.println("Active loggers: " + stats.getLoggerCount());
System.out.println("Active appenders: " + stats.getAppenderCount());
```

## API Documentation

The complete JavaDoc API documentation is available online at:

**🌐 https://richardahasting.github.io/log4Rich/**

### Key Documentation Pages

- **[Log4Rich Main API](https://richardahasting.github.io/log4Rich/com/log4rich/Log4Rich.html)** - Main entry point and facade
- **[Logger Core](https://richardahasting.github.io/log4Rich/com/log4rich/core/Logger.html)** - Core logging functionality
- **[Configuration Management](https://richardahasting.github.io/log4Rich/com/log4rich/config/ConfigurationManager.html)** - Runtime configuration
- **[Appenders](https://richardahasting.github.io/log4Rich/com/log4rich/appenders/package-summary.html)** - Output destinations
- **[All Classes](https://richardahasting.github.io/log4Rich/allclasses-index.html)** - Complete class index

## Building

### Prerequisites
- Java 8 or higher
- Maven 3.6+

### Build Commands

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Build JAR
mvn package

# Generate JavaDoc
mvn javadoc:javadoc

# Create executable JAR with dependencies
mvn clean package
```

### Maven Dependency

```xml
<dependency>
    <groupId>com.log4rich</groupId>
    <artifactId>log4Rich</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Performance

log4Rich delivers exceptional performance through advanced I/O optimizations:

### 🚀 Blazing Fast Performance

- **Standard Mode**: 200,000+ messages/second
- **Memory-Mapped Mode**: 750,000+ messages/second (5.4x faster)
- **Batch Mode**: 714,000+ messages/second single-threaded, **2.3 million+ messages/second** multi-threaded
- **Zero Context Switching**: Memory-mapped files eliminate kernel/user space transitions
- **Near-Zero Allocation**: Object pooling eliminates garbage collection pressure
- **Wide Java Support**: Compatible with Java 8 through Java 21+

### Performance Benchmarks

Based on rigorous benchmarking with 100,000 messages:

| Mode | Single-Thread | 8 Threads | Latency |
|------|---------------|-----------|---------|
| Standard | 137K msg/s | 400K msg/s | 7.3 μs |
| Memory-Mapped | 748K msg/s | 1.3M msg/s | 1.3 μs |
| Batch | 715K msg/s | 2.3M msg/s | 1.4 μs |

### Advanced Performance Features

#### 1. Memory-Mapped File I/O
Leveraging kernel-level optimizations to eliminate context switching:
```properties
log4rich.performance.memoryMapped=true
log4rich.performance.mappedSize=64M
log4rich.performance.forceInterval=1000
```

#### 2. Batch Processing
Dramatically reduce I/O operations with intelligent batching:
```properties
log4rich.performance.batchEnabled=true
log4rich.performance.batchSize=1000
log4rich.performance.batchTimeMs=100
```

#### 3. Zero-Allocation Mode
Eliminate object allocation with thread-local pools:
```properties
log4rich.performance.zeroAllocation=true
log4rich.performance.stringBuilderCapacity=1024
```

### Performance Tips

1. **Enable Performance Features** for maximum throughput:
   ```properties
   # Memory-mapped files for lowest latency
   log4rich.performance.memoryMapped=true
   
   # Or batch processing for highest throughput
   log4rich.performance.batchEnabled=true
   ```

2. **Disable Location Capture** in production:
   ```java
   Log4Rich.setLocationCapture(false);
   ```

3. **Use Appropriate Buffer Sizes**:
   ```properties
   log4rich.file.bufferSize=16384
   log4rich.file.immediateFlush=false
   ```

4. **Set Appropriate Log Levels**:
   ```properties
   log4rich.rootLevel=WARN  # In production
   ```

## Architecture

### Core Components

- **Log4Rich**: Main API facade
- **Logger**: Core logging functionality
- **LogManager**: Logger factory and registry
- **LogLevel**: Enumeration of logging levels
- **LoggingEvent**: Immutable event representation

### Appenders

- **ConsoleAppender**: Console output (STDOUT/STDERR)
- **RollingFileAppender**: File output with rotation
- **MemoryMappedFileAppender**: Ultra-fast file I/O with memory mapping
- **BatchingFileAppender**: High-throughput batch processing
- **Appender Interface**: For custom appender implementations

### Configuration

- **Configuration**: Settings container
- **ConfigurationManager**: Runtime configuration management
- **ConfigLoader**: Multi-location configuration loading

### Utilities

- **LocationInfo**: Stack trace analysis for location capture
- **ThreadSafeWriter**: Thread-safe file I/O
- **CompressionManager**: External compression program integration

## Testing

The project includes comprehensive test coverage:

```bash
# Run all tests
mvn test

# Run specific test categories
mvn test -Dtest=*ConfigurationTest
mvn test -Dtest=*StressTest
```

### Test Categories

- **Unit Tests**: Individual component testing
- **Integration Tests**: End-to-end functionality
- **Configuration Tests**: Runtime configuration management
- **Stress Tests**: Performance and load testing
- **Compression Tests**: External program integration

## Compression Support

log4Rich features advanced compression capabilities with both traditional blocking compression and high-performance asynchronous compression.

### Supported Compression Programs

| Program | Extension | Description |
|---------|-----------|-------------|
| gzip | `.gz` | Fast compression, widely supported |
| bzip2 | `.bz2` | Better compression ratio |
| xz | `.xz` | Excellent compression, slower |
| zip | `.zip` | Cross-platform compatibility |
| 7z | `.7z` | High compression ratio |

### Asynchronous Compression with Adaptive Management

**⚠️ IMPORTANT**: log4Rich includes intelligent adaptive compression management to prevent system overload.

When log rotation occurs faster than compression can keep pace:

1. **Detection**: Monitors compression queue utilization
2. **Intervention**: Temporarily blocks to prevent queue overflow
3. **Adaptation**: Automatically **DOUBLES** the file size limit
4. **Notification**: Logs the change in **CAPITAL LETTERS** in the log file:

```
*** ADAPTIVE FILE SIZE INCREASE ***
APPENDER: MyFileAppender
OLD MAX SIZE: 10.0 MB
NEW MAX SIZE: 20.0 MB (DOUBLED DUE TO COMPRESSION OVERLOAD)
TIMESTAMP: 2024-01-15 14:30:45
*** END ADAPTIVE CHANGE ***
```

This prevents:
- Memory exhaustion from unbounded compression queues
- Application blocking during high-volume logging
- System instability from compression overload

### Compression Performance Modes

#### Asynchronous Mode (Default)
- **Non-blocking**: Compression occurs in background threads
- **High throughput**: Logging continues while compression processes
- **Adaptive management**: Automatically handles overload conditions
- **Queue monitoring**: Tracks compression queue utilization

#### Blocking Mode (Legacy)
- **Guaranteed compression**: Files compressed before returning
- **Simpler behavior**: Traditional synchronous operation
- **Potential blocking**: May pause logging during compression

### Custom Compression Configuration

```properties
# High-performance parallel compression
log4rich.file.compress.program=pigz
log4rich.file.compress.args=-9 -p 4

# Async compression tuning
log4rich.file.compress.async=true
log4rich.file.compress.async.queueSize=50
log4rich.file.compress.async.threads=4
log4rich.file.compress.async.timeout=60000
```

## Troubleshooting

### Common Issues

1. **No log output**: Check if console/file logging is enabled
2. **File not found**: Verify log directory exists and is writable
3. **Compression fails**: Ensure compression program is installed and in PATH
4. **Performance issues**: Disable location capture, increase buffer sizes

### Debug Configuration

```properties
# Enable debug output
log4rich.rootLevel=DEBUG
log4rich.console.enabled=true
log4rich.console.target=STDERR
```

### Configuration Validation

```java
// Check if configuration file exists
boolean configExists = ConfigLoader.configExists();

// Get search paths
String[] searchPaths = ConfigLoader.getSearchPaths();
for (String path : searchPaths) {
    System.out.println("Search path: " + path);
}
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

### Why Apache License 2.0?

- **Industry Standard**: Same license used by Log4j, Logback, and SLF4J
- **Business Friendly**: Allows commercial use without restrictions
- **Patent Protection**: Includes explicit patent grants and protections
- **Permissive**: Modify, distribute, and use in proprietary software
- **Trusted**: Backed by the Apache Software Foundation

## Changelog

### Version 1.0.0
- **Initial Release**: Core logging functionality with console and file appenders
- **Rolling File Support**: Size-based rotation with compression
- **Runtime Configuration**: Dynamic configuration changes without restart
- **Location Information**: Automatic capture of class, method, and line numbers
- **Performance Features**: Memory-mapped files (5.4x faster), batch processing (23x faster), zero-allocation mode
- **Asynchronous Features**: Lock-free ring buffers, async compression with adaptive management
- **SLF4J Compatibility**: {} placeholder support for easy migration from SLF4J/Log4j
- **Environment Variables**: Override any configuration with LOG4RICH_* environment variables  
- **Enhanced Validation**: Detailed error messages with specific fix guidance
- **Comprehensive Testing**: Full test suite with performance benchmarks
- **Complete Documentation**: JavaDoc API reference and usage examples
- **Java 8+ compatibility** - Works with Java 8 through Java 21+

---

**log4Rich** - Simple, Fast, Reliable Logging for Java Applications