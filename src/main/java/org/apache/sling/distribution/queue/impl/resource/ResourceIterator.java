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

package org.apache.sling.distribution.queue.impl.resource;


import org.apache.sling.api.resource.Resource;

import java.util.Iterator;
import java.util.Stack;

public class ResourceIterator implements Iterator<Resource> {

    private final String folderResourceType;
    private final boolean includeFolders;
    private final boolean includeLeafs;
    private final boolean includeRoot = false;
    private final String rootPath;

    private Stack<Iterator<Resource>> folderIterators = new Stack<Iterator<Resource>>();

    private Resource currentFolder;
    private Iterator<Resource> currentIterator;

    private Resource next;


    public ResourceIterator(Resource root, String folderResourceType, boolean includeFolders, boolean includeLeafs) {
        this.folderResourceType = folderResourceType;
        this.includeFolders = includeFolders;
        this.includeLeafs = includeLeafs;
        this.rootPath = root.getPath();

        currentFolder = root;
        currentIterator = currentFolder.listChildren();
        next = seek();
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public Resource next() {
        Resource result = next;
        next = seek();
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    Resource seek() {
        Resource res;
        while ((res = seekAll()) != null) {

            if (rootPath.equals(res.getPath())) {
                if (includeRoot) {
                    return res;
                } else {
                    continue;
                }
            }

            if (includeFolders && res.isResourceType(folderResourceType)) {
                return res;
            }

            if (includeLeafs && !res.isResourceType(folderResourceType)) {
                return res;
            }
        }

        return null;
    }

    // depth first, post order (children before parents)
    Resource seekAll() {

        while (currentIterator != null) {
            if (currentIterator.hasNext()) {
                Resource res = currentIterator.next();

                if (res.isResourceType(folderResourceType)) {
                    folderIterators.push(currentIterator);

                    currentFolder = res;
                    currentIterator = currentFolder.listChildren();
                } else {
                    return res;
                }
            } else {
                Resource folder = currentFolder;

                currentFolder  = currentFolder.getParent();
                currentIterator = folderIterators.empty() ? null  : folderIterators.pop();

                return folder;
            }
        }

        return null;
    }
}
