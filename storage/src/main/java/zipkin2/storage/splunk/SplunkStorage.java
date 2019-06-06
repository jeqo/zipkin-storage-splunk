package zipkin2.storage.splunk;

import com.splunk.Args;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import zipkin2.codec.SpanBytesDecoder;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.storage.ServiceAndSpanNames;
import zipkin2.storage.SpanConsumer;
import zipkin2.storage.SpanStore;
import zipkin2.storage.StorageComponent;

public class SplunkStorage extends StorageComponent {
  static final SpanBytesDecoder DECODER =SpanBytesDecoder.JSON_V2;
  static final SpanBytesEncoder ENCODER = SpanBytesEncoder.JSON_V2;

  final ServiceArgs serviceArgs;
  final String indexName;
  final Args indexArgs;
  final String sourceType;

  volatile Service splunk;

  SplunkStorage(Builder builder) {
    this.serviceArgs = new ServiceArgs();
    this.serviceArgs.setHost(builder.host);
    this.serviceArgs.setPort(builder.port);
    this.serviceArgs.setUsername(builder.username);
    this.serviceArgs.setPassword(builder.password);
    this.serviceArgs.setScheme(builder.scheme);
    this.serviceArgs.setSSLSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
    this.indexName = builder.indexName;
    this.indexArgs = new Args();
    this.indexArgs.add("source", builder.source);
    this.indexArgs.add("sourcetype", builder.sourceType);
    this.sourceType = builder.sourceType;
  }

  @Override public SpanStore spanStore() {
    return new SplunkSpanStore(this);
  }

  @Override public SpanConsumer spanConsumer() {
    return new SplunkSpanConsumer(this);
  }

  @Override public ServiceAndSpanNames serviceAndSpanNames() {
    return new SplunkSpanStore(this);
  }

  Service splunk() {
    if (splunk == null) {
      synchronized (this) {
        if (splunk == null) {
          this.splunk = Service.connect(serviceArgs);
        }
      }
    }
    return splunk;
  }

  public static class Builder extends StorageComponent.Builder {

    String scheme = "https";
    String host;
    int port = 8089;
    String username;
    String password;
    String indexName = "zipkin";
    String source = "zipkin-server";
    String sourceType = "span";

    @Override public StorageComponent.Builder strictTraceId(boolean b) {
      return null;
    }

    @Override public StorageComponent.Builder searchEnabled(boolean b) {
      return null;
    }

    public Builder indexName(String indexName) {
      if (indexName == null) throw new NullPointerException("indexName == null");
      this.indexName = indexName;
      return this;
    }

    public Builder host(String host) {
      if (host == null) throw new NullPointerException("host == null");
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      if (port == 0) throw new NullPointerException("port == null");
      this.port = port;
      return this;
    }

    public Builder username(String username) {
      if (username == null) throw new NullPointerException("username == null");
      this.username = username;
      return this;
    }

    public Builder password(String password) {
      if (password == null) throw new NullPointerException("password == null");
      this.password = password;
      return this;
    }

    public Builder scheme(String scheme) {
      if (scheme == null) throw new NullPointerException("scheme == null");
      this.scheme = scheme;
      return this;
    }

    public Builder source(String source) {
      if (source == null) throw new NullPointerException("source == null");
      this.source = source;
      return this;
    }

    public Builder sourceType(String sourceType) {
      if (sourceType == null) throw new NullPointerException("sourceType == null");
      this.sourceType = sourceType;
      return this;
    }

    @Override public SplunkStorage build() {
      return new SplunkStorage(this);
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }
}
