# log4Rich Development Plan

## Overview
log4Rich is a lightweight custom logging framework inspired by Log4j, designed to provide essential logging capabilities without the overhead of the full Log4j implementation.

## Project Goals
- Create a lightweight, easy-to-use logging framework
- Implement only the most essential Log4j features
- Produce a single `log4Rich.jar` file
- Maintain simplicity while providing flexibility

## Selected Features for log4Rich

### 1. **Logging Levels** ✓
All 7 levels will be supported:
- TRACE
- DEBUG
- INFO
- WARN
- ERROR
- FATAL
- OFF

### 2. **Appenders** ✓
- ConsoleAppender (stdout/stderr)
- RollingFileAppender with:
  - Size-based rotation
  - Automatic compression of rolled files
  - Configurable compression program (default: gzip)
  - Custom compression arguments support
  - Configurable max file size
  - Configurable number of backup files

### 3. **Layouts** ✓
- Standard layout with pattern support
- Default pattern: `[%level] %date{yyyy-MM-dd HH:mm:ss} [%thread] %class.%method:%line - %message`
- Support for location information (class, method, line number, file)
- Configurable class name format (FULL, SIMPLE, TRUNCATED)

### 4. **Configuration** ✓
- Configuration via `log4Rich.config` file
- Simple key-value properties format
- Default config file search locations (classpath, working directory, etc.)
- Programmatic config file location override
- Logger-specific level configuration
- Performance tuning options (buffer size, immediate flush)

### 5. **Thread Safety** ✓
- Thread-safe logging implementation
- Synchronized appender writes
- Thread-local buffers for performance

### 6. **Core Features Summary**
- Multi-threaded environment support
- File compression on rotation with configurable compression program
- Location information capture (with on/off toggle for performance)
- Simple, efficient implementation
- No hierarchical loggers (keeping it lightweight)
- No async logging (synchronous only for simplicity)

## Proposed Architecture

### Package Structure
```
com.log4rich/
├── core/
│   ├── Logger.java              # Main logger class
│   ├── LogLevel.java            # Enum for log levels
│   └── LogManager.java          # Logger factory and management
├── appenders/
│   ├── Appender.java            # Base appender interface
│   ├── ConsoleAppender.java     # Console output
│   └── RollingFileAppender.java # Rolling file with compression
├── layouts/
│   ├── Layout.java              # Base layout interface
│   └── StandardLayout.java      # Pattern-based layout
├── config/
│   ├── Configuration.java       # Configuration manager
│   └── ConfigLoader.java        # Loads log4Rich.config
├── util/
│   ├── LoggingEvent.java        # Log event data structure
│   ├── ThreadSafeWriter.java    # Thread-safe write operations
│   ├── CompressionManager.java  # External compression program handler
│   └── LocationInfo.java        # Captures class, method, line info
└── Log4Rich.java                # Main entry point/facade
```

## Build Configuration
- **Build System**: Maven
- **Target Java Version**: Java 17
- **Testing Framework**: JUnit 5

## Configuration File Search Order
Default locations checked in order:
1. System property: `-Dlog4rich.config=/path/to/config`
2. Classpath: `log4Rich.config` (root of classpath)
3. Current directory: `./log4Rich.config`
4. Parent directory: `../log4Rich.config`
5. Config directories:
   - `./config/log4Rich.config`
   - `./conf/log4Rich.config`
   - `../config/log4Rich.config`
   - `../conf/log4Rich.config`
6. Programmatic override: `Log4Rich.setConfigPath("/path/to/config")`

## Error Handling Strategies

### Compression Failure Handling
- If compression program is not found or fails:
  - Log ERROR message with detailed information (program name, error message)
  - Leave the rolled file uncompressed
  - Continue normal logging operations
  - File will be named with normal rolled pattern (without compression extension)

## Development Phases

### Phase 1: Core Framework
- Basic Logger class
- LogLevel enum
- Simple console output
- Basic configuration

### Phase 2: Selected Features
- Implement chosen appenders
- Add selected layouts
- Configuration system

### Phase 3: Optimization
- Performance improvements
- Error handling
- Documentation

### Phase 4: Build and Package
- Maven pom.xml configuration
- Create log4Rich.jar
- Usage examples
- Basic documentation

### Phase 5: Testing
- Unit tests for all core components
- Integration tests for file operations
- Thread safety tests
- Performance benchmarks
- Compression failure scenarios

## Next Steps
1. Review and select desired Log4j features
2. Finalize architecture based on selected features
3. Begin implementation of core components

## Questions for Discussion
Please review the feature list above and let me know:
1. Which features are must-haves for log4Rich?
2. Which features can be omitted for simplicity?
3. Any specific requirements or constraints?
4. Expected use cases for log4Rich?