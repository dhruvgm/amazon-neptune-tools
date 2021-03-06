package com.amazonaws.services.neptune.graph;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;

public class NeptuneClient implements AutoCloseable {

    public static final int DEFAULT_BATCH_SIZE = 64;

    public static NeptuneClient create(String endpoint, int port, ConcurrencyConfig concurrencyConfig) {
        return create(endpoint, port, concurrencyConfig, DEFAULT_BATCH_SIZE);
    }

    public static NeptuneClient create(String endpoint, int port, ConcurrencyConfig concurrencyConfig, int batchSize) {
        Cluster.Builder builder = Cluster.build()
                .addContactPoint(endpoint)
                .port(port)
                .serializer(Serializers.GRYO_V3D0)
                .resultIterationBatchSize(batchSize);

        return new NeptuneClient(concurrencyConfig.applyTo(builder).create());
    }

    private final Cluster cluster;

    private NeptuneClient(Cluster cluster) {
        this.cluster = cluster;
    }

    public GraphTraversalSource newTraversalSource() {
        return EmptyGraph.instance().traversal().withRemote(DriverRemoteConnection.using(cluster));
    }

    public QueryClient queryClient(){
        return new QueryClient(cluster.connect());
    }

    @Override
    public void close() throws Exception {
        if (cluster != null && !cluster.isClosed() && !cluster.isClosing()) {
            cluster.close();
        }
    }

    public static class QueryClient implements AutoCloseable{

        private final Client client;

        QueryClient(Client client) {
            this.client = client;
        }

        public ResultSet submit(String  gremlin){
            return client.submit(gremlin);
        }

        @Override
        public void close() throws Exception {
            client.close();
        }
    }

}
