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
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class WorkloadIdentityDemoApp {

    private static final Logger logger = LoggerFactory.getLogger(WorkloadIdentityDemoApp.class);

    public static void main(String[] args) {
        logger.info("WorkloadIdentityDemoApp: Starting OKE Workload Identity demonstration.");

        String objectStorageNamespace = System.getenv("OCI_OBJECT_STORAGE_NAMESPACE");
        String bucketName = System.getenv("OCI_BUCKET_NAME");

        if (objectStorageNamespace == null || objectStorageNamespace.isEmpty() ||
            bucketName == null || bucketName.isEmpty()) {
            logger.error("Error: Environment variables OCI_OBJECT_STORAGE_NAMESPACE and OCI_BUCKET_NAME must be set.");
            logger.error("Please provide your Object Storage namespace and the target bucket name.");
            System.exit(1);
        }

        try {
            // Get the ObjectStorageClient using the singleton factory
            ObjectStorageClient osClient = WorkloadIdentityClientFactory.getObjectStorageClient();

            logger.info("WorkloadIdentityDemoApp: Attempting to list objects in bucket '{}' (namespace '{}') using Workload Identity...",
                                            bucketName, objectStorageNamespace);

            ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                                                        .namespaceName(objectStorageNamespace)
                                                        .bucketName(bucketName)
                                                        .build();

            // Perform the API call
            ListObjectsResponse listObjectsResponse = osClient.listObjects(listObjectsRequest);

            if (listObjectsResponse.getListObjects() != null &&
                listObjectsResponse.getListObjects().getObjects() != null &&
                !listObjectsResponse.getListObjects().getObjects().isEmpty()) {
                logger.info("WorkloadIdentityDemoApp: Successfully listed objects in bucket '{}':", bucketName);
                for (ObjectSummary obj : listObjectsResponse.getListObjects().getObjects()) {
                    logger.info("- {} (Size: {} bytes)", obj.getName(), obj.getSize());
                }
            } else {
                logger.info("WorkloadIdentityDemoApp: Bucket '{}' is empty or no objects found.", bucketName);
            }

        } catch (Exception e) {
            logger.error("WorkloadIdentityDemoApp: An error occurred during OCI Object Storage access: {}", e.getMessage(), e);
            logger.error("Please verify your OCI IAM policy, OKE cluster OCID, Kubernetes Service Account, Namespace, Object Storage Namespace, Bucket Name, and OCI Region.");
            System.exit(1);
        } finally {
            // It's good practice to close the client when done.
            // WorkloadIdentityClientFactory.closeClient();
            logger.info("WorkloadIdentityDemoApp: Client closed.");
        }

        logger.info("WorkloadIdentityDemoApp: Demo finished. Keeping pod alive for inspection...");
        // Keep the pod running for inspection in OKE logs (important for K8s deployments)
        try {
            TimeUnit.HOURS.sleep(1); // Keep the pod running for 1 hour
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("WorkloadIdentityDemoApp: Pod interrupted.");
        }
    }
}