/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2011 Jahia Solutions Group SA. All rights reserved.
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
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.services.search.jcr;

import static org.jahia.services.content.JCRContentUtils.stringToJCRSearchExp;
import static org.jahia.services.content.JCRContentUtils.stringToQueryLiteral;

import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaException;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.categories.Category;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.search.AbstractHit;
import org.jahia.services.search.FileHit;
import org.jahia.services.search.Hit;
import org.jahia.services.search.JCRNodeHit;
import org.jahia.services.search.SearchCriteria;
import org.jahia.services.search.SearchProvider;
import org.jahia.services.search.SearchResponse;
import org.jahia.services.search.SearchServiceImpl;
import org.jahia.services.search.Suggestion;
import org.jahia.services.search.SearchCriteria.DateValue;
import org.jahia.services.search.SearchCriteria.NodeProperty;
import org.jahia.services.search.SearchCriteria.Term;
import org.jahia.services.search.SearchCriteria.Term.MatchType;
import org.jahia.services.search.SearchCriteria.Term.SearchFields;
import org.jahia.services.tags.TaggingService;
import org.jahia.utils.DateUtils;

/**
 * This is the default search provider used by Jahia and used the index created by Jahia's main
 * repository, which is based on Apache Jackrabbit. The search request is also done on mounted 
 * external repositories.
 *
 * For now the search criteria is converted to XPATH queries, which is despite of the deprecation
 * still the most stable and performance means to use search in Jackrabbit.
 *
 * For future versions we may change to either SQL-2 or directly the QueryObejctModel specified
 * in JSR-283.
 *
 * @author Benjamin Papez
 *
 */
public class JahiaJCRSearchProvider implements SearchProvider {

    private static Logger logger = LoggerFactory.getLogger(JahiaJCRSearchProvider.class);

    private TaggingService taggingService = null;

    /* (non-Javadoc)
     * @see org.jahia.services.search.SearchProvider#search(org.jahia.services.search.SearchCriteria, org.jahia.params.ProcessingContext)
     */
    public SearchResponse search(SearchCriteria criteria, RenderContext context) {
        SearchResponse response = new SearchResponse();

        List<Hit<?>> results = new LinkedList<Hit<?>>();
        Set<String> addedHits = new HashSet<String>();
        Set<String> addedNodes = new HashSet<String>();
        try {
            JCRSessionWrapper session = ServicesRegistry
                    .getInstance()
                    .getJCRStoreService()
                    .getSessionFactory()
                    .getCurrentUserSession(context.getMainResource().getWorkspace(),
                            context.getMainResource().getLocale(), context.getFallbackLocale());
            Query query = buildQuery(criteria, session);
            if (query != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Executing search query [{}]", query.getStatement());
                }
                QueryResult queryResult = query.execute();
                RowIterator it = queryResult.getRows();
                Set<String> languages = new HashSet<String>();
                if (it.hasNext()) {
                    if (!criteria.getLanguages().isEmpty()) {
                        for (String languageCode : criteria.getLanguages().getValues()) {
                            if (!StringUtils.isEmpty(languageCode)) {
                                languages.add(languageCode);
                            }
                        }
                    } else {
                        if (session.getLocale() != null) {
                            languages.add(session.getLocale().toString());
                        }
                    }
                }

                while (it.hasNext()) {
                    try {
                        Row row = it.nextRow();
                        JCRNodeWrapper node = (JCRNodeWrapper) row.getNode();
                        if (node.isNodeType(Constants.JAHIANT_TRANSLATION)
                                || Constants.JCR_CONTENT.equals(node.getName())) {
                            node = node.getParent();
                        }
                        if (addedNodes.add(node.getIdentifier()) && !isNodeToSkip(node, criteria, languages)) {
                            Hit<?> hit = buildHit(row, node, context);

                            SearchServiceImpl.executeURLModificationRules(hit, context);

                            if (addedHits.add(hit.getLink())) {
                                results.add(hit);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error resolving search hit", e);
                    }
                }
            }
        } catch (RepositoryException e) {
            if (e.getMessage() != null && e.getMessage().contains(ParseException.class.getName())) {
                logger.warn(e.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage(), e);
                }
            } else {
                logger.error("Error while trying to perform a search", e);
            }
        } catch (Exception e) {
            logger.error("Error while trying to perform a search", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Search query has {} results", results.size());
        }
        response.setResults(results);

        return response;
    }

    private boolean isNodeToSkip(JCRNodeWrapper node, SearchCriteria criteria, Set<String> languages) {
        boolean skipNode = false;
        try {
            if (!languages.isEmpty() && !isFileSearch(criteria)
                    && (node.isFile() || node.isNodeType(Constants.NT_FOLDER))) {
                skipNode = true;
                for (PropertyIterator it = node.getWeakReferences(); it.hasNext();) {
                    try {
                        JCRNodeWrapper refNode = (JCRNodeWrapper) it.nextProperty().getParent();
                        if (languages.contains(refNode.getLanguage())) {
                            skipNode = false;
                            break;
                        }
                    } catch (Exception e) {
                        logger.debug("Error while trying to check for node language", e);
                    }
                }

            }
        } catch (RepositoryException e) {
            logger.debug("Error while trying to check for node language", e);
        }
        return skipNode;
    }

    private Hit<?> buildHit(Row row, JCRNodeWrapper node, RenderContext context) throws RepositoryException {
        AbstractHit<?> searchHit = null;
        if (node.isFile() || node.isNodeType(Constants.NT_FOLDER)) {
            searchHit = new FileHit(node, context);
        } else {
            searchHit = new JCRNodeHit(node, context);
        }

        try {
            searchHit.setScore((float) (row.getScore() / 1000.));

            // this is Jackrabbit specific, so if other implementations
            // throw exceptions, we have to do a check here            
            Value excerpt = row.getValue("rep:excerpt(.)");
            if (excerpt != null) {
                searchHit.setExcerpt(excerpt.getString());
            }
        } catch (Exception e) {
            logger.warn("Search details cannot be retrieved", e);
        }
        return searchHit;
    }

    private String buildXpathQuery(SearchCriteria params, JCRSessionWrapper session) {
        String xpathQuery = null;

        StringBuilder query = new StringBuilder(256);
        String path = null;
        boolean includeChildren = false;
        if (!params.getFilePath().isEmpty()) {
            path = params.getFilePath().getValue().trim();
            includeChildren = params.getFilePath().isIncludeChildren();
        } else if (!params.getPagePath().isEmpty()) {
            path = params.getPagePath().getValue().trim();
            includeChildren = params.getPagePath().isIncludeChildren();
        }
        if (path != null) {
            String[] pathTokens = path != null ? StringEscapeUtils
                    .unescapeHtml(path).split("/") : ArrayUtils.EMPTY_STRING_ARRAY;
            String lastFolder = null;
            StringBuilder jcrPath = new StringBuilder(64);
            jcrPath.append("/jcr:root/");
            for (String folder : pathTokens) {
                if (folder.length() == 0) {
                    continue;
                }
                if (!includeChildren) {
                    if (lastFolder != null) {
                        jcrPath.append(lastFolder).append("/");
                    }
                    lastFolder = folder;
                } else {
                    jcrPath.append(folder).append("/");
                }
            }
            if (includeChildren) {
                jcrPath.append("/");
                lastFolder = "*";
            }
            query.append(ISO9075.encodePath(jcrPath.toString())).append("element(").append(
                    lastFolder).append(",").append(
                    getNodeType(params)).append(")");
        } else if (!params.getSites().isEmpty()) {
            query.append("/jcr:root/sites/");
            if ("-all-".equals(params.getSites().getValue())) {
                query.append("*");
            } else if (params.getSites().getValues().length == 1) {
                query.append(params.getSites().getValue());
            } else {
                query.append("*[");
                int i = 0;
                for (String site : params.getSites().getValues()) {
                    if (i > 0) {
                        query.append(" or ");
                    }
                    query.append("fn:name() = '");
                    query.append(site);
                    query.append("'");
                    i++;
                }
                query.append("]");
            }

            if (!isFileSearch(params)) {
                query.append("/*[@j:isHomePage='true' or fn:name() = 'files' or fn:name() = 'contents']");
            }

            query.append("//element(*,").append(getNodeType(params))
                    .append(")");
        } else {
            query.append("//element(*,").append(
                    getNodeType(params)).append(")");
        }

        query = appendConstraints(params, query, session);
        xpathQuery = query.toString();

        if (logger.isDebugEnabled()) {
            logger.debug("XPath query built: " + xpathQuery);
        }

        return xpathQuery;
    }

    private boolean isFileSearch(SearchCriteria params) {
        for (Term term : params.getTerms()) {
            if (term.getFields() != null
                    && (term.getFields().isSiteContent() || (!term.getFields().isDescription()
                    && !term.getFields().isFileContent() && !term.getFields().isFilename()
                    && !term.getFields().isKeywords() && !term.getFields().isTitle()))) {
                return false;
            }
        }
        return true;
    }

    private String getNodeType(SearchCriteria params) {
        return StringUtils.isEmpty(params.getNodeType()) ? (isFileSearch(params) ? Constants.NT_HIERARCHYNODE
                : Constants.NT_BASE)
                : params.getNodeType();
    }

    private StringBuilder appendConstraints(SearchCriteria params,
                                            StringBuilder query, JCRSessionWrapper session) {
        StringBuilder constraints = new StringBuilder(64);

        addTermConstraints(params, constraints, session);

        addDateAndAuthorConstraints(params, constraints);

        addFileTypeConstraints(params, constraints);

        addLanguageConstraints(params, constraints);

        List<NodeProperty> props = params.getPropertiesAll();
        if (!props.isEmpty()) {
            addPropertyConstraints(constraints, props);
        }

        if (constraints.length() > 0) {
            query.append("[").append(constraints).append("]");
        }

        return query;
    }

    private StringBuilder addConstraint(StringBuilder constraints,
                                        String operand, String constraint) {
        if (constraints.length() > 0) {
            constraints.append(" ").append(operand).append(" ");
        }
        return constraints.append(constraint);
    }

    private void addDateAndAuthorConstraints(SearchCriteria params,
                                             StringBuilder constraints) {

        if (params.getCreatedBy() != null && params.getCreatedBy().length() > 0) {
            addConstraint(constraints, "and", "jcr:contains(@jcr:createdBy, "
                    + stringToJCRSearchExp(params.getCreatedBy().trim()) + ")");
        }

        if (params.getLastModifiedBy() != null
                && params.getLastModifiedBy().length() > 0) {
            addConstraint(constraints, "and",
                    "jcr:contains(@jcr:lastModifiedBy, "
                            + stringToJCRSearchExp(params.getLastModifiedBy()
                            .trim()) + ")");
        }

        if (!params.getCreated().isEmpty()
                && DateValue.Type.ANYTIME != params.getCreated().getType()) {
            addDateConstraint(constraints, params.getCreated(), "@jcr:created");
        }

        if (!params.getLastModified().isEmpty()
                && DateValue.Type.ANYTIME != params.getLastModified().getType()) {
            addDateConstraint(constraints, params.getLastModified(),
                    "@jcr:lastModified");
        }

    }

    private void addDateConstraint(StringBuilder constraints,
                                   SearchCriteria.DateValue dateValue, String paramName) {
        Calendar greaterThanDate = Calendar.getInstance();
        Calendar smallerThanDate = null;

        if (DateValue.Type.TODAY == dateValue.getType()) {
            // no date adjustment needed
        } else if (DateValue.Type.LAST_WEEK == dateValue.getType()) {
            greaterThanDate.add(Calendar.DATE, -7);
        } else if (DateValue.Type.LAST_MONTH == dateValue.getType()) {
            greaterThanDate.add(Calendar.MONTH, -1);
        } else if (DateValue.Type.LAST_THREE_MONTHS == dateValue.getType()) {
            greaterThanDate.add(Calendar.MONTH, -3);
        } else if (DateValue.Type.LAST_SIX_MONTHS == dateValue.getType()) {
            greaterThanDate.add(Calendar.MONTH, -6);
        } else if (DateValue.Type.RANGE == dateValue.getType()) {
            greaterThanDate = null;
            smallerThanDate = null;
            if (dateValue.getFromAsDate() != null) {
                greaterThanDate = Calendar.getInstance();
                greaterThanDate.setTime(dateValue.getFromAsDate());
            }
            if (dateValue.getToAsDate() != null) {
                smallerThanDate = Calendar.getInstance();
                smallerThanDate.setTime(dateValue.getToAsDate());
            }
        } else {
            throw new IllegalArgumentException("Unknown date value type '"
                    + dateValue.getType() + "'");
        }

        try {
            if (greaterThanDate != null) {
                addConstraint(constraints, "and", paramName
                        + " >= xs:dateTime('"
                        + ISO8601.format(DateUtils.dayStart(greaterThanDate))
                        + "')");
            }
            if (smallerThanDate != null) {
                addConstraint(constraints, "and", paramName
                        + " <= xs:dateTime('"
                        + ISO8601.format(DateUtils.dayEnd(smallerThanDate))
                        + "')");
            }
        } catch (IllegalStateException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private void addFileTypeConstraints(SearchCriteria params,
                                        StringBuilder constraints) {

        if (StringUtils.isNotEmpty(params.getFileType())) {
            List<String> mimeTypes = JCRContentUtils.getInstance()
                    .getMimeTypes().get(params.getFileType());
            if (mimeTypes != null && !mimeTypes.isEmpty()) {
                if (mimeTypes.size() > 1) {
                    StringBuilder fileTypeConstraints = new StringBuilder(128);
                    for (String mimeType : mimeTypes) {
                        addConstraint(fileTypeConstraints, "or",
                                getMimeTypeConstraint(mimeType));
                    }
                    addConstraint(constraints, "and", fileTypeConstraints
                            .insert(0, "(").append(")").toString());
                } else {
                    addConstraint(constraints, "and",
                            getMimeTypeConstraint(mimeTypes.get(0)));
                }
            } else {
                logger.warn("Unsupported file type '" + params.getFileType()
                        + "'. See applicationcontext-basejahiaconfig.xml file"
                        + " for configured file types.");
            }
        }
    }

    private void addPropertyConstraintCategory(
            StringBuilder categoryConstraints, String name, String value,
            boolean includeChildren) {
        try {
            String categoryPath = Category.getCategoryPath(value);
            addConstraint(categoryConstraints, "or", "@" + name + "="
                    + stringToJCRSearchExp(categoryPath));
            if (includeChildren) {
                addConstraint(categoryConstraints, "or", "jcr:like(@"
                        + name
                        + ","
                        + stringToJCRSearchExp(categoryPath
                        + Category.PATH_DELIMITER + "%") + ")");
            }
        } catch (JahiaException e) {
            logger.warn("Category: " + value + " could not be retrieved", e);
        }
    }

    private void addPropertyConstraints(StringBuilder constraints,
                                        List<NodeProperty> properties) {
        for (NodeProperty property : properties) {
            if (!property.isEmpty()) {
                if (NodeProperty.Type.CATEGORY == property.getType()) {
                    StringBuilder categoryConstraints = new StringBuilder(64);
                    for (String value : property.getCategoryValue().getValues()) {
                        addPropertyConstraintCategory(categoryConstraints,
                                property.getName(), value, property
                                .getCategoryValue().isIncludeChildren());
                    }
                    addConstraint(constraints, "and", categoryConstraints
                            .insert(0, "(").append(")").toString());
                } else if (NodeProperty.Type.DATE == property.getType()) {
                    addDateConstraint(constraints, property.getDateValue(), "@"
                            + property.getName());
                } else if (NodeProperty.Type.TEXT == property.getType()) {
                    StringBuilder propertyConstraints = new StringBuilder(64);
                    for (String value : property.getValues()) {
                        if (property.isConstrained()) {
                            String matchType = "=";
                            if(property.getMatch()==MatchType.WITHOUT_WORDS) {
                                matchType = "!=";
                            }
                            addConstraint(propertyConstraints, "or", "@"
                                    + property.getName() + matchType
                                    + stringToJCRSearchExp(value));
                        } else {
                            addConstraint(propertyConstraints, "or",
                                    "jcr:contains(@"
                                            + property.getName()
                                            + ","
                                            + getSearchExpressionForMatchType(
                                            value, property.getMatch())
                                            + ")");
                        }
                    }
                    if (propertyConstraints.length() > 0) {
                        if (property.getValues().length == 1) {
                            addConstraint(constraints, "and",
                                    propertyConstraints.toString());
                        } else {
                            addConstraint(constraints, "and", "("
                                    + propertyConstraints.toString() + ")");
                        }
                    }
                } else if (NodeProperty.Type.BOOLEAN == property.getType()) {
                    // only handle 'true' case
                    if (Boolean.parseBoolean(property.getValue())) {
                        addConstraint(constraints, "and", "@"
                                + property.getName() + "='true'");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Unknown document property type '"
                                    + property.getType() + "'");
                }
            }
        }
    }

    private void addTermConstraints(SearchCriteria params,
                                    StringBuilder constraints, JCRSessionWrapper session) {

        for (Term textSearch : params.getTerms()) {

            if (!textSearch.isEmpty()) {
                String searchExpression = getSearchExpressionForMatchType(
                        textSearch.getTerm(), textSearch.getMatch());

                SearchFields searchFields = textSearch.getFields();
                StringBuilder textSearchConstraints = new StringBuilder(256);
                if (searchFields.isSiteContent() || (!searchFields.isTags() && !searchFields.isFileContent() && !searchFields.isDescription() && !searchFields.isTitle() && !searchFields.isKeywords() && !searchFields.isFilename())) {
                    addConstraint(textSearchConstraints, "or", "jcr:contains(., " + searchExpression + ")");
                }
                if (searchFields.isFileContent()) {
                    addConstraint(textSearchConstraints, "or", "jcr:contains(jcr:content, " + searchExpression + ")");
                }
                if (searchFields.isDescription()) {
                    addConstraint(textSearchConstraints, "or", "jcr:contains(@jcr:description, " + searchExpression
                            + ")");
                }
                if (searchFields.isTitle()) {
                    addConstraint(textSearchConstraints, "or", "jcr:contains(@jcr:title, " + searchExpression + ")");
                }
                if (searchFields.isKeywords()) {
                    addConstraint(textSearchConstraints, "or", "jcr:contains(@j:keywords, " + searchExpression + ")");
                }
                if (searchFields.isFilename()) {
                    addConstraint(
                            textSearchConstraints,
                            "or",
                            "jcr:contains(@j:nodename, "
                                    + (textSearch.getTerm().endsWith("*") ? searchExpression
                                    : getSearchExpressionForMatchType(textSearch.getTerm()
                                    , textSearch.getMatch(), "*")) + ")");
                }
                if (searchFields.isTags() && getTaggingService() != null
                        && (params.getSites().getValue() != null || params.getOriginSiteKey() != null)
                        && !StringUtils.containsAny(textSearch.getTerm(), "?*")) {
                    try {
                        JCRNodeWrapper tag = getTaggingService().getTag(textSearch.getTerm(),  params.getSites().getValue() != null ? params.getSites().getValue() : params.getOriginSiteKey(), session);
                        if (tag != null) {
                            addConstraint(textSearchConstraints, "or", "@" + Constants.TAGS + "="
                                    + stringToJCRSearchExp(tag.getIdentifier()));
                        }
                    } catch (RepositoryException e) {
                        logger.warn("Error resolving tag for search", e);
                    }
                }
                if (textSearchConstraints.length() > 0) {
                    addConstraint(constraints, "and", "("
                            + textSearchConstraints.toString() + ")");
                }
            }
        }
    }

    private void addLanguageConstraints(SearchCriteria params,
                                        StringBuilder constraints) {
        StringBuilder languageSearchConstraints = new StringBuilder(256);
        if (!params.getLanguages().isEmpty()) {
            for (String languageCode : params.getLanguages().getValues()) {
                if (languageCode != null && languageCode.length() != 0) {
                    addConstraint(languageSearchConstraints, "or",
                            "@jcr:language = "
                                    + stringToJCRSearchExp(languageCode.trim()));
                }
            }
        } else {
            try {
                JCRStoreService jcrService = ServicesRegistry.getInstance()
                        .getJCRStoreService();
                JCRSessionWrapper session = jcrService.getSessionFactory()
                        .getCurrentUserSession();
                if (session.getLocale() != null) {
                    addConstraint(languageSearchConstraints, "or",
                            "@jcr:language = "
                                    + stringToJCRSearchExp(session.getLocale()
                                    .toString()));
                }
            } catch (RepositoryException e) {
            }
        }
        if (languageSearchConstraints.length() > 0) {
            addConstraint(languageSearchConstraints, "or",
                    "not(@jcr:language)");
            addConstraint(constraints, "and", "(" + languageSearchConstraints
                    .toString()+ ")");
        }
    }

    private String getMimeTypeConstraint(String mimeType) {

        return mimeType.contains("*") ? "jcr:like(jcr:content/@jcr:mimeType,"
                + stringToQueryLiteral(StringUtils.replaceChars(mimeType, '*',
                '%')) + ")" : "jcr:content/@jcr:mimeType="
                + stringToQueryLiteral(mimeType);
    }

    private String getSearchExpressionForMatchType(String term,
                                                   MatchType matchType) {
        return getSearchExpressionForMatchType(term, matchType, null);
    }

    private String getSearchExpressionForMatchType(String term,
                                                   MatchType matchType, String postfix) {
        if (Term.MatchType.AS_IS != matchType) {
            term = QueryParser.escape(term.replaceAll(" AND ", " and ").replaceAll(
                    " OR ", " or ").replaceAll(" NOT ", " not "));
        }

        if (MatchType.ANY_WORD == matchType) {
            term = StringUtils.replace(cleanMultipleWhiteSpaces(term), " ",
                    " OR ");
        } else if (MatchType.EXACT_PHRASE == matchType) {
            term = "\"" + term.trim() + "\"";
        } else if (MatchType.WITHOUT_WORDS == matchType) {
            // because of the underlying Lucene limitations a star '*' (means
            // 'match all') is added to the query string
            term = "* -"
                    + StringUtils.replace(cleanMultipleWhiteSpaces(term), " ",
                    " -");
        }

        return stringToJCRSearchExp(postfix != null ? term + postfix : term);
    }

    private String cleanMultipleWhiteSpaces(String term) {
        return term.replaceAll("\\s{2,}", " ");
    }

    /* (non-Javadoc)
    * @see org.jahia.services.search.SearchProvider#suggest(java.lang.String, java.lang.String, java.util.Locale)
    */
    public Suggestion suggest(String originalQuery, String siteKey, Locale locale) {
        if (StringUtils.isBlank(originalQuery)) {
            return null;
        }

        Suggestion suggestion = null;
        JCRSessionWrapper session;
        try {
            session = ServicesRegistry.getInstance().getJCRStoreService().getSessionFactory().getCurrentUserSession(
                    null, locale);
            QueryManager qm = session.getWorkspace().getQueryManager();
            StringBuilder xpath = new StringBuilder(64);
            xpath.append("/jcr:root[rep:spellcheck(").append(stringToJCRSearchExp(originalQuery)).append(")");
            if (locale != null) {
                xpath.append(" or @jcr:language='" + locale + "'");
            }
            xpath.append("]");
            if (siteKey != null) {
                xpath.append("/sites/").append(siteKey);
            }
            xpath.append("/(rep:spellcheck())");

            Query query = qm.createQuery(xpath.toString(), Query.XPATH);
            RowIterator rows = query.execute().getRows();
            if (rows.hasNext()) {
                Row r = rows.nextRow();
                Value v = r.getValue("rep:spellcheck()");
                if (v != null) {
                    suggestion = new Suggestion(originalQuery, v.getString());
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Making spell check suggestion for '" + originalQuery + "' site '"
                            + siteKey + "' and locale '" + locale + "' using XPath query ["
                            + xpath.toString() + "]. Result suggestion: " + suggestion);
                }
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }

        return suggestion;
    }

    /**
     * Creates the {@link Query} instance by converting the provided
     * {@link SearchCriteria} bean into XPath query.
     *
     * @param criteria the search criteria to use for the query
     * @param session current JCR session
     * @return the {@link Query} instance created by converting the provided
     *         {@link SearchCriteria} bean into XPath query or <code>null</code>
     *         if the query cannot be created
     * @throws InvalidQueryException
     * @throws RepositoryException
     */
    public Query buildQuery(SearchCriteria criteria, JCRSessionWrapper session) throws InvalidQueryException,
            RepositoryException {
        Query query = null;
        String xpathQuery = buildXpathQuery(criteria, session);
        if (!StringUtils.isEmpty(xpathQuery)) {
            QueryManager qm = session.getWorkspace().getQueryManager();
            query = qm.createQuery(xpathQuery, Query.XPATH);
            if (criteria.getLimit() > 0) {
                // set maximum hit count
                query.setLimit(criteria.getLimit());
            }
            if (criteria.getOffset() > 0) {
                // set offset for pagination
                query.setOffset(criteria.getOffset());
            }
        }

        return query;
    }

    public TaggingService getTaggingService() {
        return taggingService;
    }

    public void setTaggingService(TaggingService taggingService) {
        this.taggingService = taggingService;
    }

}
