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

import java.util.Objects;

/**
 * @author Kristoffer Erlandsson
 * @since 1.0.0
 */
public class InfluxTag {
    private final String name;
    private final String value;

    public InfluxTag(String name, String value) {
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
    }
    /** Gets the name. */

    public String getName() {
        return name;
    }
    /** Gets the value. */

    public String getValue() {
        return value;
    }
    /**
     * <p>To influx format.</p>
     * @return the string
     */

    public String toInfluxFormat() {
        return name + "=" + value;
    }
    /**
     * <p>To string.</p>
     * @return the string
     */

    @Override
    public String toString() {
        return name + "=" + value;
    }
    /**
     * <p>Hash code.</p>
     * @return the int
     */

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
    /**
     * <p>Equals.</p>
     * @param obj the obj
     * @return the boolean
     */

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InfluxTag other = (InfluxTag) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(value, other.value);
    }

}