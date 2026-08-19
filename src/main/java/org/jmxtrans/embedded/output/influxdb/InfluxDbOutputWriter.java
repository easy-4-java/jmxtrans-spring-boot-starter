/*
 * Copyright (c) 2010-2016 the original author or authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.jmxtrans.embedded.output.influxdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmxtrans.embedded.ExtendedResultNameStrategy;
import org.jmxtrans.embedded.QueryResult;
import org.jmxtrans.embedded.output.AbstractOutputWriter;
import org.jmxtrans.embedded.output.OutputWriter;
import org.jmxtrans.embedded.util.io.IoRuntimeException;
import org.jmxtrans.embedded.util.io.IoUtils;
import org.jmxtrans.embedded.util.io.IoUtils2;
import org.jmxtrans.embedded.util.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output writer for InfluxDb.
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class InfluxDbOutputWriter extends AbstractOutputWriter implements OutputWriter {

	public final static String SETTING_ENABLED = "enabled";
	private final AtomicInteger exceptionCounter = new AtomicInteger();
	private final Logger LOG = LoggerFactory.getLogger(InfluxDbOutputWriter.class);
    private URL url;
    private String database;
    private String user; // Null if not configured
    private String password; // Null if not configured 
    private String retentionPolicy; // Null if not configured
    private List<InfluxTag> tags;
    private List<InfluxMetric> batchedMetrics = new ArrayList<InfluxMetric>();
    private int connectTimeoutMillis;
    private int readTimeoutMillis;
    private int retryTimes;
    private boolean enabled;
    /**
	 * Optional proxy for the http API calls
	 */
	private Proxy proxy;
	
    public InfluxDbOutputWriter() {
    	
    }

    /**
	 * Initial setup for the writer class. Loads in settings and initializes one-time setup variables like instanceId.
	 */
	@Override
	public void start() {
		
        enabled = getBooleanSetting(SETTING_ENABLED, true);
    	
        if(!enabled) return;

        //从新定义ResultNameStrategy
        setStrategy(new ExtendedResultNameStrategy());
    	
        String urlStr = getUrl(getStringSetting("url"));
        database = getStrategy().resolveExpression(getDatabase(getStringSetting("database")));
        user = getUser(getStringSetting("user"));
        password = getPassword(getStringSetting("password"));
        retentionPolicy = getStringSetting("retentionPolicy", null);
        String tagsStr = getStringSetting("tags", "");
        
        tags = InfluxMetricConverter.tagsFromCommaSeparatedString(this.getStrategy(),tagsStr);
        connectTimeoutMillis = getIntSetting("connectTimeoutMillis", 3000);
        readTimeoutMillis = getIntSetting("readTimeoutMillis", 5000);
        retryTimes = getIntSetting("retryTimes", 10);
        
        url = parseUrlStr(getWriteEndpointForUrlStr(urlStr));
        
        if (getStringSetting(SETTING_PROXY_HOST, null) != null && !getStringSetting(SETTING_PROXY_HOST).isEmpty()) {
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getStringSetting(SETTING_PROXY_HOST), getIntSetting(SETTING_PROXY_PORT)));
		}
		
        if(LOG.isInfoEnabled()){
        
			LOG.info("Starting Stackdriver writer connected to '{}', proxy {} ...", url, proxy);
	        LOG.info( "InfluxDbOutputWriter is configured with url=" + urlStr
	                + ", database=" + database
	                + ", user=" + user
	                + ", password=" + (password != null ? "****" : null)
	                + ", tags=" + tagsStr
	                + ", connectTimeoutMills=" + connectTimeoutMillis
	                + ", readTimeoutMillis=" + readTimeoutMillis);
        }
    }
    /** Gets the write endpoint for url str. */

    private String getWriteEndpointForUrlStr(String urlStr) {
        return urlStr + (urlStr.endsWith("/") ? "write" : "/write");
    }
    /**
     * <p>Parse url str.</p>
     * @param urlStr the url str
     * @return the u r l
     */

    private URL parseUrlStr(String urlStr) {
        try {
            return new URL(urlStr + "?" + buildQueryString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * <p>Build query string.</p>
     * @return the string
     */

    private String buildQueryString() {
        StringBuilder sb = new StringBuilder();
        sb.append("precision=ms").append("&db=").append(database);
        appendParamIfNotEmptyOrNull(sb, "u", user);
        appendParamIfNotEmptyOrNull(sb, "p", password);
        appendParamIfNotEmptyOrNull(sb, "rp", retentionPolicy);
        return sb.toString();
    }
	/**
	 * <p>Write.</p>
	 * @param results the results
	 */
    
	@Override
	public void write(Iterable<QueryResult> results) {
		
		try {
			if(!enabled) return;
			if(LOG.isDebugEnabled()){
				LOG.debug("Export to '{}', proxy {} metrics {}", url, proxy, results);
			}
			if( exceptionCounter.get() > retryTimes){
				return;
			}
			for (QueryResult result : results) {
		        if(LOG.isDebugEnabled()){
		        	String msg = result.getName() + " " + result.getValue() + " " + result.getEpoch(TimeUnit.SECONDS);
		        	LOG.debug(msg);
		        }
		        String metricName = result.getName();
		        Object value = result.getValue();
		        
		        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(this.getStrategy(), metricName, value, tags, SystemClock.now());
		        batchedMetrics.add(metric);
		        
			}
			
	        String body = convertMetricsToLines(batchedMetrics);
	        String queryString = buildQueryString();
	        if(LOG.isDebugEnabled()){
	        	LOG.debug( "Sending to influx (" + url + "):\n" + body);
	        }
	        batchedMetrics.clear();
	        
	        sendMetrics(queryString, body);
			 
		} catch (Exception e) {
			exceptionCounter.incrementAndGet();
			if(LOG.isWarnEnabled()){
				LOG.warn("Failure to send result to InfluxDb '{}' with proxy {}", url, proxy, e);
			}
		}
	}
    /**
     * <p>Send metrics.</p>
     * @param queryString the query string
     * @param body the body
     * @throws IOException if an error occurs
     */

    private void sendMetrics(String queryString, String body) throws IOException {
        HttpURLConnection conn = createAndConfigureConnection();
        try {
            sendMetrics(body, conn);
        } finally {
            IoUtils.closeQuietly(conn);
        }
    }
    /**
     * <p>Send metrics.</p>
     * @param body the body
     * @param urlConnection the url connection
     * @throws IOException if an error occurs
     */

    private void sendMetrics(String body, HttpURLConnection urlConnection) throws IOException {
        writeMetrics(urlConnection, body);
        int responseCode = urlConnection.getResponseCode();
        if (responseCode / 100 != 2) {
        	exceptionCounter.incrementAndGet();
            throw new RuntimeException("Failed to write metrics, response code: " + responseCode  + ", response message: " + urlConnection.getResponseMessage());
        }
        String response = readResponse(urlConnection);
        if(LOG.isDebugEnabled()){
        	LOG.debug("Response from influx: " + response);
        }
    }
    /**
     * <p>Create and configure connection.</p>
     * @return the http u r l connection
     * @throws ProtocolException if an error occurs
     */

    private HttpURLConnection createAndConfigureConnection() throws ProtocolException {
        HttpURLConnection conn = openHttpConnection();
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        return conn;
    }
    /**
     * <p>Open http connection.</p>
     * @return the http u r l connection
     */
    
    private HttpURLConnection openHttpConnection() {
        try {
    		HttpURLConnection urlConnection = null;
        	if (proxy == null) {
    			urlConnection = (HttpURLConnection) url.openConnection();
    		} else {
    			urlConnection = (HttpURLConnection) url.openConnection(proxy);
    		}
            return urlConnection;
        } catch (Exception e) {
            throw new IoRuntimeException("Failed to create HttpURLConnection to " + url + " - is it a valid HTTP url?",  e);
        }
    }
    /**
     * <p>Write metrics.</p>
     * @param conn the conn
     * @param body the body
     */

    private void writeMetrics(HttpURLConnection conn, String body)
            throws UnsupportedEncodingException, IOException {
        byte[] toSendBytes = body.getBytes("UTF-8");
        conn.setRequestProperty("Content-Length", Integer.toString(toSendBytes.length));
        OutputStream os = null;
        try {
        	os = conn.getOutputStream();
			os.write(toSendBytes);
            os.flush();
		} finally {
			if(os != null){
				IoUtils2.closeQuietly(os);
			}
		}
    }
    /**
     * <p>Read response.</p>
     * @param conn the conn
     * @return the string
     * @throws IOException if an error occurs
     * @throws UnsupportedEncodingException if an error occurs
     */

    private String readResponse(HttpURLConnection conn) throws IOException, UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
        	is = conn.getInputStream();
        	IoUtils2.copy(is, baos);
		} finally {
			if(is != null){
				IoUtils2.closeQuietly(is);
			}
		}
        String response = new String(baos.toByteArray(), "UTF-8");
        return response;
    }
    /**
     * <p>Append param if not empty or null.</p>
     * @param sb the sb
     * @param paramName the param name
     * @param paramValue the param value
     */

    private void appendParamIfNotEmptyOrNull(StringBuilder sb, String paramName, String paramValue) {
        if (paramValue != null && !paramValue.trim().isEmpty()) {
            // NB: We do not URL encode anything, from what I understand from the Influx docs,
            // encoded data is not expected.
            sb.append("&").append(paramName).append("=").append(paramValue);
        }

    }
    /**
     * <p>Convert metrics to lines.</p>
     * @param metrics the metrics
     * @return the string
     */

    private String convertMetricsToLines(List<InfluxMetric> metrics) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<InfluxMetric> it = metrics.iterator(); it.hasNext();) {
            InfluxMetric metric = it.next();
            sb.append(metric.toInfluxFormat());
            if (it.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
	/** Gets the url. */
 
	public String getUrl(String url) {
		return url== null ? "http://127.0.0.1:8086" : url;
	}
	/** Gets the database. */

	public String getDatabase(String database) {
		return database== null ? "Metrics_127.0.0.1" : database;
	}
	/** Gets the user. */

	public String getUser(String user) {
		return user;
	}
	/** Gets the password. */

	public String getPassword(String password) {
		return password;
	}

}
