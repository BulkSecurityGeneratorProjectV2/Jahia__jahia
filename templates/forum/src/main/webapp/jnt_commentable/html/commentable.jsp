<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="acl" type="java.lang.String"--%>
<template:addResources type="css" resources="forum.css"/>
<c:set var="bindedComponent" value="${currentNode.properties['j:bindedComponent'].node}"/>
<c:if test="${not empty bindedComponent}">
    <c:choose>
        <c:when test="${jcr:isNodeType(bindedComponent, 'jnt:mainResourceDisplay')}">
            <c:set var="bindedComponent" value="${renderContext.mainResource.node}"/>
        </c:when>
        <c:otherwise>
            <c:set var="bindedComponent" value="${bindedComponent}"/>
        </c:otherwise>
    </c:choose>
    <jcr:node var="comments" path="${bindedComponent.path}/comments"/>
    <c:if test="${not empty comments}">
    <div class="boxdocspace">
        <div class="boxdocspacepadding10 boxdocspacemarginbottom16">
            <div class="boxdocspace-inner">
                <div class="boxdocspace-inner-border">
                    <template:module node="${comments}" forcedTemplate="comments"/>
                </div>
            </div>
        </div>
    </div>
    <!--stop boxdocspace -->
    </c:if>
</c:if>
<template:linker path="*"/>