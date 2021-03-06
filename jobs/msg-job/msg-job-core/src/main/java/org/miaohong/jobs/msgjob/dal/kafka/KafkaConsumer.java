package org.miaohong.jobs.msgjob.dal.kafka;

import com.alibaba.fastjson.JSON;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.miaohong.jobs.msgjob.dal.hbase.HBaseManager;
import org.miaohong.jobs.msgjob.dal.model.KafkaP2PMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by miaohong on 17/1/10.
 */
@Component
public class KafkaConsumer implements Runnable{
    @Value("${kafka.consumer.bootstrap.servers}")
    private String bootstrapServers;
    @Value("${kafka.consumer.group.id}")
    private String groupId;
    @Value("${kafka.consumer.topics}")
    private String topics;

    @Resource
    HBaseManager hBaseManager;

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    private org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer = null;
    private int id = 0;

    private void init() {
        List<String> topicList = new ArrayList<>();
        String[] tmpTopics = topics.split(",");
        for (String t : tmpTopics) {
            topicList.add(t);
        }
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        this.consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
        this.consumer.subscribe(topicList);
        hBaseManager.init();
    }

    private void consume() {
        logger.info("consume");
        while (true) {
            List<KafkaP2PMsg> kafkaP2PMsgs = new ArrayList<>();
            ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
            for (ConsumerRecord<String, String> record : records) {
                Map<String, Object> data = new HashMap<>();
                data.put("partition", record.partition());
                data.put("offset", record.offset());
                data.put("value", record.value());
                System.out.println(this.id + ": " + data);
                KafkaP2PMsg kafkaP2PMsg = JSON.parseObject(record.value(), KafkaP2PMsg.class);
                kafkaP2PMsgs.add(kafkaP2PMsg);
            }
            System.out.println(kafkaP2PMsgs);
            hBaseManager.insert(kafkaP2PMsgs);
        }
    }

    @Override
    public void run() {
        System.out.println("run");
        init();
        consume();
    }
}
