# sync4j-pcloud

[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/sync4j-pcloud)](https://search.maven.org/artifact/com.fathzer/sync4j-pcloud)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Javadocs](https://www.javadoc.io/badge/com.fathzer/sync4j-pcloud.svg)](https://www.javadoc.io/doc/com.fathzer/sync4j-pcloud)
[![SonarCloud](https://sonarcloud.io/api/project_badges/measure?project=sync4j_sync4j-pcloud&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=sync4j_sync4j-pcloud)

This is a provider for [sync4j](https://github.com/fathzer/sync4j) that allows to synchronize files with [pCloud](https://www.pcloud.com/).

## Requirements

- Java 17
- A pCloud token (see [here](https://github.com/sync4j/sync4j-pcloud/wiki/How-to-create-a-pCloud-token) to know how to get one).

## Installation

Import with Maven:

```xml
<dependency>
    <groupId>com.fathzer</groupId>
    <artifactId>sync4j-pcloud</artifactId>
    <version>0.0.1</version>
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

## Errors

The provider follows the error contract of the [FileProvider](https://github.com/sync4j/sync4j/blob/main/src/main/java/com/fathzer/sync4j/FileProvider.java) interface and never retries by itself (the synchronization does):
- pCloud rate limiting (4xxx errors), server side errors (5xxx errors), errors that are "safe to retry" (19xx), HTTP 429/500/502/503/504/509 statuses, timeouts and network errors are reported with a `RetryableIOException` (the Retry-After header, if any, is available through `getRetryAfter()`).
- A missing entry is reported with a `FileNotFoundException`.
- `Folder.mkdir` is idempotent: it returns the existing folder if there is one, and throws a `FileAlreadyExistsException` if a file has the same name.
- Any other error is a plain `IOException` (its cause is the pCloud `ApiError`).

## Use pCloud from a configuration file

This library ships a `FileProviderFactory` (type `pCloud`), declared as a service. A program that reads configuration files with [sync4j-config](https://github.com/sync4j/sync4j-config) (like [sync4j-web](https://github.com/sync4j/sync4j-web)) can use pCloud as soon as this library is on its classpath:

```json
{ "provider": "pCloud", "path": "/Photos", "config": { "zone": "us", "token": "${PCLOUD_TOKEN}" } }
```

`zone` is `us` or `eu`, `token` is the access token (it can refer to an environment variable).

## Running Tests

This project includes integration tests that communicate with the pCloud API.
These tests require a valid pCloud access token to run.

To execute the full suite of tests including integration tests, you must provide the `pcloud.token` system property:

```bash
mvn test -Dpcloud.token=YOUR_ACCESS_TOKEN
```

If the `pcloud.token` property is missing, the integration tests will be automatically skipped.

You can also specify the zone (defaults to `US` if not specified):

```bash
mvn test -Dpcloud.token=YOUR_ACCESS_TOKEN -Dpcloud.zone=EU
```

**Note for contributors:** Pull requests from forked repositories do not have access to the repository secrets. Consequently, the integration tests will be skipped in the CI pipeline for these PRs.
This is a standard security measure. If you want to run these tests, you must do so locally as described above.
A maintainer can manually run the integration tests for a PR by pushing the PR's branch to this repository.
