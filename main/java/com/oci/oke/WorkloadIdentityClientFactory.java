package com.oci.oke;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest.Builder;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.ConfigFileReader;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;

/**
 * Singleton factory to provide a single instance of ObjectStorageClient
 * configured with OKE Workload Identity or local config based on environment.
 */
public class WorkloadIdentityClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(WorkloadIdentityClientFactory.class);
    private static volatile ObjectStorageClient objectStorageClientInstance;
    //private static final String OCI_REGION_ENV_VAR = "sa-bogota-1";
    //private static final String OCI_AUTH_PROVIDER = "workload_identity";

    private WorkloadIdentityClientFactory() {
        // Private constructor to prevent direct instantiation
    }

    /**
     * Returns the singleton instance of ObjectStorageClient.
     * Initializes the client if it hasn't been initialized yet.
     * It dynamically chooses between Workload Identity and local config file authentication.
     *
     * @return The singleton ObjectStorageClient instance.
     * @throws IllegalStateException if OCI_REGION environment variable is not set.
     * @throws RuntimeException if there's an issue initializing the provider or OCI client.
     */
    public static ObjectStorageClient getObjectStorageClient() {
        if (objectStorageClientInstance == null) {
             synchronized (WorkloadIdentityClientFactory.class) {
                if (objectStorageClientInstance == null) {
                    logger.info("WorkloadIdentityClientFactory: Initializing new ObjectStorageClient instance...");

                    String ociRegion = "sa-bogota-1";

                    logger.info("WorkloadIdentityClientFactory: Get Region... "  + ociRegion );

                    if (ociRegion == null || ociRegion.isEmpty()) {
                        throw new IllegalStateException("Environment variable " + ociRegion + " is not set. Cannot determine OCI region.");
                    }

                    AuthenticationDetailsProvider provider = null; 
                    OkeWorkloadIdentityAuthenticationDetailsProvider providerworkloadidentity = null;
                    String sAuthType = System.getenv("OCI_AUTH_PROVIDER");

                    logger.info("WorkloadIdentityClientFactory: Get sAuthType... "  + sAuthType );

                    try {
                        // Check for OKE Workload Identity specific environment variables
                        if (sAuthType.equals("workload_identity")) {
                            logger.info("WorkloadIdentityClientFactory: OKE Workload Identity environment detected. Using Workload Identity provider.");
                            providerworkloadidentity = new OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder().build();
                            logger.info("WorkloadIdentityClientFactory: OkeWorkloadIdentityAuthenticationDetailsProvider initialized.");
                            objectStorageClientInstance = ObjectStorageClient.builder()
                                    .region(Region.fromRegionId(ociRegion))
                                    .build(providerworkloadidentity);
                            
                            logger.info("WorkloadIdentityClientFactory: ObjectStorageClient created successfully for region: {}", ociRegion);
                        } else {
                            logger.info("WorkloadIdentityClientFactory: OKE Workload Identity environment NOT detected. Falling back to ~/.oci/config provider for local testing.");
                            ConfigFileReader.ConfigFile config = ConfigFileReader.parseDefault();
                            provider = new ConfigFileAuthenticationDetailsProvider(config);
                            logger.info("WorkloadIdentityClientFactory: ConfigFileAuthenticationDetailsProvider initialized.");

                            objectStorageClientInstance = ObjectStorageClient.builder()
                                    .region(Region.fromRegionId(ociRegion))
                                    .build(provider);
                            logger.info("WorkloadIdentityClientFactory: ObjectStorageClient created successfully for region: {}", ociRegion);
                        }

                    } catch (IOException e) {
                        logger.error("WorkloadIdentityClientFactory: Error loading OCI config file. Ensure ~/.oci/config is set up correctly: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to load OCI config for local authentication.", e);
                    } catch (Exception e) {
                        logger.error("WorkloadIdentityClientFactory: Error initializing Authentication provider or OCI client: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to initialize OCI ObjectStorageClient.", e);
                    }
                }
            }
        }
        return objectStorageClientInstance;
    }
}