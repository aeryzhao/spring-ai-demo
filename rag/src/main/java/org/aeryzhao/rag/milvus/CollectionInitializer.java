package org.aeryzhao.rag.milvus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CollectionInitializer implements ApplicationRunner {

    @Autowired
    private MilvusCollectionManager collectionManager;

    @Value("${rag.vector.collection-name}")
    private String collectionName;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== Starting Milvus Collection Initialization ===");
        log.info("Collection Name: {}", collectionName);
        
        try {
            boolean exists = collectionManager.hasCollection(collectionName);
            
            if (exists) {
                log.info("Collection '{}' already exists", collectionName);
                collectionManager.printCollectionInfo(collectionName);
                
                boolean loaded = collectionManager.loadCollection(collectionName);
                if (loaded) {
                    log.info("Collection '{}' loaded into memory successfully", collectionName);
                } else {
                    log.warn("Failed to load collection '{}' into memory", collectionName);
                }
            } else {
                log.info("Collection '{}' does not exist, creating...", collectionName);
                
                boolean created = collectionManager.createCollection(collectionName);
                
                if (created) {
                    log.info("Collection '{}' created successfully", collectionName);
                    collectionManager.printCollectionInfo(collectionName);
                    
                    boolean loaded = collectionManager.loadCollection(collectionName);
                    if (loaded) {
                        log.info("Collection '{}' loaded into memory successfully", collectionName);
                    } else {
                        log.warn("Failed to load collection '{}' into memory", collectionName);
                    }
                } else {
                    log.error("Failed to create collection '{}'", collectionName);
                }
            }
            
            log.info("=== Milvus Collection Initialization Completed ===");
        } catch (Exception e) {
            log.error("Error during collection initialization: {}", e.getMessage(), e);
            throw e;
        }
    }
}
