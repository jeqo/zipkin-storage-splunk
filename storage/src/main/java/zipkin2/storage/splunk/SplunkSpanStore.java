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

import com.splunk.Event;
import com.splunk.ResultsReaderXml;
import com.splunk.Service;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import zipkin2.Call;
import zipkin2.Callback;
import zipkin2.DependencyLink;
import zipkin2.Span;
import zipkin2.storage.QueryRequest;
import zipkin2.storage.ServiceAndSpanNames;
import zipkin2.storage.SpanStore;

import static java.nio.charset.StandardCharsets.UTF_8;
import static zipkin2.storage.splunk.SplunkStorage.DECODER;

public class SplunkSpanStore implements SpanStore, ServiceAndSpanNames {

  final SplunkStorage storage;

  SplunkSpanStore(SplunkStorage storage) {
    this.storage = storage;
  }

  @Override public Call<List<List<Span>>> getTraces(QueryRequest queryRequest) {
    String query = "search * index=" + storage.indexName
        + " sourcetype=" + storage.sourceType
        + " | transaction traceId";
    return new GetTracesCall(storage, query);
  }

  static class GetTracesCall extends RawSplunkSearchCall<List<Span>> {

    GetTracesCall(SplunkStorage storage, String query) {
      super(storage, query);
    }

    @Override List<List<Span>> process(ResultsReaderXml results) {
      List<List<Span>> traces = new ArrayList<>();
      for (Event event : results) {
        String[] raws = event.get("_raw").split("\\n");
        List<Span> trace = new ArrayList<>();
        for (String raw : raws) {
          byte[] bytes = raw.getBytes(UTF_8);
          Span span = DECODER.decodeOne(bytes);
          trace.add(span);
        }
        traces.add(trace);
      }
      return traces;
    }

    @Override public Call<List<List<Span>>> clone() {
      return new GetTracesCall(storage, query);
    }
  }

  @Override public Call<List<Span>> getTrace(String traceId) {
    final String query = "search * index=\"" + storage.indexName + "\" "
        + "sourcetype=\"" + storage.sourceType + "\" "
        + "traceid " + traceId;
    return new GetTraceCall(storage, query, traceId);
  }

  static class GetTraceCall extends SplunkSearchCall<Span> {
    final String traceId;

    GetTraceCall(SplunkStorage storage, String query, String traceId) {
      super(storage, query);
      this.traceId = traceId;
    }

    @Override Span parse(Event event) {
      final String raw = event.get("_raw");
      final byte[] bytes = raw.getBytes(UTF_8);
      return DECODER.decodeOne(bytes);
    }

    @Override public Call<List<Span>> clone() {
      return new GetTraceCall(storage, query, traceId);
    }
  }

  @Override public Call<List<String>> getServiceNames() {
    final String query = "search * index=\"" + storage.indexName + "\" "
        + "| table localEndpoint.serviceName "
        + "| dedup localEndpoint.serviceName";
    return new GetNamesCall(storage, query, "localEndpoint.serviceName");
  }

  @Override public Call<List<String>> getRemoteServiceNames(String serviceName) {
    final String query = "search * index=\"" + storage.indexName + "\" "
        + "localEndpoint " + serviceName + " "
        + "| table remoteEndpoint.serviceName "
        + "| dedup remoteEndpoint.serviceName";
    return new GetNamesCall(storage, query, "remoteEndpoint.serviceName");
  }

  @Override public Call<List<String>> getSpanNames(String serviceName) {
    final String query = "search * index=\"" + storage.indexName + "\" "
        + "localEndpoint " + serviceName + " "
        + "| table name "
        + "| dedup name";
    return new GetNamesCall(storage, query, "name");
  }

  static class GetNamesCall extends SplunkSearchCall<String> {
    final String fieldName;

    GetNamesCall(SplunkStorage storage, String query, String fieldName) {
      super(storage, query);
      this.fieldName = fieldName;
    }

    @Override String parse(Event event) {
      return event.get(fieldName);
    }

    @Override public Call<List<String>> clone() {
      return new GetNamesCall(storage, query, fieldName);
    }
  }

  @Override public Call<List<DependencyLink>> getDependencies(long start, long end) {
    return null;
  }

  static abstract class SplunkSearchCall<T> extends RawSplunkSearchCall<T> {

    SplunkSearchCall(SplunkStorage storage, String query) {
      super(storage, query);
    }

    @Override List<T> process(ResultsReaderXml results) {
      List<T> list = new ArrayList<>();
      for (Event event : results) {
        T item = parse(event);
        list.add(item);
      }
      return list;
    }

    abstract T parse(Event event);
  }

  static abstract class RawSplunkSearchCall<T> extends Call.Base<List<T>> {
    final SplunkStorage storage;
    final Service splunk;
    final String query;

    RawSplunkSearchCall(SplunkStorage storage, String query) {
      this.storage = storage;
      this.splunk = storage.splunk();
      this.query = query;
    }

    @Override protected List<T> doExecute() throws IOException {
      try (InputStream is = splunk.oneshotSearch(query)) {
        ResultsReaderXml xml = new ResultsReaderXml(is);
        return process(xml);
      } catch (Exception e) {
        e.printStackTrace();
        return Collections.emptyList();
      }
    }

    @Override protected void doEnqueue(Callback<List<T>> callback) {
      try (InputStream is = splunk.oneshotSearch(query)) {
        ResultsReaderXml xml = new ResultsReaderXml(is);
        callback.onSuccess(process(xml));
      } catch (Exception e) {
        e.printStackTrace();
        callback.onError(e);
      }
    }

    abstract List<T> process(ResultsReaderXml results);
  }

  public static void main(String[] args) throws Exception {
    SplunkStorage storage = SplunkStorage.newBuilder()
        .host("localhost")
        .username("admin")
        .password("welcome1")
        .build();
    SpanStore spanStore = storage.spanStore();
    List<Span> spans = spanStore.getTrace("000000000000000d").execute();
    System.out.println(spans);
    ServiceAndSpanNames spanServiceNames = storage.serviceAndSpanNames();
    List<String> serviceNames = spanServiceNames.getServiceNames().execute();
    System.out.println(serviceNames);
    List<String> spanNames = spanServiceNames.getSpanNames("service1").execute();
    System.out.println(spanNames);
    List<String> remoteServiceNames = spanServiceNames.getRemoteServiceNames("service1").execute();
    System.out.println(remoteServiceNames);
    List<List<Span>> result = spanStore.getTraces(QueryRequest.newBuilder()
        .spanName("span1")
        .limit(10)
        .lookback(System.currentTimeMillis() - 1_000_000)
        .endTs(System.currentTimeMillis())
        .build()).execute();
    System.out.println(result);
  }
}
