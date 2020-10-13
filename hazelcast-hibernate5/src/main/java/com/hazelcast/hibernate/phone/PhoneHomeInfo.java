/*
 * Copyright 2020 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.hazelcast.hibernate.phone;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

/**
 * Creates query string according to plugin properties to be sent to phone home
 * server by {@link PhoneHomeService}.
 *
 * Since no dynamic information is sent by phone home calls, the query string is
 * constructed statically here. In case of the addition of a new field to home
 * calls, this class needs to be changed such that it creates appropriate query
 * strings with the additional fields.
 *
 * @since 2.1.2
 */
final class PhoneHomeInfo {

    private static final String PROPERTIES_RESOURCE = "/phone.home.properties";
    private static final String MODULE_NAME = "hazelcast-hibernate5";

    private static String version;
    private static String queryString;

    static {
        version = resolveVersion();
        queryString = buildQueryString();
    }

    private PhoneHomeInfo() {
    }

    public static String getQueryString() {
        return queryString;
    }

    private static String resolveVersion() {
        // To resolve the version described in the pom file, filtering for
        // phone.home.properties must be enabled in the resources section
        // of the pom.xml
        Properties properties = new Properties();
        try (InputStream propertiesStream = PhoneHomeInfo.class.getResourceAsStream(PROPERTIES_RESOURCE)) {
            properties.load(propertiesStream);
            return properties.getProperty("project.version", "N/A");
        } catch (IOException ignored) {
            return "N/A";
        }
    }

    private static String buildQueryString() {
        // Any change committed here must correspond to the phone
        // home server changes. Do not make standalone changes
        // especially for the parameter keys.
        return new QueryStringBuilder()
                .addParam("name", MODULE_NAME)
                .addParam("version", version)
                .build();
    }

    /**
     * Constructs query string using the added parameters.
     */
    private static class QueryStringBuilder {

        private final ILogger logger = Logger.getLogger(QueryStringBuilder.class);
        private final StringBuilder builder;

        private boolean hasParameterBefore;

        QueryStringBuilder() {
            builder = new StringBuilder();
            builder.append("?");
        }

        QueryStringBuilder addParam(String key, String value) {
            if (hasParameterBefore) {
                builder.append("&");
            } else {
                hasParameterBefore = true;
            }
            builder.append(key).append("=").append(tryEncode(value));
            return this;
        }

        private String tryEncode(String value) {
            try {
                return URLEncoder.encode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                if (logger.isFineEnabled()) {
                    logger.fine("Using <unknown> for the value which couldn't be encoded: " + value, e);
                }
                // return the known encoding of the word `unknown` which is unchanged.
                return "unknown";
            }
        }

        String build() {
            return builder.toString();
        }
    }

}