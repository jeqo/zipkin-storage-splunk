package zipkin2.autoconfigure.storage.splunk;

import java.io.Serializable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import zipkin2.storage.StorageComponent;
import zipkin2.storage.splunk.SplunkStorage;

@ConfigurationProperties("zipkin.storage.splunk")
public class ZipkinSplunkStorageProperties implements Serializable {
  private static final long serialVersionUID = 0L;

  private String scheme;
  private String host;
  private int port;
  private String username;
  private String password;
  private String indexName;
  private String sourceType;
  private String source;

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getIndexName() {
    return indexName;
  }

  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public StorageComponent.Builder toBuilder() {
    return SplunkStorage.newBuilder()
        .host(host)
        .port(port)
        .scheme(scheme)
        .username(username)
        .password(password)
        .indexName(indexName)
        .source(source)
        .sourceType(sourceType);
  }
}
