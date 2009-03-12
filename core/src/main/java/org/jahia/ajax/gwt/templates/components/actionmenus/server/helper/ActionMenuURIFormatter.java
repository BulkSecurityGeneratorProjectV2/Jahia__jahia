/**
 * 
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Limited. All rights reserved.
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
 * in Jahia's FLOSS exception. You should have recieved a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license"
 * 
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.ajax.gwt.templates.components.actionmenus.server.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jahia.ajax.usersession.userSettings;
import org.jahia.content.ContentObject;
import org.jahia.data.containers.JahiaContainerDefinition;
import org.jahia.data.containers.JahiaContainerList;
import org.jahia.data.fields.JahiaField;
import org.jahia.engines.JahiaEngine;
import org.jahia.engines.addcontainer.AddContainer_Engine;
import org.jahia.engines.containerlistproperties.ContainerListProperties_Engine;
import org.jahia.engines.deletecontainer.DeleteContainer_Engine;
import org.jahia.engines.pages.PageProperties_Engine;
import org.jahia.engines.restorelivecontainer.RestoreLiveContainer_Engine;
import org.jahia.engines.updatecontainer.UpdateContainer_Engine;
import org.jahia.exceptions.JahiaException;
import org.jahia.params.ProcessingContext;
import org.jahia.registries.EnginesRegistry;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.acl.ACLResource;
import org.jahia.services.acl.ACLResourceInterface;
import org.jahia.services.acl.JahiaACLManagerService;
import org.jahia.services.acl.JahiaBaseACL;
import org.jahia.services.containers.ContentContainer;
import org.jahia.services.containers.ContentContainerList;
import org.jahia.services.fields.ContentPageField;
import org.jahia.services.lock.LockKey;
import org.jahia.services.pages.ContentPage;
import org.jahia.services.pages.JahiaPage;
import org.jahia.services.version.EntryLoadRequest;
import org.jahia.services.workflow.WorkflowService;

/**
 * Created by IntelliJ IDEA.
 *
 * @author rfelden
 * @version 30 janv. 2008 - 10:23:08
 */
public class ActionMenuURIFormatter {

    private final static Logger logger = Logger.getLogger(ActionMenuURIFormatter.class) ;


    // ================================================================
    //    CONTAINER LIST METHODS
    // ================================================================

    /**
     * Get the url to call add container action.
     *
     * @param jParams the processing context
     * @param contentContainerList the target container list
     * @return the link to open
     * @throws JahiaException sthg bad happened
     */
    public static String drawContainerListAddUrl(final ProcessingContext jParams, final ContentContainerList contentContainerList) throws JahiaException {
        if (contentContainerList != null) {
            final JahiaACLManagerService aclService = ServicesRegistry.getInstance().getJahiaACLManagerService();
            if (aclService.getSiteActionPermission("engines.actions.add",
                    jParams.getUser(), JahiaBaseACL.READ_RIGHTS,
                    jParams.getSiteID()) > 0 &&
                    aclService.getSiteActionPermission("engines.languages." + jParams.getLocale().toString(),
                    jParams.getUser(),
                    JahiaBaseACL.READ_RIGHTS,
                    jParams.getSiteID()) > 0) {

                if (canAddContainerInContainerList(contentContainerList.getJahiaContainerList(jParams, jParams.getEntryLoadRequest()))) {
                    return drawUrlCheckWriteAccess(jParams, AddContainer_Engine.ENGINE_NAME, contentContainerList.getJahiaContainerList(jParams, jParams.getEntryLoadRequest()), false, false);
                } else {
                    logger.debug("no rights for adding container to this specific container list") ;
                    return null ;
                }
            } else {
                logger.debug("no rights for adding container to a container list on this site") ;
                return null ;
            }
        } else {
            logger.warn("Trying to generate URL for null container list");
            return null ;
        }
    }

    /**
     * Get the url to call update container list action.
     *
     * @param jParams the processing context
     * @param contentContainerList the target container list
     * @return the link to open
     * @throws JahiaException sthg bad happened
     */
    public static String drawContainerListUpdateUrl(final ProcessingContext jParams, final ContentContainerList contentContainerList) throws JahiaException {
        if (contentContainerList != null) {
            return drawUrlCheckWriteAccess(jParams, ContainerListProperties_Engine.ENGINE_NAME, contentContainerList, false, false);
        } else {
            return null ;
        }
    }

    /**
     * Check if the given container list can receive containers.
     *
     * @param jahiaContainerList the container list
     * @return true if add is allowed, false otherwise
     * @throws JahiaException sthg bad happened
     */
    private static boolean canAddContainerInContainerList(JahiaContainerList jahiaContainerList) throws JahiaException {
        final int containerListTypePropValue = Integer.parseInt(jahiaContainerList.getDefinition().getProperties().getProperty(JahiaContainerDefinition.CONTAINER_LIST_TYPE_PROPERTY));
        return (containerListTypePropValue & JahiaContainerDefinition.SINGLE_TYPE) <= 0 || jahiaContainerList.getFullSize() == 0;
    }

    /**
     * Check if the given container can be deleted from the list.
     *
     * @param jahiaContainerList the container list
     * @return true if delete is allowed, false otherwise
     * @throws JahiaException sthg bad happened
     */
    private static boolean canDeleteContainerInContainerList(JahiaContainerList jahiaContainerList) throws JahiaException {
        final int containerListTypePropValue = Integer.parseInt(jahiaContainerList.getDefinition().getProperties().getProperty(JahiaContainerDefinition.CONTAINER_LIST_TYPE_PROPERTY));
        return (containerListTypePropValue & JahiaContainerDefinition.MANDATORY_TYPE) <= 0 || jahiaContainerList.getFullSize() > 1;
    }
    
    // ================================================================
    //    CONTAINER METHODS
    // ================================================================

    /**
     * Get the url to call update container action.
     *
     * @param jParams the processing context
     * @param contentContainer the target container list
     * @param focusedFieldId the field to give focus to
     * @return the link to open
     * @throws JahiaException sthg bad happened
     */
    public static String drawContainerUpdateUrl(final ProcessingContext jParams, final ContentContainer contentContainer, int focusedFieldId) throws JahiaException {
        final JahiaACLManagerService aclService = ServicesRegistry.getInstance().getJahiaACLManagerService();
        String url = null ;
        if (aclService.getSiteActionPermission("engines.actions.update",
                jParams.getUser(), JahiaBaseACL.READ_RIGHTS,
                jParams.getSiteID()) > 0 &&
                aclService.getSiteActionPermission("engines.languages." + jParams.getLocale().toString(),
                        jParams.getUser(),
                        JahiaBaseACL.READ_RIGHTS,
                        jParams.getSiteID()) > 0) {
            url = drawUrlCheckWriteAccess(jParams, UpdateContainer_Engine.ENGINE_NAME, contentContainer, false, false);
        }
        if (focusedFieldId > 0 && url != null && url.length() > 0) {
            url = new StringBuffer(url.length() + 16).append(url).append(
                    "&fid=").append(focusedFieldId).toString();
        }
        return url;
    }

    /**
     * Get the url to call the restore action for a marked for delete container.
     *
     * @param jParams the processing context
     * @param contentContainer the target container list
     * @return the link to open
     * @throws JahiaException sthg bad happened
     */
    public static String drawContainerRestoreUrl(final ProcessingContext jParams, final ContentContainer contentContainer) throws JahiaException {
        if (contentContainer == null || !contentContainer.isMarkedForDelete()) {
            return null ;
        }
        return drawUrlCheckWriteAccess(jParams, RestoreLiveContainer_Engine.ENGINE_NAME, contentContainer, false, false);
    }

    /**
     * Get the url to call delete container action.
     *
     * @param jParams the processing context
     * @param contentContainer the target container list
     * @return the link to open
     * @throws JahiaException sthg bad happened
     */
    public static String drawContainerDeleteUrl(ProcessingContext jParams, final ContentContainer contentContainer) throws JahiaException {
        if (ServicesRegistry.getInstance().getJahiaACLManagerService().
                getSiteActionPermission("engines.actions.delete",
                        jParams.getUser(), JahiaBaseACL.READ_RIGHTS,
                        jParams.getSiteID()) > 0) {
            /** todo we removed this version of the action check because it was dreadfully
             * slow to check the rights on the whole sub tree, which could be very large.
             * We might want to do this check when opening the engine instead, or use AJAX
             * to indicate the background process is running.
             */
            // return drawUrlCheckWriteAccess( "deletecontainer", contentContainer,true,true);
            if (canDeleteContainerInContainerList(((ContentContainerList) contentContainer
                    .getParent(jParams.getEntryLoadRequest()))
                    .getJahiaContainerList(jParams, jParams
                            .getEntryLoadRequest()))) {
                return drawUrlCheckWriteAccess(jParams,
                        DeleteContainer_Engine.ENGINE_NAME, contentContainer,
                        false, false);
            } else {
                logger.debug("no rights for adding container to this specific container list");
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get the url to open the picked object if the current container is a picker.
     *
     * @param processingContext the processing context
     * @param contentContainer the target container list
     * @return the link to open
     * @throws JahiaException sthg bad happened
     */
    public static String drawContainerPickedUrl(ProcessingContext processingContext, final ContentContainer contentContainer) throws JahiaException {
        if (!ServicesRegistry.getInstance().getImportExportService().isPicker(contentContainer)) {
            return null ;
        }
        ContentObject pickedObject; // the source of the linked copy (the picked)
        ContentContainer pickedContainer;
        String pickedpageID;
        int pickedSiteID;
        boolean pagefound = false;
        try {
            pickedObject = contentContainer.getPickedObject();
            pickedSiteID = pickedObject.getSiteID();
            pickedContainer = ContentContainer.getContainer(pickedObject.getID());
            if (pickedContainer.getJahiaContainer(processingContext, processingContext.getEntryLoadRequest()) != null) {
                pickedpageID = String.valueOf(pickedContainer.getPageID());
                if (pickedpageID != null) {
                    logger.debug("pageID:" + pickedpageID);
                    pagefound = true;
                    if (pickedSiteID != contentContainer.getSiteID()) {
                        logger.debug("cross-site");
                    }
                }
                final List<ContentObject> children = pickedContainer.getChilds(null, EntryLoadRequest.STAGED);
                for (final ContentObject container : children) {
                    if (!(container instanceof ContentPageField)) continue;
                    //defensive code relative to poor impl of exception catching/throwing of method getPage below
                    final JahiaPage page = ((ContentPageField) container).getPage(processingContext, EntryLoadRequest.STAGED);

                    if (page != null) {
                        if (page.getPageType() == JahiaPage.TYPE_DIRECT) {
                            logger.debug(page.toString());
                            pickedpageID = String.valueOf(page.getID());
                            logger.debug("found contentpagefield:" + pickedpageID);
                            pagefound = true;
                            break;
                        }
                    }
                }
                if (pagefound) {
                    int pickedPageID ;
                    try {
                        pickedPageID = Integer.parseInt(pickedpageID);
                    } catch (NumberFormatException nfe) {
                        pickedPageID = 0;
                    }
                    return processingContext.composePageUrl(pickedPageID) ;
                }
            }
        } catch (JahiaException e) {
            logger.error(e);
        }
        return null ;
    }

    /**
     * Get the url to display the picker list.
     *
     * @param processingContext the processing context
     * @param contentContainer the target container list
     * @return a map of page titles /  links
     * @throws JahiaException sthg bad happened
     */
    public static Map<String, String> drawContainerPickerListUrl(ProcessingContext processingContext, final ContentContainer contentContainer) throws JahiaException {
        Set<ContentObject> pickers = contentContainer.getPickerObjects() ;
        if (pickers.size() > 0) {
            Map<String, String> pickersMap = new HashMap<String, String>() ;
            for (ContentObject co: pickers) {
                logger.debug("picker: " + co.getObjectKey().getKey()) ;
                ContentPage page = ContentPage.getPage(co.getPageID()) ;
                pickersMap.put(page.getTitle(processingContext), page.getUrl(processingContext)) ;
            }
            return pickersMap ;
        } else {
            return null ;
        }
    }

    /**
     * Get the url to the source page where the container has been declared in case it is not already available.
     * (only when the container reference is absolute)
     *
     * @param processingContext the processing context
     * @param theContainer the target container
     * @return the link to the source page
     * @throws JahiaException sthg bad happened
     */
    public static String drawContainerSourcePageReferenceUrl(ProcessingContext processingContext, final ContentContainer theContainer) throws JahiaException {
        if (theContainer.getPageID() != processingContext.getPageID() && (!(ServicesRegistry.getInstance().getWorkflowService().getWorkflowMode(theContainer) != WorkflowService.LINKED) || !(org.jahia.settings.SettingsBean.getInstance().isDevelopmentMode() || processingContext.getSessionState().getAttribute(userSettings.WF_VISU_ENABLED) != null || org.jahia.settings.SettingsBean.getInstance().isWflowDisp()))) {
            return processingContext.composePageUrl(theContainer.getPageID(), processingContext.getLocale().toString());
        } else {
            return null ;
        }
    }


    // ================================================================
    //    PAGE METHODS
    // ================================================================

    /**
     * Draw link to open page properties engine.
     *
     * @param jParams the processing context
     * @param pageID page id
     * @return the engine url
     * @throws JahiaException sthg bad happened
     */
    public static String drawPageUpdateUrl(ProcessingContext jParams, final int pageID) throws JahiaException {
        final int oldPageID = jParams.getPageID();
        if (oldPageID != pageID) jParams.changePage(pageID);
        final JahiaACLManagerService aclService = ServicesRegistry.getInstance().getJahiaACLManagerService();
        final String result;
        if (aclService.getSiteActionPermission("engines.actions.update",
                jParams.getUser(), JahiaBaseACL.READ_RIGHTS,
                jParams.getSiteID()) > 0 &&
                aclService.getSiteActionPermission("engines.languages." + jParams.getLocale().toString(),
                        jParams.getUser(),
                        JahiaBaseACL.READ_RIGHTS,
                        jParams.getSiteID()) > 0) {
            result = drawUrlCheckWriteAccess(jParams, PageProperties_Engine.ENGINE_NAME, jParams.getPage(), false, false);
        } else {
            result = null ;
        }
        if (oldPageID != pageID) {
            jParams.changePage(oldPageID);
        }
        return result;
    }





    // ================================================================
    //    COMMON METHODS
    // ================================================================

    /**
     * Generic method to compose URLs to reach engines without access check.
     *
     * @param jParams processing context
     * @param engineName the name of the engine to target
     * @param theObj a jahia object
     * @return the url to the given engine
     * @throws JahiaException sthg bad happened
     */
    public static String drawUrl(final ProcessingContext jParams, final String engineName, final Object theObj) throws JahiaException {
        String htmlResult = null ;
        final JahiaEngine theEngine = (JahiaEngine) EnginesRegistry.getInstance().getEngine(engineName);
        if (theEngine.authoriseRender(jParams)) {
            htmlResult = theEngine.renderLink(jParams, theObj);
        }
        return htmlResult;
    }

    /**
     * Generic method to compose URLs to reach engines with write access check.
     *
     * @param jParams processing context
     * @param engineName the name of the engine to target
     * @param anObject a jahia object
     * @param checkChilds check children
     * @param forceChilds force children
     * @return the url to the given engine
     * @throws JahiaException sthg bad happened
     */
    public static String drawUrlCheckWriteAccess(final ProcessingContext jParams, final String engineName, final Object anObject, boolean checkChilds, boolean forceChilds) throws JahiaException {
        final JahiaEngine theEngine = (JahiaEngine) EnginesRegistry.getInstance().getEngine(engineName);
        if (theEngine.authoriseRender(jParams)) {
            if (anObject instanceof ContentObject) {
                final ContentObject contentObject = (ContentObject) anObject;
                final ContentObject parent = contentObject.getParent(null);
                if (parent != null && ServicesRegistry.getInstance().getImportExportService().isPicker(parent)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("There is a picker parent for " + contentObject.getObjectKey().getKey()) ;
                    }
                    return null;
                }
                if (contentObject.checkWriteAccess(jParams.getUser(), checkChilds, forceChilds)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("returning URL for " + contentObject.getObjectKey().getKey()) ;
                    }
                    return theEngine.renderLink(jParams, anObject);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No write access to object " + contentObject.getObjectKey().getKey()) ;
                    }
                }
            } else if (anObject instanceof ACLResourceInterface) {
                if (anObject instanceof JahiaField) {
                    if (ServicesRegistry.getInstance().getImportExportService().isPicker(((JahiaField) anObject).getContentField())) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("This is a picker") ;
                        }
                        return null;
                    }
                }
                if (ACLResource.checkWriteAccess(null, (ACLResourceInterface) anObject, jParams.getUser())) {
                    if (anObject instanceof JahiaPage) {
                        if (ServicesRegistry.getInstance().getImportExportService().isPicker(((JahiaPage) anObject).getContentPage())) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("This is a picker") ;
                            }
                            return null;
                        }
                    }
                    return theEngine.renderLink(jParams, anObject);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No write access to object " + anObject.toString()) ;
                    }
                }
            } else if (anObject instanceof JahiaContainerList) {
                // this is mostly used to generate the add container URL when
                // a container list doesn't yet exist (created at the same time
                // as the first container).
                final JahiaContainerList containerList = (JahiaContainerList) anObject;
                if (containerList.getID() == 0) {
                    if (ServicesRegistry.getInstance().getImportExportService().isPicker(jParams.getPage().getContentPage())) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("This is a picker") ;
                        }
                        return null;
                    }
                } else {
                    if (ServicesRegistry.getInstance().getImportExportService().isPicker(containerList.getContentContainerList())) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("This is a picker") ;
                        }
                        return null;
                    }
                }
                if (engineName.equals(ContainerListProperties_Engine.ENGINE_NAME)) {
                    if (containerList.getID() == 0 && jParams.getPage().checkAdminAccess(jParams.getUser()) || containerList.getID() != 0 && containerList.checkWriteAccess(jParams.getUser())) {
                        return theEngine.renderLink(jParams, anObject);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("No write access to object " + anObject.toString()) ;
                        }
                    }
                } else {
                    // Add Container
                    if (containerList.getID() == 0 && jParams.getPage().checkWriteAccess(jParams.getUser(), true) || containerList.getID() != 0 && containerList.checkWriteAccess(jParams.getUser())) {
                        return theEngine.renderLink(jParams, anObject);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("No write access to object " + anObject.toString()) ;
                        }
                    }
                }
            } else if (anObject instanceof LockKey) {
                // this is used to create steal lock engine url
                final LockKey key = (LockKey) anObject ;
                return theEngine.renderLink(jParams, key) ;
            }
        } else {
            logger.debug("render unauthorized") ;
        }
        return null;
    }


}
