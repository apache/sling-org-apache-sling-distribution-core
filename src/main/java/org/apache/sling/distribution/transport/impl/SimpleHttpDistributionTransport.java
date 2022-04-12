/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.transport.impl;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.common.RecoverableDistributionException;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.AbstractDistributionPackage;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.transport.DistributionTransportSecret;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.util.RequestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * default HTTP implementation of {@link DistributionTransport}
 */
public class SimpleHttpDistributionTransport implements DistributionTransport {

    /**
     * The key name of an entry holding a username associated to the #PASSWORD in
     * the {@link DistributionTransportSecret}'s credentials map.
     */
    private static final String USERNAME = "username";

    /**
     * The key name of an entry holding a password associated to the #USERNAME in
     * the {@link DistributionTransportSecret}'s credentials map.
     */
    private static final String PASSWORD = "password";

    /**
     * The key name of an entry holding an Authorization header in
     * the {@link DistributionTransportSecret}'s credentials map.
     */
    private static final String AUTHORIZATION = "authorization";

    private static final String EXECUTOR_CONTEXT_KEY_PREFIX = "ExecutorContextKey";

    /**
     * The <code>Digest</code> header, see <a href="https://tools.ietf.org/html/rfc3230#section-4.3.2">section-4.3.2</a>
     * of Instance Digests in HTTP (RFC3230)
     */
    private static final String DIGEST_HEADER = "Digest";

    /**
     * distribution package origin uri
     */
    private static final String PACKAGE_INFO_PROPERTY_ORIGIN_URI = "internal.origin.uri";

    private final DefaultDistributionLog log;
    private final DistributionEndpoint distributionEndpoint;
    private final DistributionPackageBuilder packageBuilder;
    private final DistributionTransportSecretProvider secretProvider;
    private final HttpConfiguration httpConfiguration;
    private final String contextKeyExecutor;

    public SimpleHttpDistributionTransport(DefaultDistributionLog log, DistributionEndpoint distributionEndpoint,
                                           DistributionPackageBuilder packageBuilder,
                                           DistributionTransportSecretProvider secretProvider,
                                           HttpConfiguration httpConfiguration) {
        this.log = log;

        this.distributionEndpoint = distributionEndpoint;
        this.packageBuilder = packageBuilder;
        this.secretProvider = secretProvider;
        this.httpConfiguration = httpConfiguration;
        this.contextKeyExecutor = EXECUTOR_CONTEXT_KEY_PREFIX + "_" + getHostAndPort(distributionEndpoint.getUri()) + "_" + UUID.randomUUID();
    }

    public void deliverPackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionPackage distributionPackage,
                               @NotNull DistributionTransportContext distributionContext) throws DistributionException {
        String hostAndPort = getHostAndPort(distributionEndpoint.getUri());

        DistributionPackageInfo info = distributionPackage.getInfo();
        URI packageOrigin = info.get(PACKAGE_INFO_PROPERTY_ORIGIN_URI, URI.class);

        if (packageOrigin != null && hostAndPort.equals(getHostAndPort(packageOrigin))) {
            log.debug("skipping distribution of package {} to same origin {}", distributionPackage.getId(), hostAndPort);
        } else {

            try {
                Executor executor = getExecutor(distributionContext);

                Request req = Request.Post(distributionEndpoint.getUri())
                        .connectTimeout(httpConfiguration.getConnectTimeout())
                        .socketTimeout(httpConfiguration.getSocketTimeout())
                        .addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE)
                        .useExpectContinue();

                String authorizationHeader = getAuthSecret();
                if (null != authorizationHeader) {
                    req.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, authorizationHeader));
                }

                // add the message body digest, see https://tools.ietf.org/html/rfc3230#section-4.3.2
                if (distributionPackage instanceof AbstractDistributionPackage) {
                    AbstractDistributionPackage adb = (AbstractDistributionPackage) distributionPackage;
                    if (adb.getDigestAlgorithm() != null && adb.getDigestMessage() != null) {
                        req.addHeader(DIGEST_HEADER, String.format("%s=%s", adb.getDigestAlgorithm(), adb.getDigestMessage()));
                    }
                }

                InputStream inputStream = null;
                try {
                    inputStream = DistributionPackageUtils.createStreamWithHeader(distributionPackage);

                    req.bodyStream(inputStream, ContentType.APPLICATION_OCTET_STREAM);

                    Response response = executor.execute(req);
                    response.returnContent(); // throws an error if HTTP status is >= 300

                } finally {
                    IOUtils.closeQuietly(inputStream);
                }

                log.debug("delivered packageId={}, endpoint={}", distributionPackage.getId(), distributionEndpoint.getUri());
            } catch (HttpHostConnectException e) {
                throw new RecoverableDistributionException("endpoint not available " + distributionEndpoint.getUri(), e);
            } catch (HttpResponseException e) {
                int statusCode = e.getStatusCode();
                if (statusCode == 404 || statusCode == 401) {
                    throw new RecoverableDistributionException("not enough rights for " + distributionEndpoint.getUri(), e);
                }
                throw new DistributionException(e);
            } catch (Exception e) {
                throw new DistributionException(e);

            }
        }
    }

    @Nullable
    public RemoteDistributionPackage retrievePackage(@NotNull ResourceResolver resourceResolver, @NotNull DistributionRequest distributionRequest, @NotNull DistributionTransportContext distributionContext) throws DistributionException {
        log.debug("pulling from {}", distributionEndpoint.getUri());

        try {
            URI distributionURI = RequestUtils.appendDistributionRequest(distributionEndpoint.getUri(), distributionRequest);

            Executor executor = getExecutor(distributionContext);

            // TODO : add queue parameter
            InputStream inputStream = HttpTransportUtils.fetchNextPackage(executor, distributionURI, httpConfiguration);

            if (inputStream == null) {
                return null;
            }

            try {
                final DistributionPackage responsePackage = packageBuilder.readPackage(resourceResolver, inputStream);
                responsePackage.getInfo().put(PACKAGE_INFO_PROPERTY_ORIGIN_URI, distributionURI);
                log.debug("pulled package with info {}", responsePackage.getInfo());

                return new DefaultRemoteDistributionPackage(responsePackage, executor, distributionURI);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        } catch (HttpHostConnectException e) {
            log.debug("could not connect to {} - skipping", distributionEndpoint.getUri());
        } catch (Exception ex) {
            log.error("cannot retrieve packages", ex);
        }

        return null;
    }

    private String getHostAndPort(URI uri) {
        return uri.getHost() + ":" + uri.getPort();
    }


    private Executor getExecutor(DistributionTransportContext distributionContext) {
        Executor executor = distributionContext.get(contextKeyExecutor, Executor.class);
        if (executor == null) {
            executor = buildExecutor();
            distributionContext.put(contextKeyExecutor, executor);
        }
        return executor;
    }

    private Executor buildAuthExecutor(String username, String password) {
        URI uri = distributionEndpoint.getUri();
        Executor executor = Executor.newInstance()
                .auth(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), username, password)
                .authPreemptive(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()));
        log.debug("authenticate user={}, endpoint={}", username, uri);
        return executor;
    }

    private Executor buildAuthExecutor(Map<String, String> credentialsMap) {
        return (null != credentialsMap && !credentialsMap.containsKey(AUTHORIZATION))
                ? buildAuthExecutor(credentialsMap.get(USERNAME), credentialsMap.get(PASSWORD))
                : Executor.newInstance();
    }

    private Executor buildExecutor() {
        Map<String, String> credentialsMap = getCredentialsMap();
        return buildAuthExecutor(credentialsMap);
    }

    private String getAuthSecret() {
        Map<String, String> credentialsMap = getCredentialsMap();
        if (null != credentialsMap && credentialsMap.containsKey(AUTHORIZATION)) {
            return credentialsMap.get(AUTHORIZATION);
        }
        return null;
    }
    
    private Map<String, String> getCredentialsMap() {
        DistributionTransportSecret secret = secretProvider.getSecret(distributionEndpoint.getUri());
        Map<String, String> credentialsMap = null;
        if (null != secret) {
            credentialsMap = secret.asCredentialsMap();
        }
        return credentialsMap;
    }

}
