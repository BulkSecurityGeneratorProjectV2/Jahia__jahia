/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.ajax.gwt.client.widget.contentengine;

import com.extjs.gxt.ui.client.GXT;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaNodeProperty;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaNodeType;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaNode;
import org.jahia.ajax.gwt.client.data.toolbar.GWTEngineConfiguration;
import org.jahia.ajax.gwt.client.widget.Linker;
import org.jahia.ajax.gwt.client.widget.edit.EditLinker;
import org.jahia.ajax.gwt.client.widget.edit.mainarea.Hover;
import org.jahia.ajax.gwt.client.widget.edit.sidepanel.SidePanelTabItem.SidePanelLinker;

import java.util.Map;

/**
 * Engine loader component.
 * User: toto
 * Date: Jul 2, 2010
 * Time: 7:36:04 PM
 */
public class EngineLoader {
    public static final int CREATE = 1;
    public static final int EDIT = 2;

    public static void showEditEngine(final Linker linker, final GWTJahiaNode node, GWTEngineConfiguration engineConfiguration) {
        showEngine(EDIT, linker, node, null, null, null, false, false, engineConfiguration, false, null, false);
    }

    public static void showEditEngine(final Linker linker, final GWTJahiaNode node, GWTEngineConfiguration engineConfiguration, boolean skipRefreshOnSave, EditEngineJSConfig jsConfig) {
        showEngine(EDIT, linker, node, null, null, null, false, false, engineConfiguration, skipRefreshOnSave, jsConfig, false);
    }

    public static void showEditEngine(final Linker linker, final GWTJahiaNode node, boolean forceEngineWindow, GWTEngineConfiguration engineConfiguration) {
        showEngine(EDIT, linker, node, null, null, null, false, forceEngineWindow, engineConfiguration, false, null, false);
    }

    public static void showCreateEngine(final Linker linker, final GWTJahiaNode node, final GWTJahiaNodeType type,
                                        final Map<String, GWTJahiaNodeProperty> props, final String targetName,
                                        final boolean createInParentAndMoveBefore, GWTEngineConfiguration engineConfiguration) {
        showCreateEngine(linker, node, type, props, targetName, createInParentAndMoveBefore, engineConfiguration, false, false);
    }

    public static void showCreateEngine(final Linker linker, final GWTJahiaNode node, final GWTJahiaNodeType type,
                                        final Map<String, GWTJahiaNodeProperty> props, final String targetName,
                                        final boolean createInParentAndMoveBefore, GWTEngineConfiguration engineConfiguration, boolean skipRefreshOnSave, boolean systemNameReadOnly) {
        showEngine(CREATE, linker, node, type, props, targetName, createInParentAndMoveBefore, false, engineConfiguration, skipRefreshOnSave, null, systemNameReadOnly);
    }

    private static void showEngine(final int t, final Linker linker, final GWTJahiaNode node,
                                   final GWTJahiaNodeType type, final Map<String, GWTJahiaNodeProperty> props,
                                   final String targetName, final boolean createInParentAndMoveBefore,
                                   final boolean forceEngineWindow, final GWTEngineConfiguration engineConfiguration,
                                   final boolean skipRefreshOnSave, final EditEngineJSConfig jsConfig,
                                   final boolean systemNameReadOnly) {
        GWT.runAsync(new RunAsyncCallback() {

            @Override
            public void onFailure(Throwable reason) {

            }

            @Override
            public void onSuccess() {
                EngineContainer container = createContainer(linker, forceEngineWindow);

                if (t == CREATE) {
                    new CreateContentEngine(engineConfiguration == null ? linker.getConfig().getEngineConfiguration(type) : engineConfiguration, linker, node, type, props, targetName, createInParentAndMoveBefore,
                            container, skipRefreshOnSave, systemNameReadOnly);
                } else if (t == EDIT) {
                    new EditContentEngine(engineConfiguration == null ? linker.getConfig().getEngineConfiguration(node) : engineConfiguration, node, linker, container, skipRefreshOnSave, jsConfig);
                }
                container.showEngine();
                Hover.getInstance().removeAll();
            }
        });
    }

    private static EngineContainer createContainer(Linker linker, boolean forceEngineWindow) {
        EngineContainer container;
        if (!forceEngineWindow && (!(GXT.isIE) && (linker instanceof EditLinker || linker instanceof SidePanelLinker))) {
            container = new EnginePanel();
        } else {
            container = new EngineWindow();
        }
        return container;
    }

    public static EngineContainer createContainer(Linker linker) {
        return createContainer(linker, false);
    }
}
