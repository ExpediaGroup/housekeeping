package com.hotels.housekeeping.sqs;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import com.hotels.bdp.housekeeping.api.model.LegacyReplicaPath;
import com.hotels.bdp.housekeeping.api.service.HousekeepingService;

public class SQSHousekeepingService implements HousekeepingService {

  private static final Logger LOG = LoggerFactory.getLogger(SQSHousekeepingService.class);
  private static final String REFERENCE_TIME_KEY = "SentTimestamp";

  private final AmazonSQS sqs;
  private final AmazonS3 s3;
  private final String id;

  public SQSHousekeepingService(AmazonSQS sqs, AmazonS3 s3, String id) {
    if (Strings.isNullOrEmpty(id)) {
      throw new RuntimeException("Id not specified, cannot start service");
    }
    try {
      sqs.createQueue(id);
    } catch (Exception e) {
      throw new RuntimeException("Could not create service with id: '" + id + "'", e);
    }
    this.sqs = sqs;
    this.s3 = s3;
    this.id = id;
  }

  @Override
  public void cleanUp(Instant referenceTime) {
    while (cleanUpHelper(referenceTime)) { ; }
  }

  private boolean cleanUpHelper(Instant referenceTime) {
    List<Message> cleanUpPaths = new ArrayList<>();
    Set<Message> messages = new HashSet<>();

    ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest();
    receiveMessageRequest.setQueueUrl(id);
    receiveMessageRequest.setMessageAttributeNames(Lists.newArrayList("All"));
    receiveMessageRequest.setAttributeNames(Lists.newArrayList("All"));
    receiveMessageRequest.setWaitTimeSeconds(20);
    receiveMessageRequest.setMaxNumberOfMessages(10);
    receiveMessageRequest.setVisibilityTimeout(300);

    try {
      List<Message> received = sqs.receiveMessage(receiveMessageRequest).getMessages();
      if (received.size() == 0) {
        return false;
      }
      messages.addAll(received);
    } catch (RuntimeException e) {
      throw new RuntimeException("Could not load legacy paths from service " + id, e);
    }

    for (Message message : messages) {
      Long messageSentTimestamp = Long.valueOf(message.getAttributes().get(REFERENCE_TIME_KEY));
      Instant messageSentTime = new Instant(messageSentTimestamp);
      if (messageSentTime.isBefore(referenceTime)) {
        cleanUpPaths.add(message);
        LOG.debug("Loaded '{}' for housekeeping", message.getBody());
      }
    }

    try {
      for (Message cleanMe : cleanUpPaths) {
        String cleanUpPath = cleanMe.getBody();
        AmazonS3URI uri = new AmazonS3URI(cleanUpPath);
        LOG.info("Deleting path '{}' from file system", uri);
        try {
          String bucket = uri.getBucket();
          String key = uri.getKey();
          delete(bucket, key);
          LOG.info("Path '{}' has been deleted from file system", uri);
        } catch (Exception e) {
          LOG.warn("Unable to delete path '{}' from file system. Will try next time", uri, e);
          continue;
        }
        sqs.deleteMessage(id, cleanMe.getReceiptHandle());
        LOG.debug("Message '{}' has been deleted", cleanMe.toString());
      }
    } catch (Exception e) {
      throw new RuntimeException(format("Unable to execute housekeeping at instant {}", referenceTime), e);
    }
    return true;
  }

  @Override
  public void scheduleForHousekeeping(LegacyReplicaPath cleanUpPath) {
    try {
      SendMessageRequest sendMessageRequest = new SendMessageRequest();
      sendMessageRequest.setQueueUrl(id);
      sendMessageRequest.setMessageBody(cleanUpPath.getPath());
      sqs.sendMessage(sendMessageRequest);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to schedule '" + cleanUpPath + "' for housekeeping on service '" + id + "'", e);
    }
  }

  private void delete(String bucket, String key) {
    for (S3ObjectSummary object : s3.listObjects(bucket, key).getObjectSummaries()) {
      LOG.debug("Deleting object {}", object.getBucketName() + object.getKey());
      s3.deleteObject(bucket, object.getKey());
    }
    s3.deleteObject(new DeleteObjectRequest(bucket, key));
  }
}
