/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.ajax.gwt.helper;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tika.io.IOUtils;
import org.jahia.ajax.gwt.client.data.acl.GWTJahiaNodeACE;
import org.jahia.ajax.gwt.client.data.acl.GWTJahiaNodeACL;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaNodeProperty;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaNodePropertyValue;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaNode;
import org.jahia.ajax.gwt.client.service.GWTJahiaServiceException;
import org.jahia.ajax.gwt.content.server.GWTFileManagerUploadServlet;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.*;
import org.jahia.services.importexport.ImportExportBaseService;
import org.jahia.services.importexport.ReferencesHelper;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.utils.i18n.JahiaResourceBundle;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 *
 * @author rfelden
 * @version 20 juin 2008 - 12:49:42
 */
public class ContentManagerHelper {
// ------------------------------ FIELDS ------------------------------

    private static Logger logger = Logger.getLogger(ContentManagerHelper.class);

    private JahiaSitesService sitesService;
    private ImportExportBaseService importExport;

    private NavigationHelper navigation;
    private PropertiesHelper properties;
    private VersioningHelper versioning;

// --------------------- GETTER / SETTER METHODS ---------------------

    public void setImportExport(ImportExportBaseService importExport) {
        this.importExport = importExport;
    }

    public void setNavigation(NavigationHelper navigation) {
        this.navigation = navigation;
    }

    public void setProperties(PropertiesHelper properties) {
        this.properties = properties;
    }

    public void setSitesService(JahiaSitesService sitesService) {
        this.sitesService = sitesService;
    }

    public void setVersioning(VersioningHelper versioning) {
        this.versioning = versioning;
    }

    public JCRNodeWrapper addNode(JCRNodeWrapper parentNode, String name, String nodeType, List<String> mixin,
                                  List<GWTJahiaNodeProperty> props) throws GWTJahiaServiceException {
        if (!parentNode.hasPermission(JCRNodeWrapper.WRITE)) {
            throw new GWTJahiaServiceException(
                    new StringBuilder(parentNode.getPath()).append(" - ACCESS DENIED").toString());
        }
        JCRNodeWrapper childNode = null;
        if (!parentNode.isFile() && parentNode.isWriteable() && !parentNode.isLocked()) {
            try {
                if (!parentNode.isCheckedOut()) {
                    parentNode.checkout();
                }
                childNode = parentNode.addNode(name, nodeType);
                if (mixin != null) {
                    for (String m : mixin) {
                        childNode.addMixin(m);
                    }
                }
                properties.setProperties(childNode, props);
            } catch (Exception e) {
                logger.error("Exception", e);
                throw new GWTJahiaServiceException("Node creation failed. Cause: " + e.getMessage());
            }
        }
        if (childNode == null) {
            throw new GWTJahiaServiceException("Node creation failed");
        }
        return childNode;
    }

    private JCRNodeWrapper unsecureAddNode(JCRNodeWrapper parentNode, String name, String nodeType,
                                           List<GWTJahiaNodeProperty> props) throws GWTJahiaServiceException {
        JCRNodeWrapper childNode = null;
        if (!parentNode.isFile()) {
            try {
                if (!parentNode.isCheckedOut()) {
                    parentNode.checkout();
                }
                childNode = parentNode.addNode(name, nodeType);
                properties.setProperties(childNode, props);
            } catch (Exception e) {
                logger.error("Exception", e);
                throw new GWTJahiaServiceException("Node creation failed");
            }
        }
        if (childNode == null) {
            throw new GWTJahiaServiceException("Node creation failed");
        }
        return childNode;
    }

    public GWTJahiaNode createNode(String parentPath, String name, String nodeType, List<String> mixin,
                                   List<GWTJahiaNodeProperty> props, JCRSessionWrapper currentUserSession)
            throws GWTJahiaServiceException {
        JCRNodeWrapper parentNode;
        final JCRSessionWrapper jcrSessionWrapper;
        try {
            jcrSessionWrapper = currentUserSession;
            parentNode = jcrSessionWrapper.getNode(parentPath);
        } catch (RepositoryException e) {
            logger.error(e.toString(), e);
            throw new GWTJahiaServiceException(
                    new StringBuilder(parentPath).append(" could not be accessed :\n").append(e.toString()).toString());
        }
        String nodeName = name;

        if (nodeName == null) {
            nodeName = findAvailableName(parentNode, nodeType.substring(nodeType.lastIndexOf(":") + 1),
                    currentUserSession);
        } else {
            nodeName = findAvailableName(parentNode, nodeName, currentUserSession);
        }
        checkName(nodeName);
        if (checkExistence(parentPath + "/" + nodeName, currentUserSession)) {
            throw new GWTJahiaServiceException("A node already exists with name '" + nodeName + "'");
        }

        JCRNodeWrapper childNode = addNode(parentNode, nodeName, nodeType, mixin, props);
        return navigation.getGWTJahiaNode(childNode);
    }

    public String generateNameFromTitle(List<GWTJahiaNodeProperty> props) {
        String nodeName = null;
        for (GWTJahiaNodeProperty property : props) {
            if (property != null) {
                final List<GWTJahiaNodePropertyValue> propertyValues = property.getValues();
                if (property.getName().equals("jcr:title") && propertyValues != null && propertyValues.size() > 0 &&
                        propertyValues.get(0).getString() != null) {
                    nodeName = JCRContentUtils.generateNodeName(propertyValues.get(0).getString(), 32);
                }
            } else {
                logger.error("found a null property");
            }
        }
        return nodeName;
    }

// -------------------------- OTHER METHODS --------------------------

    public void createFolder(String parentPath, String name, JCRSessionWrapper currentUserSession)
            throws GWTJahiaServiceException {
        JCRNodeWrapper parentNode;
        final JCRSessionWrapper jcrSessionWrapper;
        try {
            jcrSessionWrapper = currentUserSession;
            parentNode = jcrSessionWrapper.getNode(parentPath);
            if (parentNode.isNodeType("jnt:folder")) {
                createNode(parentPath, name, "jnt:folder", null, new ArrayList<GWTJahiaNodeProperty>(), currentUserSession);
            } else {
                createNode(parentPath, name, "jnt:contentList", null, new ArrayList<GWTJahiaNodeProperty>(), currentUserSession);
            }

            currentUserSession.save();            
        } catch (RepositoryException e) {
            logger.error(e.toString(), e);
            throw new GWTJahiaServiceException(
                    new StringBuilder(parentPath).append(" could not be accessed :\n").append(e.toString()).toString());
        }
    }

    public String findAvailableName(JCRNodeWrapper dest, String name, JCRSessionWrapper currentUserSession)
            throws GWTJahiaServiceException {
        return JCRContentUtils.findAvailableNodeName(dest, name);
    }

    /**
     * Check name
     *
     * @param name
     * @throws GWTJahiaServiceException
     */
    public void checkName(String name) throws GWTJahiaServiceException {
        if (name.indexOf("*") > 0 || name.indexOf("/") > 0 || name.indexOf(":") > 0 || name.indexOf("\"") > 0) {
            throw new GWTJahiaServiceException("Invalid name : characters *,/,\",: cannot be used here");
        }
    }

    public boolean checkExistence(String path, JCRSessionWrapper currentUserSession) throws GWTJahiaServiceException {
        boolean exists = false;
        try {
            currentUserSession.getNode(path);
            exists = true;
        } catch (PathNotFoundException e) {
            exists = false;
        } catch (RepositoryException e) {
            logger.error(e.toString(), e);
            throw new GWTJahiaServiceException("Error:\n" + e.toString());
        }
        return exists;
    }

    public void move(String sourcePath, String targetPath, JCRSessionWrapper currentUserSession)
            throws RepositoryException, InvalidItemStateException, ItemExistsException {
        JCRSessionWrapper session = currentUserSession;
        session.move(sourcePath, targetPath);
        session.save();
    }

    /**
     * Remove deleted children and reorder
     *
     * @param newChildrenList
     */
    public void updateChildren(final GWTJahiaNode parentNode, final List<GWTJahiaNode> newChildrenList,
                               final JCRSessionWrapper currentUserSession) throws GWTJahiaServiceException {
        try {
            if (newChildrenList == null) {
                return;
            }

            final JCRNodeWrapper targetNode = currentUserSession.getNode(parentNode.getPath());

            if (!targetNode.isCheckedOut()) {
                currentUserSession.checkout(targetNode);
            }


            // remove deleted children
            NodeIterator oldChildrenNodes = targetNode.getNodes();
            while (oldChildrenNodes.hasNext()) {
                JCRNodeWrapper currentChildNode = (JCRNodeWrapper) oldChildrenNodes.nextNode();

                GWTJahiaNode comparingGWTJahiaNode = new GWTJahiaNode();
                comparingGWTJahiaNode.setPath(currentChildNode.getPath());

                // node has been deleted
                if (!newChildrenList.contains(comparingGWTJahiaNode)) {
                    currentChildNode.remove();
                }

            }

            // reorder existing ones
            if (newChildrenList != null) {
                for (GWTJahiaNode childNode : newChildrenList) {
                    targetNode.orderBefore(childNode.getName(), null);
                }
            }
            currentUserSession.save();
        } catch (RepositoryException e) {
            logger.error(e, e);
        }

    }

    public void moveAtEnd(String sourcePath, String targetPath, JCRSessionWrapper currentUserSession)
            throws RepositoryException, InvalidItemStateException, ItemExistsException, GWTJahiaServiceException {
        JCRSessionWrapper session = currentUserSession;
        final JCRNodeWrapper srcNode = session.getNode(sourcePath);
        final JCRNodeWrapper targetNode = session.getNode(targetPath);
        if (!targetNode.isCheckedOut()) {
            targetNode.checkout();
        }

        if (srcNode.getParent().getPath().equals(targetNode.getPath())) {
            if (targetNode.getPrimaryNodeType().hasOrderableChildNodes()) {
                targetNode.orderBefore(srcNode.getName(), null);
            }
        } else {
            if (!srcNode.isCheckedOut()) {
                srcNode.checkout();
            }
            if (!srcNode.getParent().isCheckedOut()) {
                srcNode.getParent().checkout();
            }
            String newname = findAvailableName(targetNode, srcNode.getName(), currentUserSession);
            session.move(sourcePath, targetNode.getPath() + "/" + newname);
            if (targetNode.getPrimaryNodeType().hasOrderableChildNodes()) {
                targetNode.orderBefore(newname, null);
            }
        }
        session.save();
    }

    public void moveOnTopOf(String sourcePath, String targetPath, JCRSessionWrapper currentUserSession)
            throws RepositoryException, InvalidItemStateException, ItemExistsException, GWTJahiaServiceException {
        JCRSessionWrapper session = currentUserSession;
        final JCRNodeWrapper srcNode = session.getNode(sourcePath);
        final JCRNodeWrapper targetNode = session.getNode(targetPath);
        final JCRNodeWrapper targetParent = (JCRNodeWrapper) targetNode.getParent();
        if (!targetParent.isCheckedOut()) {
            targetParent.checkout();
        }
        if (srcNode.getParent().getPath().equals(targetParent.getPath())) {
            if (targetParent.getPrimaryNodeType().hasOrderableChildNodes()) {
                targetParent.orderBefore(srcNode.getName(), targetNode.getName());
            }
        } else {
            if (!srcNode.isCheckedOut()) {
                srcNode.checkout();
            }
            if (!srcNode.getParent().isCheckedOut()) {
                srcNode.getParent().checkout();
            }
            String newname = findAvailableName(targetParent, srcNode.getName(), currentUserSession);
            session.move(sourcePath, targetParent.getPath() + "/" + newname);
            if (targetParent.getPrimaryNodeType().hasOrderableChildNodes()) {
                targetParent.orderBefore(newname, targetNode.getName());
            }
        }
        session.save();
    }

    public void checkWriteable(List<String> paths, JahiaUser user, JCRSessionWrapper currentUserSession)
            throws GWTJahiaServiceException {
        List<String> missedPaths = new ArrayList<String>();
        for (String aNode : paths) {
            JCRNodeWrapper node;
            try {
                node = currentUserSession.getNode(aNode);
            } catch (RepositoryException e) {
                logger.error(e.toString(), e);
                missedPaths.add(new StringBuilder(aNode).append(" could not be accessed : ")
                        .append(e.toString()).toString());
                continue;
            }
            if (!node.hasPermission(JCRNodeWrapper.WRITE)) {
                missedPaths.add(new StringBuilder("User ").append(user.getUsername()).append(" has no write access to ")
                        .append(node.getName()).toString());
            } else if (node.isLocked() && !node.getLockOwner().equals(user.getUsername())) {
                missedPaths.add(new StringBuilder(node.getName()).append(" is locked by ")
                        .append(user.getUsername()).toString());
            }
        }
        if (missedPaths.size() > 0) {
            StringBuilder errors = new StringBuilder("The following files could not be cut:");
            for (String err : missedPaths) {
                errors.append("\n").append(err);
            }
            throw new GWTJahiaServiceException(errors.toString());
        }
    }

    public List<GWTJahiaNode> copy(List<String> pathsToCopy, String destinationPath, String newName, boolean moveOnTop,
                                   boolean cut, boolean reference, JCRSessionWrapper currentUserSession)
            throws GWTJahiaServiceException {
        List<GWTJahiaNode> res = new ArrayList<GWTJahiaNode>();

        List<String> missedPaths = new ArrayList<String>();

        final JCRNodeWrapper targetParent;
        JCRNodeWrapper targetNode;
        try {
            targetNode = currentUserSession.getNode(destinationPath);

            if (moveOnTop) {
                targetParent = (JCRNodeWrapper) targetNode.getParent();
            } else {
                targetParent = targetNode;
            }
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }

        try {
            for (String aNode : pathsToCopy) {
                JCRNodeWrapper node = currentUserSession.getNode(aNode);
                String name = newName != null ? newName : node.getName();
                if (node.hasPermission(JCRNodeWrapper.READ)) {
                    try {
                        name = findAvailableName(targetParent, name, currentUserSession);
                        if (targetParent.isWriteable() && !targetParent.isLocked()) {
                            final JCRNodeWrapper copy = doPaste(targetParent, node, name, cut, reference);

                            if (moveOnTop && targetParent.getPrimaryNodeType().hasOrderableChildNodes()) {
                                targetParent.orderBefore(name, targetNode.getName());
                            }
                            currentUserSession.save();
                            res.add(navigation.getGWTJahiaNode(copy));
                        } else {
                            missedPaths
                                    .add(new StringBuilder("File ").append(name).append(" could not be referenced in ")
                                            .append(targetParent.getPath()).toString());
                        }
                    } catch (RepositoryException e) {
                        logger.error("Exception", e);
                        missedPaths.add(new StringBuilder("File ").append(name).append(" could not be referenced in ")
                                .append(targetParent.getPath()).toString());
                    } catch (JahiaException e) {
                        logger.error("Exception", e);
                        missedPaths.add(new StringBuilder("File ").append(name).append(" could not be referenced in ")
                                .append(targetParent.getPath()).toString());
                    }
                } else {
                    missedPaths.add(new StringBuilder("Source file ").append(name).append(" could not be read ")
                            .append(" - ACCESS DENIED").toString());
                }
            }
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }

        if (missedPaths.size() > 0) {
            StringBuilder errors = new StringBuilder("The following files could not have their reference pasted:");
            for (String err : missedPaths) {
                errors.append("\n").append(err);
            }
            throw new GWTJahiaServiceException(errors.toString());
        }
        return res;
    }

    private JCRNodeWrapper doPaste(JCRNodeWrapper targetNode, JCRNodeWrapper node, String name, boolean cut,
                                   boolean reference) throws RepositoryException, JahiaException {
        if (cut) {
            node.checkout();
            targetNode.checkout();
            targetNode.getSession().move(node.getPath(), targetNode.getPath() + "/" + name);
        } else if (reference) {
            /*Property p = */
            if (!targetNode.isCheckedOut()) {
                targetNode.checkout();
            }
            if (targetNode.getPrimaryNodeTypeName().equals("jnt:members")) {
                if (node.getPrimaryNodeTypeName().equals("jnt:user")) {
                    Node member = targetNode.addNode(name, Constants.JAHIANT_MEMBER);
                    member.setProperty("j:member", node.getUUID());
                } else if (node.getPrimaryNodeTypeName().equals("jnt:group")) {
                    Node node1 = node.getParent().getParent();
                    int id = 0;
                    if (node1 != null && node1.getPrimaryNodeType().getName().equals(Constants.JAHIANT_VIRTUALSITE)) {
                        id = sitesService.getSiteByKey(node1.getName()).getID();
                    }
                    name += ("___" + id);
                    Node member = targetNode.addNode(name, Constants.JAHIANT_MEMBER);
                    member.setProperty("j:member", node.getUUID());
                }
            } else {
                targetNode.clone(node, name);
            }

        } else {
            node.copy(targetNode, name, true);
        }
        return targetNode.getNode(name);
    }

    public void deletePaths(List<String> paths, JahiaUser user, JCRSessionWrapper currentUserSession)
            throws GWTJahiaServiceException {
        List<String> missedPaths = new ArrayList<String>();
        for (String path : paths) {
            JCRNodeWrapper nodeToDelete;
            try {
                nodeToDelete = currentUserSession.getNode(path);
            } catch (RepositoryException e) {
                missedPaths.add(new StringBuilder(path).append(" could not be accessed : ")
                        .append(e.toString()).toString());
                continue;
            }
            if (!user.isRoot() && nodeToDelete.isLocked() && !nodeToDelete.getLockOwner().equals(user.getUsername())) {
                missedPaths.add(new StringBuilder(nodeToDelete.getPath()).append(" - locked by ")
                        .append(nodeToDelete.getLockOwner()).toString());
            }
            if (!nodeToDelete.hasPermission(JCRNodeWrapper.WRITE)) {
                missedPaths.add(new StringBuilder(nodeToDelete.getPath()).append(" - ACCESS DENIED").toString());
            } else if (!getRecursedLocksAndFileUsages(nodeToDelete, missedPaths, user.getUsername())) {
                try {
                    if (!nodeToDelete.getParent().isCheckedOut()) {
                        nodeToDelete.getParent().checkout();
                    }

                    nodeToDelete.remove();
                    nodeToDelete.saveSession();
                } catch (AccessDeniedException e) {
                    missedPaths.add(new StringBuilder(nodeToDelete.getPath()).append(" - ACCESS DENIED").toString());
                } catch (ReferentialIntegrityException e) {
                    missedPaths.add(new StringBuilder(nodeToDelete.getPath()).append(" - is in use").toString());
                } catch (RepositoryException e) {
                    logger.error("error", e);
                    missedPaths.add(new StringBuilder(nodeToDelete.getPath()).append(" - UNSUPPORTED").toString());
                }
            }
        }
        if (missedPaths.size() > 0) {
            StringBuilder errors = new StringBuilder("The following files could not be deleted:");
            for (String err : missedPaths) {
                errors.append("\n").append(err);
            }
            throw new GWTJahiaServiceException(errors.toString());
        }
    }

    public void rename(String path, String newName, JCRSessionWrapper currentUserSession)
            throws GWTJahiaServiceException {
        JCRNodeWrapper node;
        try {
            node = currentUserSession.getNode(path);
        } catch (RepositoryException e) {
            logger.error(e.toString(), e);
            throw new GWTJahiaServiceException(
                    new StringBuilder(path).append(" could not be accessed :\n").append(e.toString()).toString());
        }
        try {
            if (node.isLocked() && !node.getLockOwner().equals(currentUserSession.getUser().getUsername())) {
                throw new GWTJahiaServiceException(new StringBuilder(node.getName()).append(" is locked by ")
                        .append(currentUserSession.getUser().getUsername()).toString());
            } else if (!node.hasPermission(JCRNodeWrapper.WRITE)) {
                throw new GWTJahiaServiceException(
                        new StringBuilder(node.getName()).append(" - ACCESS DENIED").toString());
            } else if (!node.rename(newName)) {
                throw new GWTJahiaServiceException(
                        new StringBuilder("Could not rename file ").append(node.getName()).append(" into ")
                                .append(newName).toString());
            }
        } catch (ItemExistsException e) {
            throw new GWTJahiaServiceException(new StringBuilder(newName).append(" already exists").toString());
        } catch (RepositoryException e) {
            logger.error(e.toString(), e);
            throw new GWTJahiaServiceException(
                    new StringBuilder("Could not rename file ").append(node.getName()).append(" into ")
                            .append(newName).toString());
        }
        try {
            node.saveSession();
        } catch (RepositoryException e) {
            logger.error("error", e);
            throw new GWTJahiaServiceException(
                    new StringBuilder("Could not save file ").append(node.getName()).append(" into ")
                            .append(newName).toString());
        }
    }

    public void importContent(String parentPath, String fileKey) throws GWTJahiaServiceException {
        GWTFileManagerUploadServlet.Item item = GWTFileManagerUploadServlet.getItem(fileKey);
        try {
            if ("application/zip".equals(item.getContentType())) {
                try {
                    importExport.importZip(parentPath, item.getFile(), false);
                } finally {
                    item.dispose();
                }
            } else if ("application/xml".equals(item.getContentType()) || "text/xml".equals(item.getContentType())) {
                FileInputStream is = item.getStream();
                try {
                    importExport.importXML(parentPath, is, false);
                } finally {
                    IOUtils.closeQuietly(is);
                    item.dispose();
                }
            }
        } catch (Exception e) {
            logger.error("Error when importing", e);
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public GWTJahiaNodeACL getACL(String path, boolean newAcl, JCRSessionWrapper currentUserSession, Locale uiLocale)
            throws GWTJahiaServiceException {
        JCRNodeWrapper node;
        try {
            node = currentUserSession.getNode(path);
        } catch (RepositoryException e) {
            logger.error(e.toString(), e);
            throw new GWTJahiaServiceException(
                    new StringBuilder(path).append(" could not be accessed :\n").append(e.toString()).toString());
        }
        Map<String, List<String[]>> m = node.getAclEntries();

        GWTJahiaNodeACL acl = new GWTJahiaNodeACL();
        List<GWTJahiaNodeACE> aces = new ArrayList<GWTJahiaNodeACE>();

        for (Iterator<String> iterator = m.keySet().iterator(); iterator.hasNext();) {
            String principal = iterator.next();
            GWTJahiaNodeACE ace = new GWTJahiaNodeACE();
            ace.setPrincipalType(principal.charAt(0));
            ace.setPrincipal(principal.substring(2));

            List<String[]> st = m.get(principal);
            Map<String, String> perms = new HashMap<String, String>();
            Map<String, String> inheritedPerms = new HashMap<String, String>();
            String inheritedFrom = null;
            for (String[] strings : st) {
                if (newAcl || !path.equals(strings[0])) {
                    inheritedFrom = strings[0];
                    inheritedPerms.put(strings[2], strings[1]);
                } else {
                    perms.put(strings[2], strings[1]);
                }
            }

            ace.setInheritedFrom(inheritedFrom);
            ace.setInheritedPermissions(inheritedPerms);
            ace.setPermissions(perms);
            ace.setInherited(perms.isEmpty());

            aces.add(ace);
        }
        acl.setAce(aces);
        acl.setAvailablePermissions(new HashMap<String, List<String>>(node.getAvailablePermissions()));
        Map<String, String> labels = new HashMap<String, String>();
        for (List<String> list : acl.getAvailablePermissions().values()) {
            for (String s : list) {
                String k = s;
                if (k.contains(":")) {
                    k = k.substring(k.indexOf(':') + 1);
                }
                labels.put(s, JahiaResourceBundle.getJahiaInternalResource(
                        "org.jahia.engines.rights.ManageRights." + k + ".label", uiLocale, k));
            }
        }
        acl.setPermissionLabels(labels);
        acl.setAclDependencies(new HashMap<String, List<String>>());
        return acl;
    }

    public void setACL(String path, GWTJahiaNodeACL acl, JCRSessionWrapper currentUserSession)
            throws GWTJahiaServiceException {
        JCRNodeWrapper node;
        try {
            node = currentUserSession.getNode(path);
            if (!node.isCheckedOut()) {
                node.checkout();
            }
        } catch (RepositoryException e) {
            logger.error(e.toString(), e);
            throw new GWTJahiaServiceException(
                    new StringBuilder(path).append(" could not be accessed :\n").append(e.toString()).toString());
        }

        node.revokeAllPermissions();
        for (GWTJahiaNodeACE ace : acl.getAce()) {
            String user = ace.getPrincipalType() + ":" + ace.getPrincipal();
            if (!ace.isInherited()) {
                node.changePermissions(user, ace.getPermissions());
            }
        }
        try {
            node.save();
        } catch (RepositoryException e) {
            logger.error("error", e);
            throw new GWTJahiaServiceException("Could not save file " + node.getName());
        }
    }

    private boolean getRecursedLocksAndFileUsages(JCRNodeWrapper nodeToDelete, List<String> lockedNodes,
                                                  String username) {
        for (JCRNodeWrapper child : nodeToDelete.getChildren()) {
            getRecursedLocksAndFileUsages(child, lockedNodes, username);
            if (lockedNodes.size() >= 10) {
                // do not check further
                return true;
            }
        }
        if (nodeToDelete.isLocked() && !nodeToDelete.getLockOwner().equals(username)) {
            lockedNodes.add(new StringBuilder(nodeToDelete.getPath()).append(" - locked by ")
                    .append(nodeToDelete.getLockOwner()).toString());
        }
        return !lockedNodes.isEmpty();
    }

    public void setLock(List<String> paths, boolean locked, JCRSessionWrapper currentUserSession)
            throws GWTJahiaServiceException {
        JahiaUser user = currentUserSession.getUser();
        List<String> missedPaths = new ArrayList<String>();
        for (String path : paths) {
            JCRNodeWrapper node;
            try {
                node = currentUserSession.getNode(path);
            } catch (RepositoryException e) {
                logger.error(e.toString(), e);
                missedPaths.add(new StringBuilder(path).append(" could not be accessed : ")
                        .append(e.toString()).toString());
                continue;
            }
            if (!node.hasPermission(JCRNodeWrapper.WRITE)) {
                missedPaths.add(new StringBuilder(node.getName()).append(": write access denied").toString());
            } else if (node.isLocked()) {
                if (!locked) {
                    if (node.getLockOwner() != null && !node.getLockOwner().equals(user.getUsername()) &&
                            !user.isRoot()) {
                        missedPaths.add(new StringBuilder(node.getName()).append(": locked by ")
                                .append(node.getLockOwner()).toString());
                    } else {
                        try {
                            if (!node.forceUnlock()) {
                                missedPaths.add(new StringBuilder(node.getName())
                                        .append(": repository exception").toString());
                            }
                        } catch (RepositoryException e) {
                            logger.error(e.toString(), e);
                            missedPaths
                                    .add(new StringBuilder(node.getName()).append(": repository exception").toString());
                        }
                    }
                } else {
                    // already locked
                }
            } else {
                if (locked) {
                    try {
                        if (!node.lockAndStoreToken()) {
                            missedPaths
                                    .add(new StringBuilder(node.getName()).append(": repository exception").toString());
                        }
                    } catch (RepositoryException e) {
                        logger.error(e.toString(), e);
                        missedPaths.add(new StringBuilder(node.getName()).append(": repository exception").toString());
                    }
                } else {
                    // already unlocked
                }
            }
        }
        try {
            currentUserSession.save();
        } catch (RepositoryException e) {
            logger.error("error", e);
            throw new GWTJahiaServiceException("Could not save session");
        }
        if (missedPaths.size() > 0) {
            StringBuilder errors =
                    new StringBuilder("The following files could not be ").append(locked ? "locked:" : "unlocked:");
            for (String missedPath : missedPaths) {
                errors.append("\n").append(missedPath);
            }
            throw new GWTJahiaServiceException(errors.toString());
        }
    }

    /**
     * Uploda file depending on operation (add version, auto-rename or just upload)
     *
     * @param location
     * @param tmpName
     * @param operation
     * @param newName
     * @param currentUserSession
     * @throws GWTJahiaServiceException
     */
    public void uploadedFile(String location, String tmpName, int operation, String newName,
                             JCRSessionWrapper currentUserSession) throws GWTJahiaServiceException {
        try {
            JCRNodeWrapper parent = currentUserSession.getNode(location);
            switch (operation) {
                case 3:
                    JCRNodeWrapper node = (JCRNodeWrapper) parent.getNode(newName);
                    if (node == null) {
                        throw new GWTJahiaServiceException(
                                "Could'nt add a new version, file " + location + "/" + newName + "not found ");
                    }
                    versioning.addNewVersionFile(node, tmpName);
                    break;
                case 1:
                    newName = findAvailableName(parent, newName, currentUserSession);
                case 0:
                    if (parent.hasNode(newName)) {
                        throw new GWTJahiaServiceException("file exists");
                    }
                default:
                    GWTFileManagerUploadServlet.Item item = GWTFileManagerUploadServlet.getItem(tmpName);
                    FileInputStream is = null;
                    try {
                        is = item.getStream();
                        parent.uploadFile(newName, is, item.getContentType());
                    } catch (FileNotFoundException e) {
                        logger.error(e.getMessage(), e);
                    } finally {
                        IOUtils.closeQuietly(is);
                        item.dispose();
                    }
                    break;
            }
            parent.save();
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void synchro(final Map<String, String> pathsToSyncronize, JCRSessionWrapper currentUserSession)
            throws GWTJahiaServiceException {
        try {
            JCRTemplate.getInstance()
                    .doExecuteWithSystemSession(currentUserSession.getUser().getUsername(), new JCRCallback<Object>() {
                        public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                            HashMap<String, List<String>> references = new HashMap<String, List<String>>();
                            List<JCRNodeWrapper> pageTemplates = new ArrayList<JCRNodeWrapper>();
                            for (Map.Entry<String, String> entry : pathsToSyncronize.entrySet()) {
                                JCRNodeWrapper originalNode = session.getNode(entry.getKey());
                                JCRNodeWrapper destinationNode = null;
                                try {
                                    destinationNode = session.getNode(entry.getValue());
                                    synchro(originalNode, destinationNode, session, false, references, pageTemplates);
                                } catch (PathNotFoundException e) {
                                    destinationNode =
                                            session.getNode(StringUtils.substringBeforeLast(entry.getValue(), "/"));
                                    originalNode.copy(destinationNode,
                                            StringUtils.substringAfterLast(entry.getValue(), "/"), true);
                                }
                            }
                            ReferencesHelper.resolveCrossReferences(session, references);
                            session.save();
                            for (JCRNodeWrapper pageTemplate : pageTemplates) {
                                List<JCRNodeWrapper> pages = new ArrayList<JCRNodeWrapper>();
                                PropertyIterator pi = pageTemplate.getWeakReferences("j:sourceTemplate");
                                while (pi.hasNext()) {
                                    JCRPropertyWrapper property = (JCRPropertyWrapper) pi.next();
                                    if (property.getParent().isNodeType("jnt:page")) {
                                        pages.add(property.getParent());
                                    }
                                }
                                for (JCRNodeWrapper page : pages) {
                                    references = new HashMap<String, List<String>>();
                                    synchro(pageTemplate, page, session, true, references, null);
                                    ReferencesHelper.resolveCrossReferences(session, references);
                                    session.save();
                                }
                            }
                            return null;
                        }
                    });
            currentUserSession.save();
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public void synchro(final JCRNodeWrapper source, final JCRNodeWrapper destinationNode, JCRSessionWrapper session,
                        final boolean toPage, Map<String, List<String>> references, List<JCRNodeWrapper> pageTemplates)
            throws RepositoryException {
        if ("j:acl".equals(destinationNode.getName())) {
            return;
        }

        session.checkout(destinationNode);

        if (!source.hasProperty("j:templateDeployed")) {
            source.checkout();
            source.addMixin("jmix:templateInformation");
            source.setProperty("j:templateDeployed", true);
        }

        final Map<String, String> uuidMapping = session.getUuidMapping();

        if (!toPage) {
            final boolean sharedSource =
                    source.hasProperty("j:templateShared") && source.getProperty("j:templateShared").getBoolean();
            final boolean sharedDestination = destinationNode.hasProperty("j:templateShared") &&
                    destinationNode.getProperty("j:templateShared").getBoolean();
            if (!sharedSource && sharedDestination) {
                final JCRNodeWrapper parent = destinationNode.getParent();
                destinationNode.remove();
                source.copy(parent, source.getName(), !toPage);
                return;
            }

            try {
                NodeType[] mixin = source.getMixinNodeTypes();
                for (NodeType aMixin : mixin) {
                    destinationNode.addMixin(aMixin.getName());
                }
            } catch (RepositoryException e) {
                logger.error("Error adding mixin types to copy", e);
            }

            uuidMapping.put(source.getIdentifier(), destinationNode.getIdentifier());
            if (source.hasProperty("jcr:language")) {
                destinationNode.setProperty("jcr:language", source.getProperty("jcr:language").getString());
            }
            source.copyProperties(destinationNode, references);
        } else {
            if (source.isNodeType("jmix:templateInformation")) {
                destinationNode.addMixin("jmix:templateInformation");
            }
            if (source.hasProperty("j:templateLocked")) {
                destinationNode.setProperty("j:templateLocked", source.getProperty("j:templateLocked").getBoolean());
            }
            final boolean sharedSource =
                    source.hasProperty("j:templateShared") && source.getProperty("j:templateShared").getBoolean();
            final boolean sharedDestination = destinationNode.hasProperty("j:templateShared") &&
                    destinationNode.getProperty("j:templateShared").getBoolean();
            if (sharedSource && !sharedDestination) {
                final JCRNodeWrapper parent = destinationNode.getParent();
                destinationNode.remove();
                parent.clone(source, source.getName());
                return;
            } else if (!sharedSource && sharedDestination) {
                final JCRNodeWrapper parent = destinationNode.getParent();
                destinationNode.remove();
                source.copy(parent, source.getName(), !toPage);
                return;
            }
        }

        NodeIterator ni = source.getNodes();
        Set<String> names = new HashSet<String>();
        while (ni.hasNext()) {
            JCRNodeWrapper child = (JCRNodeWrapper) ni.next();
            names.add(child.getName());
            if ((child.hasProperty("j:templateShared") && child.getProperty("j:templateShared").getBoolean())) {
                if (uuidMapping.containsKey(child.getIdentifier())) {
                    // ugly save because to make node really shareable
                    session.save();
                    if (destinationNode.hasNode(child.getName())) {
                        JCRNodeWrapper node = destinationNode.getNode(child.getName());
                        synchro(child, node, session, toPage, references, pageTemplates);
                    } else {
                        destinationNode
                                .clone(session.getNodeByUUID(uuidMapping.get(child.getIdentifier())), child.getName());
                    }
                } else {
                    if (destinationNode.hasNode(child.getName())) {
                        JCRNodeWrapper node = destinationNode.getNode(child.getName());
                        synchro(child, node, session, toPage, references, pageTemplates);
                    } else {
                        if (!toPage) {
                            destinationNode.clone(child, child.getName());
                        } else {
                            if (!child.hasProperty("j:templateDeployed")) {
                                child.addMixin("jmix:templateInformation");
                                child.setProperty("j:templateDeployed", true);
                            }
                            child.copy(destinationNode, child.getName(), !toPage);
                        }
                    }
                }
            } else {
                if (destinationNode.hasNode(child.getName())) {
                    JCRNodeWrapper node = destinationNode.getNode(child.getName());
                    synchro(child, node, session, toPage, references, pageTemplates);
                } else {
                    if (!child.hasProperty("j:templateDeployed")) {
                        child.addMixin("jmix:templateInformation");
                        child.setProperty("j:templateDeployed", true);
                    }
                    child.copy(destinationNode, child.getName(), !toPage);
                }
            }
        }
        ni = destinationNode.getNodes();
        while (ni.hasNext()) {
            JCRNodeWrapper oldChild = (JCRNodeWrapper) ni.next();
            if (!names.contains(oldChild.getName()) && !oldChild.hasProperty("j:sourceTemplate") &&
                    oldChild.hasProperty("j:templateDeployed") &&
                    oldChild.getProperty("j:templateDeployed").getBoolean()) {
                oldChild.remove();
            }
        }

//        session.save();
        // deploy to pages
        if (!toPage && source.isNodeType("jnt:page")) {
            pageTemplates.add(destinationNode);
        }

    }


}
