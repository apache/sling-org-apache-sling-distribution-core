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
import org.junit.Test;

/**
 * Tests for {@link PrivilegeDistributionRequestAuthorizationStrategy}
 */
public class PrivilegeDistributionRequestAuthorizationStrategyTest {

    String jcrPrivilege = "foo";
    String[] additionalJcrPrivilegesForAdd = {"addPermission"};
    String[] additionalJcrPrivilegesForDelete = {"deletePermission"};

    @Test(expected = DistributionException.class)
    public void testCheckPermissionWithoutSession() throws Exception {
        PrivilegeDistributionRequestAuthorizationStrategy strategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege, additionalJcrPrivilegesForAdd, additionalJcrPrivilegesForDelete);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }

    @Test
    public void testCheckPermissionWithSession() throws Exception {
        PrivilegeDistributionRequestAuthorizationStrategy strategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege, additionalJcrPrivilegesForAdd, additionalJcrPrivilegesForDelete);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }

    @Test(expected = DistributionException.class)
    public void testNoPermissionOnAdd() throws Exception {
        additionalJcrPrivilegesForAdd = new String[0];
        PrivilegeDistributionRequestAuthorizationStrategy strategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege, additionalJcrPrivilegesForAdd, additionalJcrPrivilegesForDelete);
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
    public void testPermissionOnAdd() throws Exception {
        additionalJcrPrivilegesForAdd = new String[0];
        PrivilegeDistributionRequestAuthorizationStrategy strategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege, additionalJcrPrivilegesForAdd, additionalJcrPrivilegesForDelete);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(jcrPrivilege)).thenReturn(privilege);
        Privilege jcrReadPrivilege = mock(Privilege.class);
        when(acm.privilegeFromName(Privilege.JCR_READ)).thenReturn(jcrReadPrivilege);

        when(session.getAccessControlManager()).thenReturn(acm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        String[] paths = new String[]{"/foo"};
        for (String path : paths) {
            when(acm.hasPrivileges(path, new Privilege[]{jcrReadPrivilege, privilege})).thenReturn(true);
        }
        when(distributionRequest.getPaths()).thenReturn(paths);

        when(distributionRequest.getRequestType()).thenReturn(DistributionRequestType.ADD);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }
    
    @Test
    public void testAdditionalPermissionsOnAdd() throws Exception {
        PrivilegeDistributionRequestAuthorizationStrategy strategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege, additionalJcrPrivilegesForAdd, additionalJcrPrivilegesForDelete);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(jcrPrivilege)).thenReturn(privilege);
        Privilege additionalPrivilegeForAdd = mock(Privilege.class);
        for (String privilegeAdd : additionalJcrPrivilegesForAdd) {
            when(acm.privilegeFromName(privilegeAdd)).thenReturn(additionalPrivilegeForAdd);
        }

        when(session.getAccessControlManager()).thenReturn(acm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        String[] paths = new String[]{"/foo"};
        for (String path : paths) {
            when(acm.hasPrivileges(path, new Privilege[]{additionalPrivilegeForAdd, privilege})).thenReturn(true);
        }
        when(distributionRequest.getPaths()).thenReturn(paths);

        when(distributionRequest.getRequestType()).thenReturn(DistributionRequestType.ADD);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }

    @Test(expected = DistributionException.class)
    public void testNoPermissionOnDelete() throws Exception {
        additionalJcrPrivilegesForDelete = new String[0];
        PrivilegeDistributionRequestAuthorizationStrategy strategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege, additionalJcrPrivilegesForAdd, additionalJcrPrivilegesForDelete);
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
    public void testPermissionOnDelete() throws Exception {
        additionalJcrPrivilegesForDelete = new String[0];
        PrivilegeDistributionRequestAuthorizationStrategy strategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege, additionalJcrPrivilegesForAdd, additionalJcrPrivilegesForDelete);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(jcrPrivilege)).thenReturn(privilege);
        Privilege jcrDeletePrivilege = mock(Privilege.class);
        when(acm.privilegeFromName(Privilege.JCR_REMOVE_NODE)).thenReturn(jcrDeletePrivilege);

        when(session.getAccessControlManager()).thenReturn(acm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        String[] paths = new String[]{"/foo"};
        for (String path : paths) {
            when(acm.hasPrivileges(path, new Privilege[]{jcrDeletePrivilege, privilege})).thenReturn(true);
            when(session.nodeExists(path)).thenReturn(true);
        }
        when(distributionRequest.getPaths()).thenReturn(paths);

        when(distributionRequest.getRequestType()).thenReturn(DistributionRequestType.DELETE);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }
    
    @Test
    public void testAdditionalPermissionsOnDelete() throws Exception {
        PrivilegeDistributionRequestAuthorizationStrategy strategy = new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege, additionalJcrPrivilegesForAdd, additionalJcrPrivilegesForDelete);
        DistributionRequest distributionRequest = mock(DistributionRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(jcrPrivilege)).thenReturn(privilege);
        Privilege additionalPrivilegeForDelete = mock(Privilege.class);
        for (String privilegeDelete : additionalJcrPrivilegesForDelete) {
            when(acm.privilegeFromName(privilegeDelete)).thenReturn(additionalPrivilegeForDelete);
        }

        when(session.getAccessControlManager()).thenReturn(acm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        String[] paths = new String[]{"/foo"};
        for (String path : paths) {
            when(acm.hasPrivileges(path, new Privilege[]{additionalPrivilegeForDelete, privilege})).thenReturn(true);
            when(session.nodeExists(path)).thenReturn(true);
        }
        when(distributionRequest.getPaths()).thenReturn(paths);

        when(distributionRequest.getRequestType()).thenReturn(DistributionRequestType.DELETE);
        strategy.checkPermission(resourceResolver, distributionRequest);
    }

}