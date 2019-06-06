/*
 * Copyright 2019 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin2.storage.splunk;

import com.splunk.Args;
import com.splunk.Index;
import com.splunk.Service;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import zipkin2.Call;
import zipkin2.Callback;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.storage.SpanConsumer;

import static java.nio.charset.StandardCharsets.*;
import static zipkin2.storage.splunk.SplunkStorage.*;

public class SplunkSpanConsumer implements SpanConsumer {

  final SplunkStorage storage;

  SplunkSpanConsumer(SplunkStorage storage) {
    this.storage = storage;
  }

  @Override public Call<Void> accept(List<Span> spans) {
    if (spans.isEmpty()) return Call.create(null);
    return new SplunkIndexCall(storage, spans);
  }

  static class SplunkIndexCall extends Call.Base<Void> {
    final SplunkStorage storage;
    final Service splunk;
    final Index index;
    final Args indexArgs;
    final List<Span> spans;

    SplunkIndexCall(SplunkStorage storage, List<Span> spans) {
      this.storage = storage;
      this.splunk = storage.splunk();
      this.index = splunk.getIndexes().get(storage.indexName);
      this.indexArgs = storage.indexArgs;
      this.spans = spans;
    }

    @Override protected Void doExecute() throws IOException {
      try (Socket socket = index.attach(indexArgs)) {
        OutputStream os = socket.getOutputStream();
        for (Span span : spans) {
          os.write(SpanBytesEncoder.JSON_V2.encode(span));
          os.write("\r\n".getBytes(UTF_8));
        }
        os.flush();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override protected void doEnqueue(Callback<Void> callback) {
      try (Socket socket = index.attach(indexArgs)) {
        OutputStream os = socket.getOutputStream();
        for (Span span : spans) {
          os.write(ENCODER.encode(span));
          os.write("\r\n".getBytes(UTF_8));
        }
        callback.onSuccess(null);
      } catch (Exception e) {
        e.printStackTrace();
        callback.onError(e);
      }
    }

    @Override public Call<Void> clone() {
      return new SplunkIndexCall(storage, spans);
    }
  }

  public static void main(String[] args) throws Exception {
    Span span0 = Span.newBuilder()
        .traceId("e")
        .id("a")
        .name("span2")
        .duration(100)
        .remoteEndpoint(Endpoint.newBuilder().serviceName("kafka").build())
        .localEndpoint(Endpoint.newBuilder().serviceName("service1").build())
        .timestamp(System.currentTimeMillis())
        .build();
    Span span1 = Span.newBuilder()
        .traceId("e")
        .id("c")
        .name("span1")
        .duration(100)
        .putTag("test", "value1")
        .localEndpoint(Endpoint.newBuilder().serviceName("service1").build())
        .timestamp(System.currentTimeMillis() + 2)
        .build();

    SplunkStorage storage = newBuilder()
        .host("localhost")
        .username("admin")
        .password("welcome1")
        .build();
    SpanConsumer spanConsumer = storage.spanConsumer();
    spanConsumer.accept(Arrays.asList(span0, span1)).execute();
    Thread.sleep(1_000);
  }
}
