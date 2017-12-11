package com.hotels.housekeeping.sqs;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

import com.hotels.bdp.housekeeping.api.model.LegacyReplicaPath;
import com.hotels.bdp.housekeeping.api.service.HousekeepingService;

@RunWith(MockitoJUnitRunner.class)
public class SQSHousekeepingServiceTest {

  private @Mock AmazonS3 s3;
  private @Mock AmazonSQS sqs;
  private @Mock ListQueuesResult listQueuesResult;
  private @Mock ReceiveMessageResult receiveMessageResult;
  private @Mock Message message;
  private @Mock ObjectListing objectListing;
  private @Mock LegacyReplicaPath legacyReplicaPath;

  @Test
  public void constructorCreatesHousekeepingQueue() {
    when(sqs.listQueues()).thenReturn(listQueuesResult);
    List<String> queueUrls = new ArrayList<>();
    when(listQueuesResult.getQueueUrls()).thenReturn(queueUrls);
    when(sqs.createQueue(anyString())).thenReturn(new CreateQueueResult());
    HousekeepingService housekeepingService = new SQSHousekeepingService(sqs, s3, "topic");
    verify(sqs, times(1)).createQueue(anyString());
  }

  @Test(expected = RuntimeException.class)
  public void doesntCreateNewHousekeepingQueueWhenQueueNameEmpty() {
    when(sqs.listQueues()).thenReturn(listQueuesResult);
    String queueName = "";
    HousekeepingService housekeepingService = new SQSHousekeepingService(sqs, s3, queueName);
  }

  @Test(expected = RuntimeException.class)
  public void doesntCreateNewHousekeepingQueueWhenQueueNameNull() {
    when(sqs.listQueues()).thenReturn(listQueuesResult);
    String queueName = null;
    HousekeepingService housekeepingService = new SQSHousekeepingService(sqs, s3, queueName);
  }

  @Test
  public void scheduleForHousekeeping() throws Exception {
    when(sqs.sendMessage(any(SendMessageRequest.class))).thenReturn(new SendMessageResult());
    HousekeepingService housekeepingService = new SQSHousekeepingService(sqs, s3, "topic");
    housekeepingService.scheduleForHousekeeping(legacyReplicaPath);
    verify(sqs, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test(expected = RuntimeException.class)
  public void doesntScheduleForHousekeepingQueueWhenQueueNameEmpty() {
    when(sqs.listQueues()).thenReturn(listQueuesResult);
    String queueName = "";
    HousekeepingService housekeepingService = new SQSHousekeepingService(sqs, s3, queueName);
    housekeepingService.scheduleForHousekeeping(legacyReplicaPath);
  }

  @Test(expected = RuntimeException.class)
  public void doesntScheduleForHousekeepingQueueWhenQueueNameNull() {
    when(sqs.listQueues()).thenReturn(listQueuesResult);
    String queueName = null;
    HousekeepingService housekeepingService = new SQSHousekeepingService(sqs, s3, queueName);
    housekeepingService.scheduleForHousekeeping(legacyReplicaPath);
  }

  @Test
  public void housekeepEmptyDirectoryObject() throws Exception {
    when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveMessageResult);
    List<Message> receivedMessages = new ArrayList<>();
    receivedMessages.add(message);
    when(receiveMessageResult.getMessages()).thenReturn(receivedMessages).thenReturn(Collections.<Message> emptyList());
    when(message.getBody()).thenReturn("s3://bucket/key");
    Map<String, String> attributes = new HashMap<>();
    attributes.put("SentTimestamp", "1234");
    when(message.getAttributes()).thenReturn(attributes);

    when(s3.listObjects(anyString(), anyString())).thenReturn(new ObjectListing());
    doNothing().when(s3).deleteObject(any(DeleteObjectRequest.class));
    HousekeepingService housekeepingService = new SQSHousekeepingService(sqs, s3, "topic");
    housekeepingService.cleanUp(new Instant().minus(Duration.standardDays(3)));
    verify(sqs, times(2)).receiveMessage(any(ReceiveMessageRequest.class));
    verify(receiveMessageResult, times(2)).getMessages();
    verify(message, times(2)).getBody();
    verify(message, times(1)).getAttributes();
    verify(s3, times(1)).listObjects(anyString(), anyString());
    verify(s3, times(1)).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  public void housekeepDirectoryObjectWithContents() throws Exception {
    when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveMessageResult);
    List<Message> receivedMessages = new ArrayList<>();
    receivedMessages.add(message);
    when(receiveMessageResult.getMessages()).thenReturn(receivedMessages).thenReturn(Collections.<Message> emptyList());
    when(message.getBody()).thenReturn("s3://bucket/key");
    Map<String, String> attributes = new HashMap<>();
    attributes.put("SentTimestamp", "1234");
    when(message.getAttributes()).thenReturn(attributes);
    when(s3.listObjects(anyString(), anyString())).thenReturn(objectListing);
    List<S3ObjectSummary> s3ObjectSummaries = new ArrayList<>();
    s3ObjectSummaries.add(new S3ObjectSummary());
    s3ObjectSummaries.add(new S3ObjectSummary());
    when(objectListing.getObjectSummaries()).thenReturn(s3ObjectSummaries);
    doNothing().when(s3).deleteObject(any(DeleteObjectRequest.class));
    doNothing().when(s3).deleteObject(anyString(), anyString());
    HousekeepingService housekeepingService = new SQSHousekeepingService(sqs, s3, "topic");
    housekeepingService.cleanUp(new Instant().minus(Duration.standardDays(3)));
    verify(sqs, times(2)).receiveMessage(any(ReceiveMessageRequest.class));
    verify(receiveMessageResult, times(2)).getMessages();
    verify(message, times(2)).getBody();
    verify(message, times(1)).getAttributes();
    verify(s3, times(1)).listObjects(anyString(), anyString());
    verify(s3, times(1)).deleteObject(any(DeleteObjectRequest.class));
    verify(s3, times(2)).deleteObject(anyString(), anyString());
  }

  @Test(expected = RuntimeException.class)
  public void doesntCleanUpWhenQueueNameNull() {
    when(sqs.listQueues()).thenReturn(listQueuesResult);
    String queueName = null;
    HousekeepingService housekeepingService = new SQSHousekeepingService(sqs, s3, queueName);
    housekeepingService.cleanUp(Instant.now());
  }

  @Test(expected = RuntimeException.class)
  public void doesntCleanUpWhenQueueNameEmpty() {
    when(sqs.listQueues()).thenReturn(listQueuesResult);
    String queueName = "";
    HousekeepingService housekeepingService = new SQSHousekeepingService(sqs, s3, queueName);
    housekeepingService.cleanUp(Instant.now());
  }
}