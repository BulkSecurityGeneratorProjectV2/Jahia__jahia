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
package org.jahia.taglibs.jcr.xpath;

import org.apache.log4j.Logger;
import org.apache.taglibs.standard.tag.common.core.Util;
import org.jahia.params.ProcessingContext;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.NodeIteratorImpl;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.taglibs.AbstractJahiaTag;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.security.Principal;
import java.util.Collections;

/**
 * Tag implementation for exposing a result of XPath JCR query into the template scope.
 */
public class JCRXPathTag extends AbstractJahiaTag {
    private static final Logger logger = Logger.getLogger(JCRXPathTag.class);
    private int scope = PageContext.PAGE_SCOPE;
    private String var;
    private String xpath;

    public int doEndTag() {
        resetState();
        return EVAL_PAGE;
    }

    public int doStartTag() throws JspException {
        try {
            final ProcessingContext ctx = getProcessingContext();
            if (ctx != null) {
                pageContext.setAttribute(var, findNodeIteratorByXpath(ctx.getUser(), xpath), scope);
            } else {
                logger.error("ProcessingContext instance is null.");
            }
            return EVAL_BODY_INCLUDE;

        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
        return SKIP_BODY;
    }

    /**
     * Find Node iterator by principal and XPath expression.
     * 
     * @param p
     *            the principal
     * @param path
     *            an Xpath expression to perform the JCR query
     * @return the {@link NodeIterator} instance with the results of the query;
     *         returns empty iterator if nothing is found
     */
    private NodeIterator findNodeIteratorByXpath(Principal p, String path) {
        NodeIterator ni = null;
        if (logger.isDebugEnabled()) {
            logger.debug("Find node by xpath[ " + path + " ]");
        }
        if (p instanceof JahiaGroup) {
            logger.warn("method not implemented for JahiaGroup");
        } else {
            try {
                QueryManager queryManager = ServicesRegistry.getInstance().getJCRStoreService().getQueryManager((JahiaUser) p);
                if (queryManager != null) {
                    Query q = queryManager.createQuery(path, Query.XPATH);
                    // execute query
                    QueryResult queryResult = q.execute();
    
                    // get node iterator
                    ni = queryResult.getNodes();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Path[" + path + "] --> found [" + ni.getSize() + "] values.");
                    }
                }
            } catch (javax.jcr.PathNotFoundException e) {
                logger.debug("javax.jcr.PathNotFoundException: Path[" + path + "]");
            } catch (javax.jcr.ItemNotFoundException e) {
                logger.debug(e, e);
            } catch (javax.jcr.query.InvalidQueryException e) {
                logger.error("InvalidQueryException ---> [" + path + "] is not valid.", e);
            }
            catch (RepositoryException e) {
                logger.error(e, e);
            }
        }
        return ni != null ? ni : new NodeIteratorImpl(Collections.EMPTY_LIST.iterator(), 0);
    }

    public int getScope() {
        return scope;
    }

    public String getVar() {
        return var;
    }

    public String getXpath() {
        return xpath;
    }

    @Override
    protected void resetState() {
        super.resetState();
        scope = PageContext.PAGE_SCOPE;
        xpath = null;
        var = null;
    }

    public void setScope(String scope) {
        this.scope = Util.getScope(scope);
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

}
