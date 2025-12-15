# sync4j-pcloud

[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/sync4j-pcloud)](https://search.maven.org/artifact/com.fathzer/sync4j-pcloud)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Javadocs](https://www.javadoc.io/badge/com.fathzer/sync4j-pcloud.svg)](https://www.javadoc.io/doc/com.fathzer/sync4j-pcloud)
[![SonarCloud](https://sonarcloud.io/api/project_badges/measure?project=fathzer_sync4j-pcloud&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fathzer_sync4j-pcloud)

This is a provider for [sync4j](https://github.com/fathzer/sync4j) that allows to synchronize files with [pCloud](https://www.pcloud.com/).

## Requirements

- Java 17
- A pCloud token.

## Installation

Import with Maven:

```xml
<dependency>
    <groupId>com.fathzer</groupId>
    <artifactId>sync4j-pcloud</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Usage

```java
try (FileProvider provider = new PCloudProvider(Zone.US, accessToken)) {
    Entry entry = provider.get("/path/to/file");
    if (entry.isFile()) {
        File file = entry.asFile();
    }
    if (entry.isFolder()) {
        Folder folder = entry.asFolder();
    }
}
```

