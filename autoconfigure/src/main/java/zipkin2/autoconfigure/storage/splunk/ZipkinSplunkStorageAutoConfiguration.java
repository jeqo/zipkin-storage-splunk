package zipkin2.autoconfigure.storage.splunk;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.storage.StorageComponent;

@Configuration
@EnableConfigurationProperties(ZipkinSplunkStorageProperties.class)
@ConditionalOnProperty(name = "zipkin.storage.type", havingValue = "splunk")
@ConditionalOnMissingBean(StorageComponent.class)
public class ZipkinSplunkStorageAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  StorageComponent storage(ZipkinSplunkStorageProperties properties) {
    return properties.toBuilder().build();
  }

}
