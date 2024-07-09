package org.dromara.stream.config;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

/**
 * kafka stream 配置
 *
 * @author LionLi
 */
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean
    public KStream<String, String> demoStream(StreamsBuilder builder) {
        // 输入主题
        KStream<String, String> source = builder.stream("input-topic");
        // 转换逻辑：这里只是简单地将消息转换为大写
        KStream<String, String> processed = source.mapValues(value -> value.toUpperCase());
        // 输出到另一个主题
        processed.to("output-topic");
        return source;
    }

}
