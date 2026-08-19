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
package org.jmxtrans.embedded.util.io;

import java.io.Closeable;
import java.io.InputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class IoUtils {
	
    protected final static Logger logger = LoggerFactory.getLogger(IoUtils.class.getName());
    /**
     * <p>Close quietly.</p>
     * @param cnn the cnn
     * @return the static void
     */

    public static void closeQuietly (URLConnection cnn) {
        if (cnn == null) {
            return;
        } else if (cnn instanceof HttpURLConnection) {
            ((HttpURLConnection) cnn).disconnect();
        } else {
            // do nothing
        }
    }
    /**
     * <p>Close quietly.</p>
     * @param closeable the closeable
     * @return the static void
     */
    
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null)
            return;
        try {
            closeable.close();
        } catch (Exception e) {
            // ignore silently
        }
    }
    /**
     * <p>Close quietly.</p>
     * @param writer the writer
     * @return the static void
     */

    public static void closeQuietly(Writer writer) {
        if (writer == null)
            return;
        try {
            writer.close();
        } catch (Exception e) {
            // ignore silently
        }
    }

    /**
     * Needed for old JVMs where {@link java.io.InputStream} does not implement {@link java.io.Closeable}.
     */
    public static void closeQuietly(InputStream inputStream) {
        if (inputStream == null)
            return;
        try {
            inputStream.close();
        } catch (Exception e) {
            // ignore silently
        }
    }
      
}
