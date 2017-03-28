package cz.tomasdvorak.elastic.labelling.elastic;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ElasticClient {

    private TransportClient client;

    @Value("${elastic.host}")
    private String host;

    @Value("${elastic.port}")
    private int port;

    @Value("${elastic.username}")
    private String username;

    @Value("${elastic.password}")
    private String password;

    public TransportClient getClient() {
        return client;
    }

    @PostConstruct
    public void postConstruct() throws UnknownHostException {
        this.client = new PreBuiltXPackTransportClient(Settings.builder()
                .put("cluster.name", "docker-cluster")
                .put("xpack.security.user", username + ":" + password)
                .build())
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));
    }

}
