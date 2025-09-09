package com.dropslot.store.config;

import com.dropslot.store.dto.UserCreatedEventDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // 🛠️ Generic method to create ConsumerFactory with custom group and offset
    private ConsumerFactory<String, UserCreatedEventDto> createConsumerFactory(String groupId, String offsetReset) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        if (offsetReset != null) {
            configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
        }
        
        // Configure JsonDeserializer for our specific DTO
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, UserCreatedEventDto.class.getName());
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    // 🏪 Store service consumer factory
    @Bean
    public ConsumerFactory<String, UserCreatedEventDto> storeConsumerFactory() {
        return createConsumerFactory("dropslot-store", null); // Uses default offset behavior
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserCreatedEventDto> storeKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserCreatedEventDto> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(storeConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // 📧 Email service consumer factory  
    @Bean
    public ConsumerFactory<String, UserCreatedEventDto> emailConsumerFactory() {
        return createConsumerFactory("dropslot-email", "earliest"); // Starts from beginning
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserCreatedEventDto> emailKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserCreatedEventDto> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(emailConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // 📊 Example: Analytics service consumer factory (if you add it later)
    // @Bean
    // public ConsumerFactory<String, UserCreatedEventDto> analyticsConsumerFactory() {
    //     return createConsumerFactory("dropslot-analytics", "earliest");
    // }
}
