/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2010 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.retention.RetentionPolicy;

import org.apache.jackrabbit.core.JackrabbitRepositoryStub;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.retention.RetentionPolicyImpl;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.test.RepositoryStubException;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.jcr.JCRGroupManagerProvider;
import org.jahia.services.usermanager.jcr.JCRUserManagerProvider;

public class JahiaJackrabbitRepositoryStub extends RepositoryStub {

    /**
     * The encoding of the test resources.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Repository settings.
     */
    private final Properties settings;

    public JahiaJackrabbitRepositoryStub(Properties settings) {
        super(getStaticProperties());
        // set some attributes on the sessions
        superuser.setAttribute("jackrabbit", "jackrabbit");
        readwrite.setAttribute("jackrabbit", "jackrabbit");
        readonly.setAttribute("jackrabbit", "jackrabbit");
        // Repository settings
        this.settings = getStaticProperties();
        this.settings.putAll(settings);
    }

    private static Properties getStaticProperties() {
        Properties properties = new Properties();
        try {
            InputStream stream = getResource("JackrabbitRepositoryStub.properties");
            try {
                properties.load(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            // TODO: Log warning
        }
        try {
            InputStream stream = getResource("JahiaJackrabbitRepositoryStub.properties");
            try {
                Properties jahiaProperties = new Properties();
                jahiaProperties.load(stream);
                properties.putAll(jahiaProperties);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            // TODO: Log warning
        }
        return properties;
    }

    private static InputStream getResource(String name) {
        InputStream is = JahiaJackrabbitRepositoryStub.class.getResourceAsStream(name);
        if (is == null) {
            is = JackrabbitRepositoryStub.class.getResourceAsStream(name);
        }
        return is;
    }

    @Override
    public synchronized Repository getRepository() throws RepositoryStubException {
        Repository repository = JCRSessionFactory.getInstance();
        if (repository == null) {
            throw new RepositoryStubException("Failed to start repository");
        } else {
            try {
                Session session = JCRSessionFactory.getInstance().getCurrentUserSession();
                try {
                    if (!isTestWorkspacePrepared(session)) {
                        prepareTestContent(session);
                    }
                } finally {
                }
            } catch (Exception e) {
                RepositoryStubException exception = new RepositoryStubException(
                        "Failed to start repository");
                exception.initCause(e);
                throw exception;
            } finally {
                //JCRSessionFactory.getInstance().setCurrentUser(null);
            }
        }
        return repository;
    }

    private boolean isTestWorkspacePrepared(Session session) throws RepositoryException,
            IOException {
        boolean workspacePrepared = false;

        try {
            if (session.getRootNode().getNode("testdata") != null) {
                workspacePrepared = true;
            }
        } catch (PathNotFoundException e) {
        }
        return workspacePrepared;
    }

    private void prepareTestContent(Session session) throws RepositoryException, IOException, org.jahia.services.content.nodetypes.ParseException {
        Workspace workspace = session.getWorkspace();

        NodeTypeManager manager = workspace.getNodeTypeManager();
        if (manager instanceof NodeTypeRegistry) {
            ((NodeTypeRegistry) manager).addDefinitionsFile(
                    new File(JahiaJackrabbitRepositoryStub.class.getResource("test_nodetypes.cnd")
                            .getPath()), NodeTypeRegistry.SYSTEM + "-testnodetypes", true);
        }

        JahiaUser readOnlyUser = JCRUserManagerProvider.getInstance().lookupUser(readonly.getUserID());
        if (readOnlyUser == null) {
            readOnlyUser = JCRUserManagerProvider.getInstance().createUser(readonly.getUserID(), new String(readonly.getPassword()), new Properties());
            JahiaGroup usersGroup = JCRGroupManagerProvider.getInstance().lookupGroup(JCRGroupManagerProvider.USERS_GROUPNAME);
            usersGroup.addMember(readOnlyUser);
        }
        
        Node data = getOrAddNode(session.getRootNode(), "testdata");
        addPropertyTestData(getOrAddNode(data, "property"));
        addQueryTestData(getOrAddNode(data, "query"));
        addNodeTestData(getOrAddNode(data, "node"));
        addLifecycleTestData(getOrAddNode(data, "lifecycle"));
        addExportTestData(getOrAddNode(data, "docViewTest"));

//        Node conf = getOrAddNode(session.getRootNode(), "testconf");
//        addRetentionTestData(getOrAddNode(conf, "retentionTest"));

        session.save();
    }

    private Node getOrAddNode(Node node, String name) throws RepositoryException {
        try {
            return node.getNode(name);
        } catch (PathNotFoundException e) {
            return node.addNode(name);
        }
    }

    /**
     * Creates a boolean, double, long, calendar and a path property at the given node.
     */
    private void addPropertyTestData(Node node) throws RepositoryException {
        node.setProperty("boolean", true);
        node.setProperty("double", Math.PI);
        node.setProperty("long", 90834953485278298l);
        Calendar c = Calendar.getInstance();
        c.set(2005, 6, 18, 17, 30);
        node.setProperty("calendar", c);
        ValueFactory factory = node.getSession().getValueFactory();
        node.setProperty("path", factory.createValue("/", PropertyType.PATH));
        node.setProperty("multi", new String[] { "one", "two", "three" });
    }

    /**
     * Creates a node with a RetentionPolicy
     */
    private void addRetentionTestData(Node node) throws RepositoryException {
        RetentionPolicy rp = RetentionPolicyImpl.createRetentionPolicy("testRetentionPolicy",
                node.getSession());
        node.getSession().getRetentionManager().setRetentionPolicy(node.getPath(), rp);
    }

    /**
     * Creates four nodes under the given node. Each node has a String property named "prop1" with some content set.
     */
    private void addQueryTestData(Node node) throws RepositoryException {
        while (node.hasNode("node1")) {
            node.getNode("node1").remove();
        }
        getOrAddNode(node, "node1").setProperty("prop1",
                "You can have it good, cheap, or fast. Any two.");
        getOrAddNode(node, "node1").setProperty("prop1", "foo bar");
        getOrAddNode(node, "node1").setProperty("prop1", "Hello world!");
        getOrAddNode(node, "node2").setProperty("prop1", "Apache Jackrabbit");
    }

    /**
     * Creates three nodes under the given node: one of type nt:resource and the other nodes referencing it.
     */
    private void addNodeTestData(Node node) throws RepositoryException, IOException {
        if (node.hasNode("multiReference")) {
            node.getNode("multiReference").remove();
        }
        if (node.hasNode("resReference")) {
            node.getNode("resReference").remove();
        }
        if (node.hasNode("myResource")) {
            node.getNode("myResource").remove();
        }

        Node resource = node.addNode("myResource", "nt:resource");
        // nt:resource not longer referenceable since JCR 2.0
        resource.addMixin("mix:referenceable");
        resource.setProperty("jcr:encoding", ENCODING);
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data",
                new ByteArrayInputStream("Hello w\u00F6rld.".getBytes(ENCODING)));
        resource.setProperty("jcr:lastModified", Calendar.getInstance());

        Node resReference = getOrAddNode(node, "reference");
        resReference.setProperty("ref", resource);
        // make this node itself referenceable
        resReference.addMixin("mix:referenceable");

        Node multiReference = node.addNode("multiReference");
        ValueFactory factory = node.getSession().getValueFactory();
        multiReference.setProperty("ref",
                new Value[] { factory.createValue(resource), factory.createValue(resReference) });
    }

    /**
     * Creates a lifecycle policy node and another node with a lifecycle referencing that policy.
     */
    private void addLifecycleTestData(Node node) throws RepositoryException {
        Node policy = getOrAddNode(node, "policy");
        policy.addMixin(NodeType.MIX_REFERENCEABLE);
        Node transitions = getOrAddNode(policy, "transitions");
        Node transition = getOrAddNode(transitions, "identity");
        transition.setProperty("from", "identity");
        transition.setProperty("to", "identity");

        Node lifecycle = getOrAddNode(node, "node");
        ((NodeImpl) ((JCRNodeWrapper) lifecycle).getRealNode()).assignLifecyclePolicy(
                ((JCRNodeWrapper) policy).getRealNode(), "identity");        
    }

    private void addExportTestData(Node node) throws RepositoryException, IOException {
        getOrAddNode(node, "invalidXmlName").setProperty("propName", "some text");

        // three nodes which should be serialized as xml text in docView export
        // separated with spaces
        getOrAddNode(node, "jcr:xmltext").setProperty("jcr:xmlcharacters",
                "A text without any special character.");
        getOrAddNode(node, "some-element");
        getOrAddNode(node, "jcr:xmltext").setProperty(
                "jcr:xmlcharacters",
                " The entity reference characters: <, ', ,&, >,  \" should"
                        + " be escaped in xml export. ");
        getOrAddNode(node, "some-element");
        getOrAddNode(node, "jcr:xmltext").setProperty("jcr:xmlcharacters",
                "A text without any special character.");

        Node big = getOrAddNode(node, "bigNode");
        big.setProperty("propName0", "SGVsbG8gd8O2cmxkLg==;SGVsbG8gd8O2cmxkLg==".split(";"),
                PropertyType.BINARY);
        big.setProperty("propName1", "text 1");
        big.setProperty("propName2", "multival text 1;multival text 2;multival text 3".split(";"));
        big.setProperty("propName3", "text 1");

        addExportValues(node, "propName");
        addExportValues(node, "Prop<>prop");
    }

    /**
     * create nodes with following properties binary & single binary & multival notbinary & single notbinary & multival
     */
    private void addExportValues(Node node, String name) throws RepositoryException, IOException {
        String prefix = "valid";
        if (name.indexOf('<') != -1) {
            prefix = "invalid";
        }
        node = getOrAddNode(node, prefix + "Names");

        String[] texts = new String[] { "multival text 1", "multival text 2", "multival text 3" };
        getOrAddNode(node, prefix + "MultiNoBin").setProperty(name, texts);

        Node resource = getOrAddNode(node, prefix + "MultiBin");
        resource.setProperty("jcr:encoding", ENCODING);
        resource.setProperty("jcr:mimeType", "text/plain");
        String[] values = new String[] { "SGVsbG8gd8O2cmxkLg==", "SGVsbG8gd8O2cmxkLg==" };
        resource.setProperty(name, values, PropertyType.BINARY);
        resource.setProperty("jcr:lastModified", Calendar.getInstance());

        getOrAddNode(node, prefix + "NoBin").setProperty(name, "text 1");

        resource = getOrAddNode(node, "invalidBin");
        resource.setProperty("jcr:encoding", ENCODING);
        resource.setProperty("jcr:mimeType", "text/plain");
        byte[] bytes = "Hello w\u00F6rld.".getBytes(ENCODING);
        resource.setProperty(name, new ByteArrayInputStream(bytes));
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
    }

    @Override
    public Principal getKnownPrincipal(Session session) throws RepositoryException {

        Principal knownPrincipal = null;

        if (session instanceof SessionImpl) {
            for (Principal p : ((SessionImpl) session).getSubject().getPrincipals()) {
                if (!(p instanceof Group)) {
                    knownPrincipal = p;
                }
            }
        }

        if (knownPrincipal != null) {
            return knownPrincipal;
        } else {
            throw new RepositoryException("no applicable principal found");
        }
    }

    private static Principal UNKNOWN_PRINCIPAL = new Principal() {
        public String getName() {
            return "an_unknown_user";
        }
    };

    @Override
    public Principal getUnknownPrincipal(Session session) throws RepositoryException,
            NotExecutableException {
        return UNKNOWN_PRINCIPAL;
    }
}
