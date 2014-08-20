/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.ajax.gwt.client.widget.toolbar.action;

import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Listener;
import org.jahia.ajax.gwt.client.core.BaseAsyncCallback;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaNode;
import org.jahia.ajax.gwt.client.data.publication.GWTJahiaPublicationInfo;
import org.jahia.ajax.gwt.client.messages.Messages;
import org.jahia.ajax.gwt.client.service.content.JahiaContentManagementService;
import org.jahia.ajax.gwt.client.widget.edit.mainarea.Module;

import java.util.*;

/**
 * User: toto
 * Date: Sep 25, 2009
 * Time: 6:59:01 PM
 */
public class ViewPublishStatusActionItem extends ViewStatusActionItem {

    @Override
    public void viewStatus(final List<Module> moduleList) {

        final Map<String, Module> modules = new HashMap<String, Module>();
        for (Module module : moduleList) {
            if (module.getNode() != null) {
                modules.put(module.getNode().getPath(), module);
            }
        }

        linker.loading(Messages.get("label.gettingPublicationInfo", "Getting publication information"));

        JahiaContentManagementService.App.getInstance().getNodes(new ArrayList<String>(modules.keySet()), Arrays.asList(GWTJahiaNode.PUBLICATION_INFO), new BaseAsyncCallback<List<GWTJahiaNode>>() {
            public void onSuccess(List<GWTJahiaNode> result) {
                linker.loaded();
                Listener<ComponentEvent> removeListener = createRemoveListener();

                String lastUnpublished = null;
                boolean allPublished = true;

                for (GWTJahiaNode node : result) {
                    Module module = modules.get(node.getPath());

                    GWTJahiaPublicationInfo info = node.getAggregatedPublicationInfo();
                    if (info.getStatus() != GWTJahiaPublicationInfo.PUBLISHED) {
                        allPublished = false;
                        if (lastUnpublished != null && node.getPath().startsWith(lastUnpublished)) {
                            continue;
                        }

                        final String label = GWTJahiaPublicationInfo.statusToLabel.get(info.getStatus());
                        String status = Messages.get("label.publication." + label, label);

                        if (info.isLocked()) {
                            infoLayers.addInfoLayer(module, Messages.get("label.publication.locked", "locked"), "orange", "orange", removeListener, true,
                                    "0.7");
                        }  else if (info.isDraft()) {
                            lastUnpublished = node.getPath();
                            infoLayers.addInfoLayer(module, status, "dimgray", "dimgray", removeListener, false,
                                    "0.7");
                        } else if (info.getStatus() == GWTJahiaPublicationInfo.NOT_PUBLISHED || info.getStatus() == GWTJahiaPublicationInfo.UNPUBLISHED) {
                            lastUnpublished = node.getPath();
                            infoLayers.addInfoLayer(module, status, "black", "black", removeListener, false,
                                        "0.7");
                        } else if (info.getStatus() == GWTJahiaPublicationInfo.MODIFIED) {
                            infoLayers.addInfoLayer(module, status, "red", "red", removeListener, true,
                                    "0.7");
                        } else if (info.getStatus() == GWTJahiaPublicationInfo.LIVE_MODIFIED) {
                            infoLayers.addInfoLayer(module, status, "blue", "blue", removeListener, true,
                                    "0.7");
                        } else if (info.getStatus() == GWTJahiaPublicationInfo.CONFLICT) {
                            infoLayers.addInfoLayer(module, status, "red", "red", removeListener, true,
                                    "0.7");
                        } else if (info.getStatus() == GWTJahiaPublicationInfo.MANDATORY_LANGUAGE_UNPUBLISHABLE) {
                            infoLayers.addInfoLayer(module, status, "red", "red", removeListener, true,
                                    "0.7");
                        } else if (info.getStatus() == GWTJahiaPublicationInfo.MANDATORY_LANGUAGE_VALID) {
                            infoLayers.addInfoLayer(module, status, "red", "red", removeListener, true,
                                    "0.7");
                        }
                    }
                }

                if (allPublished) {
                    infoLayers.addInfoLayer(moduleList.iterator().next(), Messages.get("everything.published", "Everything published"), "black", "white",
                            removeListener, false,
                            "0.7");
                }

            }
        });

    }

}
