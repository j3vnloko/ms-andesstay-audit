package cl.duoc.andesstay.audit.config;

import cl.duoc.andesstay.audit.exception.InvalidEventException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Manejo de fallos del consumer segun la topologia del caso: tras N reintentos
 * el mensaje se publica en un topico DLT propio de este consumidor
 * ("<topico>.audit.DLT") con los metadatos del error en los headers
 * (excepcion, topico/particion/offset de origen), en vez de perderse.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${audit.kafka.topic}")
    private String topic;

    @Value("${audit.kafka.dlt-suffix:.audit.DLT}")
    private String dltSuffix;

    @Value("${audit.kafka.retry.attempts:3}")
    private long retryAttempts;

    @Value("${audit.kafka.retry.interval-ms:1000}")
    private long retryIntervalMs;

    @Value("${audit.kafka.partitions:3}")
    private int partitions;

    @Value("${audit.kafka.replicas:1}")
    private int replicas;

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                // particion -1: el productor elige, asi el DLT no depende de tener
                // las mismas particiones que el topico de origen.
                (record, ex) -> new TopicPartition(record.topic() + dltSuffix, -1));

        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer, new FixedBackOff(retryIntervalMs, retryAttempts));
        // Un evento mal formado no mejora reintentandolo: directo al DLT.
        handler.addNotRetryableExceptions(InvalidEventException.class);
        return handler;
    }

    /** Crea el topico DLT al arrancar (si no existe). En AWS: audit.kafka.replicas=3. */
    @Bean
    public NewTopic auditDeadLetterTopic() {
        return TopicBuilder.name(topic + dltSuffix)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
