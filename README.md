# log4Rich - Lightweight Java Logging Framework

A high-performance, lightweight logging framework for Java 17+ inspired by Log4j, designed for modern applications with emphasis on simplicity, thread safety, and configurability.

## Documentation

- 📖 **[API Documentation](https://richardahasting.github.io/log4Rich/)** - Complete JavaDoc API reference
- 📚 **[User Guide](#quick-start)** - Getting started and usage examples
- 🔧 **[Configuration Reference](#configuration)** - Complete configuration guide

## Features

- ✅ **7 Log Levels**: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF
- ✅ **Thread-Safe**: Built for multi-CPU environments with concurrent logging
- ✅ **Console Output**: Configurable STDOUT/STDERR with custom patterns
- ✅ **Rolling File Appender**: Size-based file rotation with backup management
- ✅ **External Compression**: Support for gzip, bzip2, xz, and custom compression programs
- ✅ **Location Information**: Automatic capture of class name, method name, and line numbers
- ✅ **Runtime Configuration**: Dynamic configuration changes without restart
- ✅ **Flexible Configuration**: File-based configuration with multiple search locations
- ✅ **High Performance**: Optimized for 200,000+ messages per second
- ✅ **Maven Integration**: Easy build and dependency management

## Quick Start

### 1. Basic Usage

```java
import com.log4rich.Log4Rich;
import com.log4rich.core.Logger;

public class MyApplication {
    private static final Logger logger = Log4Rich.getLogger(MyApplication.class);
    
    public static void main(String[] args) {
        logger.info("Application starting...");
        logger.debug("Debug information");
        logger.warn("Warning message");
        logger.error("Error occurred", new Exception("Sample exception"));
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

#### Logger-Specific Settings
- `log4rich.logger.{name}`: Set level for specific logger

## Advanced Usage

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
- Java 17 or higher
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

log4Rich is designed for high-performance logging:

- **Throughput**: 200,000+ messages per second
- **Thread Safety**: Lock-free design where possible
- **Memory Efficient**: Minimal object allocation
- **Async Friendly**: Non-blocking appender operations
- **Configurable Buffering**: Tunable buffer sizes for optimal I/O

### Performance Tips

1. **Disable Location Capture** in production for better performance:
   ```java
   Log4Rich.setLocationCapture(false);
   ```

2. **Use Appropriate Buffer Sizes**:
   ```properties
   log4rich.file.bufferSize=16384
   log4rich.file.immediateFlush=false
   ```

3. **Set Appropriate Log Levels**:
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

log4Rich supports multiple compression programs:

| Program | Extension | Description |
|---------|-----------|-------------|
| gzip | `.gz` | Fast compression, widely supported |
| bzip2 | `.bz2` | Better compression ratio |
| xz | `.xz` | Excellent compression, slower |
| zip | `.zip` | Cross-platform compatibility |
| 7z | `.7z` | High compression ratio |

### Custom Compression

```properties
# Custom compression program
log4rich.file.compress.program=pigz
log4rich.file.compress.args=-9 -p 4
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
- Initial release
- Core logging functionality
- Console and file appenders
- Rolling file support with compression
- Runtime configuration management
- Location information capture
- Comprehensive test suite
- Complete JavaDoc documentation

---

**log4Rich** - Simple, Fast, Reliable Logging for Java Applications