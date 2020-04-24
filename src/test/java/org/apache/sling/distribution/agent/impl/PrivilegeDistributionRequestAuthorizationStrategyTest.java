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
package org.apache.sling.distribution.agent.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for {@link PrivilegeDistributionRequestAuthorizationStrategy}
 */
public class PrivilegeDistributionRequestAuthorizationStrategyTest {

    @Rule
    public final OsgiContext context = new OsgiContext();

    final String JCR_PRIVILEGE_PROP = "jcrPrivilege";

    final String ADDITIONAL_JCR_PRIVILEGES_FOR_ADD_PROP = "additionalJcrPrivilegesForAdd";

    final String ADDITIONAL_JCR_PRIVILEGES_FOR_DELETE_PROP = "additionalJcrPrivilegesForDelete";

    @Test(expected = DistributionException.class)
    public void testCheckPermissionWithoutSession() throws Exception {
        String jcrPrivilege = "foo";
        DistributionRequestAuthorizationStrategy strategy = context.registerInjectActivateService(new PrivilegeDistributionRequestAuthorizationStrategyFactory(),
                JCR_PRIVILEGE_PROP, jcrPrivilege);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }

    @Test
    public void testCheckPermissionWithSession() throws Exception {
        String jcrPrivilege = "foo";
        DistributionRequestAuthorizationStrategy strategy = context.registerInjectActivateService(new PrivilegeDistributionRequestAuthorizationStrategyFactory(),
                JCR_PRIVILEGE_PROP, jcrPrivilege);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }

    @Test(expected = DistributionException.class)
    public void testNoPermissionOnAdd() throws Exception {
        String jcrPrivilege = "somePermission";
        DistributionRequestAuthorizationStrategy strategy = context.registerInjectActivateService(new PrivilegeDistributionRequestAuthorizationStrategyFactory(),
                JCR_PRIVILEGE_PROP, jcrPrivilege);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(jcrPrivilege)).thenReturn(privilege);

        when(session.getAccessControlManager()).thenReturn(acm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        String[] paths = new String[]{"/foo"};
        for (String path : paths) {
            when(acm.hasPrivileges(path, new Privilege[]{privilege})).thenReturn(false);
        }
        when(distributionRequest.getPaths()).thenReturn(paths);

        when(distributionRequest.getRequestType()).thenReturn(DistributionRequestType.ADD);

        strategy.checkPermission(resourceResolver, distributionRequest);
    }

    @Test
    public void testDefaultPermissionOnAdd() throws Exception {
        String jcrPrivilege = "somePermission";
        String DEFAULT_READ_PRIVELEGE = "jcr:read";
        DistributionRequestAuthorizationStrategy strategy = context.registerInjectActivateService(new PrivilegeDistributionRequestAuthorizationStrategyFactory(),
                JCR_PRIVILEGE_PROP, jcrPrivilege);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(jcrPrivilege)).thenReturn(privilege);
        Privilege jcrReadPrivilege = mock(Privilege.class);
        when(acm.privilegeFromName(DEFAULT_READ_PRIVELEGE)).thenReturn(jcrReadPrivilege);

        when(session.getAccessControlManager()).thenReturn(acm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        String[] paths = new String[]{"/foo"};
        for (String path : paths) {
            when(acm.hasPrivileges(path, new Privilege[]{privilege, jcrReadPrivilege})).thenReturn(true);
        }
        when(distributionRequest.getPaths()).thenReturn(paths);

        when(distributionRequest.getRequestType()).thenReturn(DistributionRequestType.ADD);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }
    
    @Test
    public void testAdditionalPermissionsOnAdd() throws Exception {
        String jcrPrivilege = "somePermission";
        String[] additionalJcrPrivilegesForAdd = new String[]{"addPermission"};
        DistributionRequestAuthorizationStrategy strategy = context.registerInjectActivateService(new PrivilegeDistributionRequestAuthorizationStrategyFactory(),
                JCR_PRIVILEGE_PROP, jcrPrivilege, ADDITIONAL_JCR_PRIVILEGES_FOR_ADD_PROP, additionalJcrPrivilegesForAdd);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(jcrPrivilege)).thenReturn(privilege);
        Privilege additionalPrivilegeForAdd = mock(Privilege.class);
        when(acm.privilegeFromName(additionalJcrPrivilegesForAdd[0])).thenReturn(additionalPrivilegeForAdd);

        when(session.getAccessControlManager()).thenReturn(acm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        String[] paths = new String[]{"/foo"};
        for (String path : paths) {
            when(acm.hasPrivileges(path, new Privilege[]{privilege, additionalPrivilegeForAdd})).thenReturn(true);
        }
        when(distributionRequest.getPaths()).thenReturn(paths);

        when(distributionRequest.getRequestType()).thenReturn(DistributionRequestType.ADD);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }

    @Test(expected = DistributionException.class)
    public void testNoPermissionOnDelete() throws Exception {
        String jcrPrivilege = "somePermission";
        DistributionRequestAuthorizationStrategy strategy = context.registerInjectActivateService(new PrivilegeDistributionRequestAuthorizationStrategyFactory(),
                JCR_PRIVILEGE_PROP, jcrPrivilege);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(jcrPrivilege)).thenReturn(privilege);

        when(session.getAccessControlManager()).thenReturn(acm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        String[] paths = new String[]{"/foo"};
        for (String path : paths) {
            when(acm.hasPrivileges(path, new Privilege[]{privilege})).thenReturn(false);
            when(session.nodeExists(path)).thenReturn(true);
        }
        when(distributionRequest.getPaths()).thenReturn(paths);

        when(distributionRequest.getRequestType()).thenReturn(DistributionRequestType.DELETE);

        strategy.checkPermission(resourceResolver, distributionRequest);
    }

    @Test
    public void testDefaultPermissionOnDelete() throws Exception {
        String jcrPrivilege = "somePermission";
        String DEFAULT_DELETE_PRIVELEGE = "jcr:removeNode";
        DistributionRequestAuthorizationStrategy strategy = context.registerInjectActivateService(new PrivilegeDistributionRequestAuthorizationStrategyFactory(),
                JCR_PRIVILEGE_PROP, jcrPrivilege);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(jcrPrivilege)).thenReturn(privilege);
        Privilege jcrDeletePrivilege = mock(Privilege.class);
        when(acm.privilegeFromName(DEFAULT_DELETE_PRIVELEGE)).thenReturn(jcrDeletePrivilege);

        when(session.getAccessControlManager()).thenReturn(acm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        String[] paths = new String[]{"/foo"};
        for (String path : paths) {
            when(acm.hasPrivileges(path, new Privilege[]{privilege, jcrDeletePrivilege})).thenReturn(true);
            when(session.nodeExists(path)).thenReturn(true);
        }
        when(distributionRequest.getPaths()).thenReturn(paths);

        when(distributionRequest.getRequestType()).thenReturn(DistributionRequestType.DELETE);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }
    
    @Test
    public void testAdditionalPermissionsOnDelete() throws Exception {
        String jcrPrivilege = "somePermission";
        String[] additionalJcrPrivilegesForDelete = new String[]{"deletePermission"};
        DistributionRequestAuthorizationStrategy strategy = context.registerInjectActivateService(new PrivilegeDistributionRequestAuthorizationStrategyFactory(),
                JCR_PRIVILEGE_PROP, jcrPrivilege, ADDITIONAL_JCR_PRIVILEGES_FOR_DELETE_PROP, additionalJcrPrivilegesForDelete);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(jcrPrivilege)).thenReturn(privilege);
        Privilege additionalPrivilegeForDelete = mock(Privilege.class);
        when(acm.privilegeFromName(additionalJcrPrivilegesForDelete[0])).thenReturn(additionalPrivilegeForDelete);

        when(session.getAccessControlManager()).thenReturn(acm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        String[] paths = new String[]{"/foo"};
        for (String path : paths) {
            when(acm.hasPrivileges(path, new Privilege[]{privilege, additionalPrivilegeForDelete})).thenReturn(true);
            when(session.nodeExists(path)).thenReturn(true);
        }
        when(distributionRequest.getPaths()).thenReturn(paths);

        when(distributionRequest.getRequestType()).thenReturn(DistributionRequestType.DELETE);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }

}