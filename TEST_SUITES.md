# Test Suites Guide

This document describes how to run different test suites in the log4Rich project. The tests have been categorized into different groups to allow for flexible test execution based on your needs.

## Test Categories

### Fast Tests (Default)
- **What**: Unit tests that run quickly (< 5 seconds each)
- **When to use**: Regular development, CI/CD pipelines, pre-commit hooks
- **Command**: `mvn test` (default behavior)

### Slow Tests  
- **What**: Tests with longer execution times (> 10 seconds each) including compression tests
- **When to use**: Before releases, detailed testing
- **Tests included**: AsyncCompressionTest, specific AsyncLoggingTest methods

### Performance Tests
- **What**: Benchmarking tests that measure throughput and latency
- **When to use**: Performance regression testing, optimization validation
- **Tests included**: PerformanceBenchmarkTest, AsyncPerformanceBenchmark
- **Duration**: 2-5 minutes per test class

### Stress Tests
- **What**: High-load concurrent tests with thousands of operations
- **When to use**: Load testing, stability validation
- **Tests included**: StressTest
- **Duration**: 3-10 minutes per test class

## Running Different Test Suites

### Default Build (Fast Tests Only)
```bash
# Runs only fast tests, excludes slow/performance/stress tests
mvn test
```

### All Tests (Including Slow Ones)
```bash
# Runs every test in the project
mvn test -P all-tests
```

### Only Slow Tests
```bash
# Runs only slow, performance, and stress tests
mvn test -P slow-tests
```

### Only Performance Tests
```bash
# Runs only performance benchmark tests
mvn test -P performance-tests
```

### Only Stress Tests
```bash
# Runs only stress tests
mvn test -P stress-tests
```

### Fast Tests Explicitly
```bash
# Explicitly runs only fast tests
mvn test -P fast-tests
```

## Test Execution Times

| Test Suite | Typical Duration | Test Count |
|------------|------------------|------------|
| Fast Tests | 10-30 seconds | ~15 tests |
| Performance Tests | 2-5 minutes | ~15 tests |
| Stress Tests | 3-10 minutes | ~6 tests |
| All Tests | 5-15 minutes | ~35+ tests |

## CI/CD Integration

### Recommended CI Pipeline
```yaml
# Fast feedback for pull requests
- name: Fast Tests
  run: mvn test

# Nightly or release builds
- name: Full Test Suite
  run: mvn test -P all-tests
```

### GitHub Actions Example
```yaml
jobs:
  fast-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 8
        uses: actions/setup-java@v3
        with:
          java-version: '8'
          distribution: 'temurin'
      - name: Run fast tests
        run: mvn test

  performance-tests:
    runs-on: ubuntu-latest
    if: github.event_name == 'schedule' || github.event_name == 'release'
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 8
        uses: actions/setup-java@v3
        with:
          java-version: '8' 
          distribution: 'temurin'
      - name: Run performance tests
        run: mvn test -P performance-tests
```

## Tagged Test Classes

### @Tag("performance")
- `PerformanceBenchmarkTest` - Comprehensive performance benchmarks
- `AsyncPerformanceBenchmark` - Async logging performance validation

### @Tag("stress") 
- `StressTest` - High-load concurrent testing

### @Tag("slow")
- `AsyncCompressionTest` - Compression functionality tests
- Individual test methods with high iteration counts
- Tests with long timeouts (>10 seconds)

## IDE Integration

### IntelliJ IDEA
1. Go to Run/Debug Configurations
2. Create new JUnit configuration
3. In "Test kind" select "Tags"
4. Specify tag expression: `!slow & !performance & !stress` for fast tests

### Eclipse
1. Right-click on project → Run As → Run Configurations
2. Create new JUnit configuration  
3. In "Test" tab, use "Run all tests in the selected project"
4. In "Arguments" tab, add VM arguments: `-Dgroups="!slow,!performance,!stress"`

## Custom Test Execution

### Run Specific Test Categories
```bash
# Run only compression-related tests
mvn test -Dtest="*Compression*"

# Run tests matching a pattern
mvn test -Dtest="*Performance*,*Benchmark*"

# Exclude specific slow tests
mvn test -Dtest="!StressTest"
```

### Environment-Specific Testing
```bash
# Development environment (fast feedback)
mvn test -P fast-tests

# Staging environment (comprehensive)  
mvn test -P all-tests

# Production deployment (performance validation)
mvn test -P performance-tests
```

## Troubleshooting

### Tests Take Too Long
- Use `mvn test` instead of `mvn test -P all-tests`
- Run specific test categories: `mvn test -P fast-tests`

### Performance Tests Fail
- Ensure adequate system resources (CPU, memory)
- Run on a dedicated test machine
- Consider adjusting performance thresholds in test code

### Memory Issues
- Increase JVM heap size: `export MAVEN_OPTS="-Xmx2g"`
- Run tests sequentially: `mvn test -Dfork.count=1`

### CI Pipeline Timeouts
- Split test execution across multiple jobs
- Use fast tests for PR validation
- Schedule comprehensive tests for nightly builds

## Best Practices

1. **Development Workflow**: Use fast tests during active development
2. **Pre-commit**: Run `mvn test` before committing changes
3. **Release Testing**: Run full test suite (`mvn test -P all-tests`) before releases
4. **Performance Monitoring**: Run performance tests regularly to catch regressions
5. **Resource Management**: Allocate sufficient time and resources for comprehensive testing

## Test Output Interpretation

### Performance Test Results
Performance tests output metrics including:
- Throughput (messages/second)
- Latency (nanoseconds/microseconds per message)
- Memory usage statistics
- Comparative performance improvements

### Stress Test Results  
Stress tests validate:
- Concurrent access safety
- Memory leak prevention
- Configuration stability under load
- Error handling robustness

For more detailed information about specific tests, see the JavaDoc documentation in each test class.