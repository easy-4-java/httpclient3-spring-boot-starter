/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
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
package org.apache.http.spring.boot.client.property;

import java.util.NoSuchElementException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

public class HttpConnectionManagerProperties extends HttpConnectionManagerParams {

	public static final String TIMEOUT_INTERVAL = "http.timeout.interval";
	public static final int  DEFAULT_TIMEOUT_INTERVAL = 5000;
	
	/**
	 * Enumeration of supported connection-manager types.
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
	 */
	public enum ManagerType {

		/** Multi-threaded connection manager that pools connections across threads. */
		MULTI_THREADED("multi-threaded"),
		/** Simple connection manager intended for single-threaded use. */
		SIMPLE("simple");

		private final String type;

		ManagerType(String type) {
			this.type = type;
		}

		/**
		 * Return the string representation of this type.
		 * @return the type name
		 */
		public String get() {
			return type;
		}
		
		/**
		 * Return {@code true} when this type equals the given enum value.
		 * @param type the value to compare with
		 * @return {@code true} if the values are equal
		 */
		public boolean equals(ManagerType type){
			return this.compareTo(type) == 0;
		}
		
		/**
		 * Return {@code true} when this type matches the given string value (case-insensitive).
		 * @param type the string value to compare with
		 * @return {@code true} if the values are equal
		 */
		public boolean equals(String type){
			return this.compareTo(ManagerType.valueOfIgnoreCase(type)) == 0;
		}
		
		/**
		 * Look up a {@link ManagerType} by its string key, ignoring case.
		 * @param key the string key to resolve
		 * @return the matching {@link ManagerType}
		 * @throws NoSuchElementException if no type matches the given key
		 */
		public static ManagerType valueOfIgnoreCase(String key) {
			for (ManagerType type : ManagerType.values()) {
				if(type.get().equalsIgnoreCase(key)) {
					return type;
				}
			}
	    	throw new NoSuchElementException("Cannot found type with key '" + key + "'.");
	    }
		
	}
	
	private ManagerType type = ManagerType.SIMPLE;

    private boolean alwaysClose = false;
    
    /**
	 * Return the interval, in milliseconds, at which idle connections are evicted.
	 * @return the idle-eviction interval
	 */
	public int getTimeoutInterval() {
		return getIntParameter(TIMEOUT_INTERVAL,DEFAULT_TIMEOUT_INTERVAL);
	}
	
	/**
	 * Set the interval, in milliseconds, at which idle connections are evicted.
	 * @param timeoutInterval the idle-eviction interval
	 */
	public void setTimeoutInterval(int timeoutInterval) {
		 setIntParameter(TIMEOUT_INTERVAL,timeoutInterval);
	}
	
	
	/**
	 * Return the type of connection manager to create.
	 * @return the connection-manager type
	 */
	public ManagerType getType() {
		return type;
	}

	/**
	 * Set the type of connection manager to create.
	 * @param type the connection-manager type
	 */
	public void setType(ManagerType type) {
		this.type = type;
	}

	/**
	 * Return whether a {@link SimpleHttpConnectionManager} should always close its underlying
	 * connection.
	 * @return {@code true} to always close the connection
	 */
	public boolean isAlwaysClose() {
		return alwaysClose;
	}


	/**
	 * Set whether a {@link SimpleHttpConnectionManager} should always close its underlying
	 * connection.
	 * @param alwaysClose {@code true} to always close the connection
	 */
	public void setAlwaysClose(boolean alwaysClose) {
		this.alwaysClose = alwaysClose;
	}


	/**
	 * Apply sensible defaults for socket and pool parameters (TCP no-delay, 30s connect
	 * timeout, 60s read timeout, 1MB send/receive buffers, 20 connections per host and 60 total
	 * connections) and return this instance for chaining.
	 * @return this instance with defaults applied
	 */
	public HttpConnectionManagerProperties getInitedParams() {
		
		// setshttpclientwhether使用NoDelay策略;default true
		this.setTcpNoDelay(getBooleanParameter(TCP_NODELAY, true));
		// 通过网络与服务器建立连接的timeout时间。Httpclient包中通过一个asyncthread去creates与服务器的socket连接，这就是该socket连接的timeout时间(单位毫秒)，default30000
		this.setConnectionTimeout(getIntParameter(CONNECTION_TIMEOUT, 30000));
		// 连接读取数据timeout时间(单位毫秒)，default60000 
		this.setSoTimeout(getIntParameter(SO_TIMEOUT, 60000));
		// 每个HOST的maximum连接count 
		this.setDefaultMaxConnectionsPerHost(getIntParameter(MAX_HOST_CONNECTIONS, 20));
		// 连接池的maximum连接数
		this.setMaxTotalConnections(getIntParameter(MAX_TOTAL_CONNECTIONS, 60));
		//socket发送数据的缓冲size ;default ：1M
		this.setSendBufferSize( getIntParameter(SO_SNDBUF, 1024 * 1024));
		//socket接收数据的缓冲size ;default ：1M
		this.setReceiveBufferSize(getIntParameter(SO_RCVBUF, 1024 * 1024));
		//检查连接whether有效的心跳周期 
		this.setTimeoutInterval(getIntParameter(TIMEOUT_INTERVAL, 1000 * 5));
		// 使用系统提供的default的resume策略
		this.setParameter(HttpMethodParams.RETRY_HANDLER,new DefaultHttpMethodRetryHandler());
		
		return this;
		
	}
	

	
	
}
