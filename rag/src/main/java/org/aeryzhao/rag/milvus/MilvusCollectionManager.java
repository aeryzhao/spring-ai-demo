package org.aeryzhao.rag.milvus;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MilvusCollectionManager {

    private static final String FIELD_ID = "id";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_EMBEDDING = "embedding";
    private static final String FIELD_METADATA = "metadata";

    @Autowired
    private MilvusServiceClient milvusClient;

    @Value("${rag.vector.collection-name}")
    private String collectionName;

    @Value("${rag.vector.dimension}")
    private Integer dimension;

    @Value("${rag.vector.index-type}")
    private String indexType;

    @Value("${rag.vector.metric-type}")
    private String metricType;

    @Value("${rag.vector.nlist}")
    private Integer nlist;

    public boolean hasCollection(String collectionName) {
        try {
            R<Boolean> response = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Collection '{}' exists: {}", collectionName, response.getData());
                return response.getData();
            } else {
                log.error("Failed to check collection existence: {}", response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error checking collection existence: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean createCollection() {
        return createCollection(collectionName);
    }

    public boolean createCollection(String collectionName) {
        try {
            if (hasCollection(collectionName)) {
                log.warn("Collection '{}' already exists", collectionName);
                return true;
            }

            FieldType idField = FieldType.newBuilder()
                    .withName(FIELD_ID)
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();

            FieldType contentField = FieldType.newBuilder()
                    .withName(FIELD_CONTENT)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(65535)
                    .build();

            FieldType embeddingField = FieldType.newBuilder()
                    .withName(FIELD_EMBEDDING)
                    .withDataType(DataType.FloatVector)
                    .withDimension(dimension)
                    .build();

            FieldType metadataField = FieldType.newBuilder()
                    .withName(FIELD_METADATA)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(65535)
                    .build();

            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription("RAG document collection for semantic search")
                    .withShardsNum(2)
                    .addFieldType(idField)
                    .addFieldType(contentField)
                    .addFieldType(embeddingField)
                    .addFieldType(metadataField)
                    .build();

            R<RpcStatus> response = milvusClient.createCollection(createParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Collection '{}' created successfully", collectionName);
                
                TimeUnit.MILLISECONDS.sleep(500);
                
                boolean indexCreated = createIndex(collectionName);
                if (!indexCreated) {
                    log.error("Failed to create index for collection '{}'", collectionName);
                    return false;
                }
                
                return true;
            } else {
                log.error("Failed to create collection '{}': {}", collectionName, response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error creating collection '{}': {}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    public boolean createIndex(String collectionName) {
        try {
            IndexType indexTypeEnum = IndexType.valueOf(indexType);
            MetricType metricTypeEnum = MetricType.valueOf(metricType);

            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName(FIELD_EMBEDDING)
                    .withIndexType(indexTypeEnum)
                    .withMetricType(metricTypeEnum)
                    .withExtraParam("{\"nlist\":" + nlist + "}")
                    .withIndexName("embedding_index")
                    .withSyncMode(Boolean.TRUE)
                    .build();

            R<RpcStatus> response = milvusClient.createIndex(indexParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Index created successfully for collection '{}' on field '{}' with index type {} and metric type {}", 
                        collectionName, FIELD_EMBEDDING, indexType, metricType);
                return true;
            } else {
                log.error("Failed to create index for collection '{}': {}", collectionName, response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error creating index for collection '{}': {}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    public boolean loadCollection(String collectionName) {
        try {
            R<RpcStatus> response = milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Collection '{}' loaded into memory successfully", collectionName);
                return true;
            } else {
                log.error("Failed to load collection '{}': {}", collectionName, response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error loading collection '{}': {}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    public boolean releaseCollection(String collectionName) {
        try {
            R<RpcStatus> response = milvusClient.releaseCollection(ReleaseCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Collection '{}' released from memory", collectionName);
                return true;
            } else {
                log.error("Failed to release collection '{}': {}", collectionName, response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error releasing collection '{}': {}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    public boolean dropCollection(String collectionName) {
        try {
            if (!hasCollection(collectionName)) {
                log.warn("Collection '{}' does not exist", collectionName);
                return true;
            }

            R<RpcStatus> response = milvusClient.dropCollection(DropCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Collection '{}' dropped successfully", collectionName);
                return true;
            } else {
                log.error("Failed to drop collection '{}': {}", collectionName, response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error dropping collection '{}': {}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    public void printCollectionInfo(String collectionName) {
        try {
            if (!hasCollection(collectionName)) {
                log.warn("Collection '{}' does not exist", collectionName);
                return;
            }

            log.info("=== Collection Info: {} ===", collectionName);
            log.info("Dimension: {}", dimension);
            log.info("Index Type: {}", indexType);
            log.info("Metric Type: {}", metricType);
            log.info("Nlist: {}", nlist);
            log.info("Fields:");
            log.info("  - id: INT64, Primary Key");
            log.info("  - content: VARCHAR (max 65535)");
            log.info("  - embedding: FLOAT_VECTOR (dimension {})", dimension);
            log.info("  - metadata: VARCHAR (max 65535)");
        } catch (Exception e) {
            log.error("Error printing collection info: {}", e.getMessage(), e);
        }
    }
}
