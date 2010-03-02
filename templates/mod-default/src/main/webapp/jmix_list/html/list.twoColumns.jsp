<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<template:addResources type="css" resources="Columns.css"/>

<%@include file="../include/header.jspf" %>

<div class="columns2"><!--start 2columns -->
    <c:forEach items="${currentList}" var="subchild" begin="${begin}" end="${end}">
        <div class="column-item">
            <div class="spacer">
                <template:module node="${subchild}" template="${subNodesTemplate}" editable="${editable}">
                    <c:if test="${not empty forcedSkin}">
                        <template:param name="forcedSkin" value="${forcedSkin}"/>
                    </c:if>
                    <c:if test="${not empty renderOptions}">
                        <template:param name="renderOptions" value="${renderOptions}"/>
                    </c:if>
                </template:module>
            </div>
        </div>
    </c:forEach>
    <c:if test="${editable and renderContext.editMode}">
        <div class="column-item">
            <div class="spacer">
                <template:module path="*"/>
            </div>
        </div>
    </c:if>
    <div class="clear"></div>
</div>
<%@include file="../include/footer.jspf" %>
