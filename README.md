# Zipkin Storage: Splunk

Zipkin storage based on Splunk.

## Configuration

| Env variable | Description | Default value |
|--------------|-------------|---------------|
| SPLUNK_HOST | Splunk hostname | `localhost` |
| SPLUNK_PORT | Splunk admin port | `8089` |
| SPLUNK_USERNAME | Splunk username | `<tbd>` |
| SPLUNK_PASSWORD | SPlunk user password | `<tbd>` |
| SPLUNK_SCHEME | Splunk communication protocol. http or https | `https` |
| SPLUNK_INDEX_NAME | Splunk index name | `zipkin` |
| SPLUNK_SOURCE_TYPE | Splunk source type | `span` |
| SPLUNK_SOURCE | Splunk source | `zipkin-server` |

### Splunk Configuration

1. If running on Docker, change default password to enable access to REST services.

2. Create an index for zipkin data (e.g. index name = `zipkin`)

3. Create a source type for spans (e.g. source type = `span`)

## How to run it

### Docker

```bash
docker-compose up -d
#or
make all-docker
```

### Local

```bash
make download-zipkin
make all-local
```