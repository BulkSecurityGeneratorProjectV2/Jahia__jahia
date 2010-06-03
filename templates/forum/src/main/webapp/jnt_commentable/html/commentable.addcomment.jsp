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
    <script type="text/javascript">
        function jahiaForumQuote(targetId, quotedText) {
            var targetArea = document.getElementById(targetId);
            if (targetArea) {
                targetArea.value = targetArea.value + '\n<blockquote>\n' + quotedText + '\n</blockquote>\n';
            }
            return false;
        }
    </script>


    <a name="threadPost"></a>

    <form action="${url.base}${bindedComponent.path}.addComment.do" method="post">
        <input type="hidden" name="nodeType" value="jnt:post"/>
        <input type="hidden" name="redirectTo" value="${url.base}${renderContext.mainResource.node.path}"/>
        <input type="hidden" name="newNodeOutputFormat" value="html"/>

        <div class="post-reply"><!--start post-reply-->
            <div class="forum-box forum-box-style2">
                <span class="forum-corners-top"><span></span></span>

                <div id="forum-Form"><!--start forum-Form-->
                    <h4 class="forum-h4-first">${bindedComponent.properties.threadSubject.string} : <fmt:message
                            key="reply"/></h4>

                    <fieldset>
                        <p class="field">
                            <input value="<c:if test="${not empty bindedComponent.children}"> Re:</c:if>${bindedComponent.properties.threadSubject.string}"
                                   type="text" size="35" id="forum_site" name="jcr:title"
                                   tabindex="1"/>
                        </p>

                        <p class="field">
                            <textarea rows="7" cols="35" id="jahia-forum-thread-${bindedComponent.identifier}"
                                      name="content"
                                      tabindex="2"></textarea>
                        </p>

                        <p class="forum_button">
                            <input type="reset" value="Reset" class="button" tabindex="3"/>

                            <input type="submit" value="Submit" class="button" tabindex="4"/>
                        </p>
                    </fieldset>
                </div>
                <!--stop forum-Form-->


                <div class="clear"></div>
                <span class="forum-corners-bottom"><span></span></span>
            </div>
        </div>
        <!--stop post-reply-->
    </form>
</c:if>
<template:linker path="*"/>