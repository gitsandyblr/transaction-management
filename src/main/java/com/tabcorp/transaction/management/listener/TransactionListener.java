package com.tabcorp.transaction.management.listener;

import com.tabcorp.transaction.management.entity.Transaction;
import com.tabcorp.transaction.management.service.TransactionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionListener {

    private final TransactionService transactionService;
    private final MeterRegistry meterRegistry;
    
    private Timer jsonProcessingTimer;
    private Timer bsonProcessingTimer;

    @PostConstruct
    public void init() {
        jsonProcessingTimer = Timer.builder("transaction.processing")
            .tag("format", "json")
            .description("Timer for JSON transaction processing")
            .register(meterRegistry);
            
        bsonProcessingTimer = Timer.builder("transaction.processing")
            .tag("format", "bson")
            .description("Timer for BSON transaction processing")
            .register(meterRegistry);
    }

    @KafkaListener(
        topics = "${kafka.topic.json-transactions}",
        groupId = "${kafka.group.json-transactions}",
        containerFactory = "jsonKafkaListenerContainerFactory"
    )
    public void consumeJsonTransactions(List<ConsumerRecord<String, Transaction>> records) {
        AtomicInteger batchSize = new AtomicInteger(records.size());
        log.info("Received batch of {} JSON transactions", batchSize.get());
        
        Timer.Sample timer = Timer.start();
        
        Flux.fromIterable(records)
            .map(ConsumerRecord::value)
            .map(transaction -> {
                transaction.setDataFormat("JSON");
                return transaction;
            })
            .buffer(100)
            .parallel(3)
            .runOn(Schedulers.boundedElastic())
            .flatMap(batch -> {
                log.debug("Processing sub-batch of {} JSON transactions", batch.size());
                return transactionService.processJsonTransactions(batch)
                    .doOnError(error -> {
                        log.error("Error processing JSON batch: {}", error.getMessage(), error);
                        meterRegistry.counter("transaction.errors", "format", "json").increment();
                    });
            })
            .sequential()
            .doOnComplete(() -> {
                timer.stop(jsonProcessingTimer);
                log.info("Completed processing {} JSON transactions", batchSize.get());
                meterRegistry.counter("transaction.processed", "format", "json")
                    .increment(batchSize.get());
            })
            .subscribe();
    }


    @KafkaListener(
        topics = "${kafka.topic.bson-transactions}",
        groupId = "${kafka.group.bson-transactions}",
        containerFactory = "bsonKafkaListenerContainerFactory"
    )
    public void consumeBsonTransactions(List<ConsumerRecord<String, Transaction>> records) {
        AtomicInteger batchSize = new AtomicInteger(records.size());
        log.info("Received batch of {} BSON transactions", batchSize.get());

        Timer.Sample timer = Timer.start();

        Flux.fromIterable(records)
            .map(ConsumerRecord::value)
            .map(transaction -> {
                transaction.setDataFormat("BSON");
                return transaction;
            })
            .buffer(100)
            .parallel(3)
            .runOn(Schedulers.boundedElastic())
            .flatMap(batch -> {
                log.debug("Processing sub-batch of {} BSON transactions", batch.size());
                return transactionService.processBsonTransactions(batch)
                    .doOnError(error -> {
                        log.error("Error processing BSON batch: {}", error.getMessage(), error);
                        meterRegistry.counter("transaction.errors", "format", "bson").increment();
                    });
            })
            .sequential()
            .doOnComplete(() -> {
                timer.stop(bsonProcessingTimer);
                log.info("Completed processing {} BSON transactions", batchSize.get());
                meterRegistry.counter("transaction.processed", "format", "bson")
                    .increment(batchSize.get());
            })
            .subscribe();
    }

}
