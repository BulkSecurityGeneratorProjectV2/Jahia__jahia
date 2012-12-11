/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.test.services.workflow;

import org.jahia.services.SpringContextSingleton;
import org.jahia.services.workflow.jbpm.JBPMProvider;
import org.jbpm.api.Execution;
import org.jbpm.api.ExecutionService;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.RepositoryService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 * @author Tom Baeyens
 */
public class StateSequenceTest {

    static String deploymentId;
    private static RepositoryService repositoryService;
    private static ExecutionService executionService;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        JBPMProvider jBPMProvider = (JBPMProvider) SpringContextSingleton.getBean("jBPMProvider");
        repositoryService = jBPMProvider.getRepositoryService();
        executionService = jBPMProvider.getExecutionService();
        deploymentId = repositoryService.createDeployment().addResourceFromClasspath(
                "org/jahia/test/services/workflow/sequence_process.jpdl.xml").deploy();
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        repositoryService.deleteDeploymentCascade(deploymentId);

    }

    @Test
    public void testWaitStatesSequence() {
        ProcessInstance processInstance = executionService.startProcessInstanceByKey("StateSequence");
        Execution executionInA = processInstance.findActiveExecutionIn("a");
        assertNotNull(executionInA);

        processInstance = executionService.signalExecutionById(executionInA.getId());
        Execution executionInB = processInstance.findActiveExecutionIn("b");
        assertNotNull(executionInB);

        processInstance = executionService.signalExecutionById(executionInB.getId());
        Execution executionInC = processInstance.findActiveExecutionIn("c");
        assertNotNull(executionInC);
    }
}
