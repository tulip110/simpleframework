/*
 * SessionBuilder.java February 2014
 *
 * Copyright (C) 2014, Niall Gallagher <niallg@users.sf.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License.
 */

package org.simpleframework.http.socket.service;

import java.io.IOException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.socket.Session;
import org.simpleframework.transport.Channel;
import org.simpleframework.transport.reactor.Reactor;

/**
 * The <code>SessionBuilder</code> object is used to create sessions
 * for connected WebSockets. Before the session is created a response
 * is sent back to the connected client. If for some reason the session
 * is not valid or does not conform to the requirements of RFC 6455
 * then a HTTP 400 response code is sent and the TCP channel is closed.
 * 
 * @author Niall Gallagher
 */
class SessionBuilder {

   /**
    * This is the response associated with the WebSocket session.
    */
   private final Response response;
   
   /**
    * This is the request associated with the WebSocket session.
    */
   private final Request request;
   
   /**
    * This is the reactor used to notify of read events.
    */
   private final Reactor reactor;
   
   /**
    * This is the underlying TCP channel associated with the session.
    */
   private final Channel channel;
   
   /**
    * Constructor for the <code>SessionBuilder</code> object. This is
    * used to create sessions using the request and response associated
    * with the WebSocket opening handshake. 
    * 
    * @param request the request involved in initiating the session
    * @param response the response involved in initiating the session
    * @param reactor the reactor used to notify of read events
    */
   public SessionBuilder(Request request, Response response, Reactor reactor) {
      this.channel = request.getChannel();
      this.response = response;
      this.request = request;
      this.reactor = reactor;
   }
   
   /**
    * This is used to create a WebSocket session. If at any point there 
    * is an error creating the session the underlying TCP connection is
    * closed and a <code>Session</code> is returned regardless. 
    * 
    * @return this returns the session associated with the WebSocket
    */
   public Session create() throws Exception {
      FrameChannel operation = new FrameChannel(request, response, channel, reactor);
      ResponseBuilder responder = new ResponseBuilder(request, response, channel);

      try {
         responder.commit();
      } catch(Exception e) {
         throw new IOException("Could not send response", e);
      }
      return operation.open();
   }
}
