/*
 * Copyright (C) 2014 Gazihan Alankus.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.rosjava.ekorob_firebase_java_pkg.ekorob_firebase_pubsub;
//package com.github.ekorob_firebase_java_pkg.ekorob_firebase_pubsub;

import com.github.ekorob_firebase_java_pkg.ekorob_firebase_pubsub.ValueBuffer;
import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.auth.oauth2.GoogleCredentials;
import org.ros.node.topic.Subscriber;

/**
 * A simple {@link Publisher} {@link NodeMain}.
 */
public class Talker extends AbstractNodeMain {

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("rosjava/talker");
  }

  @Override
  public void onStart(final ConnectedNode connectedNode) {
  
    try {
      FileInputStream serviceAccount =
      new FileInputStream("/home/gazihan/ros/kinetic/myjava/src/ekorob_firebase_java_pkg/ekorob_firebase_pubsub/googlekey/robotic-speech-firebase-adminsdk-95hnc-afce420e7e.json");

      FirebaseOptions options = new FirebaseOptions.Builder()
      .setCredentials(GoogleCredentials.fromStream(serviceAccount))
      .setDatabaseUrl("https://robotic-speech.firebaseio.com")
      .build();

      FirebaseApp.initializeApp(options);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }


    System.out.println("LISTENING LISTENING");

    //souffle=sufle yok, waitlocation=yes, whoiam=kullanıcı, location=location2, facedetected=no

    startTopic(connectedNode, "souffle");
    startTopic(connectedNode, "waitlocation");
    startTopic(connectedNode, "whoiam");
    startTopic(connectedNode, "location");
    startTopic(connectedNode, "facedetected");

    final Publisher<std_msgs.String> publisher =
        connectedNode.newPublisher("ekorob_firebase", std_msgs.String._TYPE);
    // This CancellableLoop will be canceled automatically when the node shuts
    // down.
    connectedNode.executeCancellableLoop(new CancellableLoop() {
      private int sequenceNumber;

      @Override
      protected void setup() {
        sequenceNumber = 0;
      }

      @Override
      protected void loop() throws InterruptedException {
        std_msgs.String str = publisher.newMessage();
        str.setData("Hello world! " + sequenceNumber);
        publisher.publish(str);
        sequenceNumber++;
        Thread.sleep(1000);
      }
    });
  }

  private void startTopic(ConnectedNode connectedNode, final String name) {
    final ValueBuffer buffer = new ValueBuffer();
    buffer.value = "";

    java.lang.String topic = "/firebase/" + name;
    final Publisher<std_msgs.String> pub =
            connectedNode.newPublisher(topic, std_msgs.String._TYPE);
    final DatabaseReference ref = FirebaseDatabase.getInstance()
        .getReference(name);
    ref.addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        Object document = dataSnapshot.getValue();
        String docStr = document.toString();
        // publish this
        System.out.println(name + " from firebase:");
        System.out.println(docStr);

        if (docStr.equals(buffer.value)) {
          System.out.println("the same, ignoring");
        } else {
          System.out.println("different, sending");
          std_msgs.String str = pub.newMessage();
          str.setData(docStr);
          pub.publish(str);
          buffer.value = docStr;
        }

      }

      @Override
      public void onCancelled(DatabaseError error) {
      }
    });


    Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber(topic, std_msgs.String._TYPE);
    subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
      @Override
      public void onNewMessage(std_msgs.String message) {
        java.lang.String msgData = message.getData();

        System.out.println(name + " to Firebase:");
        System.out.println(msgData);

        buffer.value = msgData;

        ref.setValueAsync(msgData);
      }
    });


  }
}
