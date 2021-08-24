/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.core.page;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public abstract class ProjectPageBase
    extends ApplicationPageBase
    implements ProjectContext
{
    private static final long serialVersionUID = -5616613971979378668L;

    public static final String NS_PROJECT = "/p";
    public static final String PAGE_PARAM_PROJECT = "p";

    private @SpringBean ProjectService projectService;
    private IModel<Project> projectModel;

    public ProjectPageBase(final PageParameters aParameters)
    {
        super(aParameters);

        if (getProjectModel().getObject() == null) {
            getSession().error(
                    format("[%s] requires a project to be selected", getClass().getSimpleName()));
            throw new RestartResponseException(getApplication().getHomePage());
        }
    }

    protected final void requireProjectRole(User aUser, PermissionLevel... aRoles)
    {
        Project project = getProjectModel().getObject();

        Set<PermissionLevel> roles = aRoles != null ? new LinkedHashSet<>(asList(aRoles))
                : emptySet();

        // Check access to project
        if (aUser == null || !projectService.hasRole(aUser, project, aRoles)) {
            if (roles.isEmpty()) {
                getSession()
                        .error(format("To access the [%s] you need to be a member of the project",
                                getClass().getSimpleName()));
            }
            else {
                getSession().error(format("To access the [%s] you require any of these roles: [%s]",
                        getClass().getSimpleName(),
                        roles.stream().map(PermissionLevel::getId).collect(joining(", "))));
            }

            backToProjectPage();
        }
    }

    public void backToProjectPage()
    {
        Class<? extends Page> projectDashboard = WicketObjects.resolveClass(
                "de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage");

        PageParameters pageParameters = new PageParameters();
        setProjectPageParameter(pageParameters, getProject());
        throw new RestartResponseException(projectDashboard, pageParameters);
    }

    protected void setProjectModel(IModel<Project> aModel)
    {
        projectModel = aModel;
    }

    @Override
    public Project getProject()
    {
        return getProjectModel().getObject();
    }

    public IModel<Project> getProjectModel()
    {
        if (projectModel == null) {
            projectModel = LoadableDetachableModel.of(this::getProjectFromParameters);
        }

        return projectModel;
    }

    public Project getProjectFromParameters()
    {
        StringValue projectParameter = getPageParameters().get(PAGE_PARAM_PROJECT);

        if (projectParameter.isEmpty()) {
            return null;
        }

        try {
            try {
                return projectService.getProject(projectParameter.toLong());
            }
            catch (StringValueConversionException e) {
                // Ignore lookup by ID and try lookup by slug instead.
            }

            return projectService.getProjectBySlug(projectParameter.toString());
        }
        catch (NoResultException e) {
            getSession().error("Project [" + projectParameter + "] does not exist");
            throw new RestartResponseException(getApplication().getHomePage());
        }
    }

    public static void setProjectPageParameter(PageParameters aParameters, Project aProject)
    {
        if (aProject.getSlug() != null) {
            aParameters.set(PAGE_PARAM_PROJECT, aProject.getSlug());
        }
        else {
            aParameters.set(PAGE_PARAM_PROJECT, aProject.getId());
        }
    }
}
