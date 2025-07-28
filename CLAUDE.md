# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

log4Rich is a high-performance Java logging framework designed for ultra-fast logging with advanced features including memory-mapped files, batch processing, asynchronous logging, and adaptive compression management. Current version: **1.0.5-RELEASE**.

## Build and Development Commands

### Build Commands
```bash
# Clean and compile
mvn clean compile

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PerformanceBenchmarkTest
mvn test -Dtest=AsyncCompressionTest
mvn test -Dtest=JsonLayoutTest

# Build JAR with dependencies
mvn package

# Generate JavaDoc
mvn javadoc:javadoc

# Clean everything
mvn clean
```

### Demo Applications
```bash
# Run main usage demo (comprehensive feature demonstration)
mvn compile exec:java -Dexec.mainClass="com.log4rich.Log4RichUsageDemo"

# Run JSON logging demo
mvn compile exec:java -Dexec.mainClass="com.log4rich.JsonLoggingTestApp"

# Run version check
java -cp target/classes com.log4rich.Log4Rich --version
java -cp target/classes com.log4rich.Log4Rich --banner
```

### Performance Testing
```bash
# Run performance benchmarks
mvn test -Dtest=PerformanceBenchmarkTest
mvn test -Dtest=AsyncPerformanceBenchmark

# Run async compression tests
mvn test -Dtest=AsyncCompressionTest

# Run stress tests
mvn test -Dtest=StressTest
```

## High-Level Architecture

### Core Framework Design
The framework follows a layered architecture with clear separation of concerns:

**API Layer (Facade)**
- `Log4Rich.java` - Main static API facade for easy access
- `LogManager.java` - Logger factory and registry with thread-safe singleton management
- `Logger.java` - Core logging implementation with SLF4J-style placeholder support

**Configuration System**
- Multi-location configuration loading (classpath, filesystem, environment variables)
- Runtime configuration management with hot reloading capabilities
- Environment variable overrides (29 LOG4RICH_* variables for container deployments)
- Validation with detailed error messages and fix guidance

**Appender Architecture**
- Plugin-based appender system with common interface
- High-performance appenders: `MemoryMappedFileAppender` (5.4x faster), `BatchingFileAppender` (23x faster)
- Async wrapper capability to make any appender asynchronous
- Built-in compression support with adaptive file size management

**Asynchronous Logging System**
- Lock-free ring buffer implementation for sub-microsecond latency
- Configurable overflow strategies (DROP_OLDEST, BLOCK, DISCARD)
- Background compression with queue monitoring and adaptive management
- Thread-safe async logger wrapper for existing synchronous appenders

### Key Performance Features
1. **Memory-Mapped I/O**: Eliminates kernel/user space context switching
2. **Lock-Free Ring Buffers**: Sub-microsecond async logging latency
3. **Intelligent Batching**: Reduces I/O operations through configurable batch processing
4. **Zero-Allocation Mode**: Object pools eliminate GC pressure in critical paths
5. **Adaptive Compression**: Automatically doubles file size limits when compression can't keep pace

### Configuration Philosophy
- **Backward Compatibility**: All new features maintain compatibility with existing configurations
- **Performance-First**: Default settings prioritize performance over safety where reasonable
- **Runtime Reconfiguration**: Most settings can be changed without restart
- **Environment-Friendly**: Full Docker/Kubernetes support via environment variables

## Key Implementation Patterns

### Async Compression with Adaptive Management
The framework includes intelligent compression overload protection:
- Monitors compression queue utilization in real-time
- Automatically doubles file size limits when queue reaches critical threshold (25 items)
- Logs all adaptive changes in CAPITAL LETTERS for visibility
- Prevents memory exhaustion from unbounded queues

### Thread Safety Strategy
- `ConcurrentHashMap` for logger registry in LogManager
- `CopyOnWriteArrayList` for appender collections in Logger
- Lock-free ring buffers for async logging operations
- Volatile fields for configuration flags with safe publication

### SLF4J Compatibility Layer
- Full {} placeholder support with identical semantics to SLF4J
- Automatic exception detection in parameter arrays
- Mixed parameter/throwable handling for easy migration
- Performance optimized with minimal object allocation

### JSON Structured Logging
- Built-in JSON layout support without external dependencies
- Multiple pre-configured layouts (Compact, Pretty, Minimal, Production)
- Customizable additional fields for structured data
- Superior performance compared to standard layouts (18% faster in benchmarks)

## Testing Strategy

### Test Categories
- **Unit Tests**: Individual component testing with isolated dependencies
- **Integration Tests**: End-to-end functionality with real file I/O
- **Performance Tests**: Throughput and latency benchmarking with statistical analysis
- **Stress Tests**: High-load scenarios with concurrent thread testing
- **Compression Tests**: Async compression behavior under various load conditions

### Important Test Utilities
- `SlowCompressionManager` - Simulates compression delays for testing adaptive behavior
- `JsonLoggingTestApp` - Comprehensive JSON logging demonstration
- `Log4RichUsageDemo` - Complete feature demonstration with performance comparisons

### Performance Benchmarking
The framework includes comprehensive performance testing with:
- Single-threaded and multi-threaded throughput measurements
- Latency distribution analysis
- Memory usage tracking
- Comparison with standard Java logging frameworks

## Configuration Files

### Primary Configurations
- `log4Rich.demo.config` - Comprehensive configuration template with all features
- `log4Rich.sample.config` - Basic configuration example
- `json-test.config` - JSON logging specific configuration

### Configuration Loading Order
1. System property: `-Dlog4rich.config=/path/to/config`
2. Classpath: `log4Rich.config`
3. Current directory: `./log4Rich.config`
4. Parent directory: `../log4Rich.config`
5. Config directories: `./config/`, `./conf/`, `../config/`, `../conf/`

## Development Best Practices

### When Adding New Features
1. Maintain backward compatibility with existing configurations
2. Add comprehensive test coverage including edge cases
3. Update configuration validation with specific error messages
4. Consider performance impact and add benchmarks if needed
5. Update JavaDoc with examples and performance characteristics
6. Add environment variable support for container deployments

### Performance Considerations
- Always disable location capture in production (`log4rich.location.capture=false`)
- Use appropriate buffer sizes for file I/O operations
- Consider async compression for high-volume logging scenarios
- Monitor compression queue statistics in production deployments
- Leverage memory-mapped files for maximum throughput

### Repository Management
**IMPORTANT**: Use `gh` (GitHub CLI) for all repository operations instead of raw `git` commands.

### Code Quality Standards
- Thread-safety is mandatory for all core components
- Comprehensive JavaDoc documentation for public APIs
- Zero external dependencies for core functionality
- Performance regression testing for all changes
- Detailed error messages with actionable fix guidance