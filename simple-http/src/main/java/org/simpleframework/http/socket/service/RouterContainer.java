/*
 * RouterContainer.java February 2014
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
import org.simpleframework.http.core.Container;

/**
 * The <code>RouterContainer</code> is used to route requests that
 * satisfy a WebSocket opening handshake to a specific service. Each
 * request intercepted by this <code>Container</code> implementation 
 * is examined for opening handshake criteria as specified by RFC 6455,
 * and if it contains the required information it is router to a
 * specific service using a <code>Router</code> implementation. If the
 * request does not contain the required criteria it is handled by
 * an internal container delegate. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.socket.service.Router
 */
public class RouterContainer implements Container {
   
   /**
    * This is the service dispatcher used to dispatch requests.
    */
   private final ServiceDispatcher dispatcher;
   
   /**
    * This is the container used to handle traditional requests.
    */
   private final Container container;
   
   /**
    * This is the router used to select specific services.
    */
   private final Router router;

   /**
    * Constructor for the <code>RouterContainer</code> object. This
    * requires a container to delegate traditional requests to and
    * a <code>Router</code> implementation which can be used to 
    * select a service to dispatch a WebSocket session to.
    * 
    * @param container this is the container to delegate to
    * @param router this is the router used to select services
    * @param threads this contains the number of threads to use
    */
   public RouterContainer(Container container, Router router, int threads) throws IOException {
      this(container, router, threads, 5000);
   }
   
   /**
    * Constructor for the <code>RouterContainer</code> object. This
    * requires a container to delegate traditional requests to and
    * a <code>Router</code> implementation which can be used to 
    * select a service to dispatch a WebSocket session to.
    * 
    * @param container this is the container to delegate to
    * @param router this is the router used to select services
    * @param threads this contains the number of threads to use
    * @param ping this is the frequency to send ping frames with
    */
   public RouterContainer(Container container, Router router, int threads, long ping) throws IOException {
      this(container, router, threads, ping, 20000);
   }
   
   /**
    * Constructor for the <code>RouterContainer</code> object. This
    * requires a container to delegate traditional requests to and
    * a <code>Router</code> implementation which can be used to 
    * select a service to dispatch a WebSocket session to.
    * 
    * @param container this is the container to delegate to
    * @param router this is the router used to select services
    * @param threads this contains the number of threads to use
    * @param ping this is the frequency to send ping frames with
    * @param expiry the length of time a connection can be idle for
    */
   public RouterContainer(Container container, Router router, int threads, long ping, long expiry) throws IOException {
      this.dispatcher = new ServiceDispatcher(router, threads, ping, expiry);
      this.container = container;
      this.router = router;
   }

   /**
    * This method is used to create a dispatch a <code>Session</code> to
    * a specific service selected by a router. If the session initiating
    * handshake fails for any reason this will close the underlying TCP
    * connection and send a HTTP 400 response back to the client. All
    * traditional requests that do not represent an WebSocket opening
    * handshake are dispatched to the internal container. 
    * 
    * @param req the request that contains the client HTTP message
    * @param resp the response used to deliver the server response
    */
   public void handle(Request req, Response resp) {
      Service service = router.route(req, resp);
      
      if(service != null) {
         dispatcher.dispatch(req, resp);
      } else {
         container.handle(req, resp);
      }
   }   
   
   /**
    * This is used to initiating session management by pinging all
    * connected WebSocket channels. If after a specific number of 
    * pings the WebSocket does not respond then the WebSocket is
    * closed using a control frame.
    */
   public void start() {
      dispatcher.start();
   }
   
   /**
    * This is used to stop session management. Stopping the session
    * manger means connected WebSocket channels will not receive
    * any ping messages, they will however still receive pong frames
    * if a ping is sent to it. Session management can be started 
    * and stopped at will.
    */
   public void stop() {
      dispatcher.stop();
   }
}
