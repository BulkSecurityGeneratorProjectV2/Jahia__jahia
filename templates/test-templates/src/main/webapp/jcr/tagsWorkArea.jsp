<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<utility:useConstants var="jcr" className="org.jahia.api.Constants" scope="application"/>
<jcr:node var="jcrnode" path="/content/shared/files"/>
<h3>Access to node attributes directly :</h3>
<ul>
    <li>Node: ${jcrnode.name}</li>
    <li>URL: ${jcrnode.url}</li>
    <li>Date: ${jcrnode.lastModifiedAsDate}</li>
    <li>File: ${jcrnode.file}</li>
    <li>Collection: ${jcrnode.collection}</li>
</ul>

<h3>Access to node definition and node type directly :</h3>
<ul>
    <li>Definition - name: ${jcrnode.definition.name}</li>
    <li>Definition - declaring nodeType: ${jcrnode.definition.declaringNodeType.name}</li>
    <c:forEach items="${jcrnode.definition.requiredPrimaryTypes}" var="child">
        <li> required primary types: ${child.name}</li>
    </c:forEach>
</ul>

<h3>Access to specific property</h3>
<jcr:nodeProperty node="${jcrnode}" name="jcr:created" var="createdDate" varDef="createdDef">
    <ul>
        <li>Creation Date : <fmt:formatDate value="${createdDate.date.time}" dateStyle="full"/></li>
        <li>Is Property Multi Valued : ${createdDef.multiple}</li>
    </ul>
</jcr:nodeProperty>

<h3>Access to childs of a node</h3>
<c:forEach items="${jcrnode.children}" var="child">
    <ul>
        <li>Node: ${child.name}</li>
        <li>URL: ${child.url}</li>
        <li>Date: ${child.lastModifiedAsDate}</li>
        <li>File: ${child.file}</li>
        <c:if test="${child.file}">
            <li>Download: <jcr:link path="${child.path}">link</jcr:link> or <jcr:link path="${child.path}"
                                                                                      absolute="true">absolute link</jcr:link></li>
        </c:if>
        <jcr:nodeProperty node="${child}" name="j:defaultCategory" var="cat" varDef="catDef">
            <li>Access to categories as multivalued string :
                <c:if test="${catDef.multiple}">
                    <ul>
                        <c:forEach items="${cat}" var="category">
                            <li>${category.string}</li>
                        </c:forEach>
                    </ul>
                </c:if>
            </li>
            <li>Access to categories as org.jahia.data.beans.CategoryBean :
                <ul>
                    <c:forEach items="${cat}" var="category">
                        <li>${category.category.title}</li>
                    </c:forEach>
                </ul>
            </li>
        </jcr:nodeProperty>
    </ul>
</c:forEach>
<h3>Executing an XPath expression [//element(*, nt:query)] for retrieving all saved search:</h3>
<jcr:xpath var="savedSearchIterator" xpath="//element(*, nt:query)"/>
<c:if test="${savedSearchIterator.size == 0}">
    No saved searches found
</c:if>
<c:forEach items="${savedSearchIterator}" var="node">
    <ul>
        <li>Node: ${node.name}</li>
        <li>URL: ${node.url}</li>
        <li>Date: ${node.lastModifiedAsDate}</li>
    </ul>
</c:forEach>

<h3>Executing an XPath expression [//element(*, jnt:portlet)] for retrieving all mashups:</h3>
<jcr:xpath var="allMashupsIterator" xpath="//element(*, jnt:portlet)"/>
<c:if test="${allMashupsIterator.size == 0}">
    No mashups found
</c:if>
<c:forEach items="${allMashupsIterator}" var="node">
    <ul>
        <li>Node: ${node.name}</li>
        <li>URL: ${node.url}</li>
        <li>Date: ${node.lastModifiedAsDate}</li>
    </ul>
</c:forEach>

<h3>Usage of nodetype</h3>
<jcr:nodeType ntName="${jcr.nt_file}" var="type"/>
<ul>
    <li>Node type name : "${type.name}"</li>
    <li>Localized Node type name : "${jcr:label(type)}"</li>
</ul>
<h3>Name of the different fields</h3>
<ul>
    <c:forEach items="${type.propertyDefinitions}" var="propertyDefinition">
        <li> Current Locale Name of field : ${jcr:label(propertyDefinition)}</li>
        <li> French Locale Name of field  : ${jcr:labelForLocale(propertyDefinition,"fr")}</li>
    </c:forEach>
</ul>