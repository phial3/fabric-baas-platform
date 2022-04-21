package com.anhui.fabricbaascommon.service;

import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MinIOService {
    @Autowired
    private MinioClient minioClient;

    private void createBucketIfNotPresent(String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidResponseException, XmlParserException, InternalException, InvalidKeyException {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * @param bucketName 之前创建的桶名，例如"asiatrip"
     * @param objectName 保存的文件对象的名称，例如"asiaphotos-2015.zip"
     * @param path       文件路径，例如"/home/user/Photos/asiaphotos.zip"
     */
    public void putFile(String bucketName, String objectName, String path) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        createBucketIfNotPresent(bucketName);
        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .filename(path)
                        .build());
    }

    public void putBytes(String bucketName, String objectName, byte[] bytes) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        createBucketIfNotPresent(bucketName);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, inputStream.available(), -1).
                        build()
        );
    }

    /**
     * @param bucketName 之前创建的桶名，例如"asiatrip"
     * @param objectName 保存的文件对象的名称，例如"asiaphotos-2015.zip"
     * @param path       文件路径，例如"/home/user/Photos/asiaphotos.zip"
     */
    public void getAsFile(String bucketName, String objectName, String path) throws InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ServerException {
        byte[] bytes = getAsBytes(bucketName, objectName);
        FileUtils.writeByteArrayToFile(new File(path), bytes);
    }

    public byte[] getAsBytes(String bucketName, String objectName) throws InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ServerException {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {
            return IOUtils.toByteArray(stream);
        }
    }
}
