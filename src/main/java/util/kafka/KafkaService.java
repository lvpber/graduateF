package util.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

/** 单例模式 官网推荐单例模式
 * The producer is thread safe and sharing a single producer instance across threads
 * will generally be faster than having multiple instances.
 * */
public class KafkaService {
    // Beijing VM
//    private static final String HOST = "192.168.3.188:9092";
    // MSI Main VM
    private static final String HOST = "192.168.93.129:9092";
    private static KafkaProducer<String,String> kafkaProducer;

    public static void init() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", HOST);
        properties.put("acks", "all");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer = new KafkaProducer<>(properties);
    }

    // send data to kafka
    public static void sendMsgToKafka(String topic,String message) {
        //System.out.println("prepare to send msg...");
        kafkaProducer.send(new ProducerRecord<>(topic,message));
        //System.out.println("send msg over...");
    }

    public static void close(){
        kafkaProducer.close();
    }

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", HOST);
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);
        System.out.println("sender ...");
        int i = 1;
        producer.send(new ProducerRecord<>("test", "fuck", "you"));
        System.out.println("over ...");
        producer.close();
    }
}