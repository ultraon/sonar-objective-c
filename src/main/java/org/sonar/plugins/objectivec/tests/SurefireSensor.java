/*
 * Sonar Objective-C Plugin
 * Copyright (C) 2012 OCTO Technology, Backelite
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.objectivec.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.objectivec.core.ObjectiveC;

import java.io.File;

public class SurefireSensor implements Sensor {
    private static final Logger LOG = LoggerFactory.getLogger(SurefireSensor.class);
    private static final String REPORT_PATH_KEY = "sonar.junit.reportsPath";
    private static final String DEFAULT_REPORT_PATH = "sonar-reports/";

    private final Settings settings;
    private final FileSystem fileSystem;

    public SurefireSensor(final FileSystem fileSystem, final Settings config) {
        this.settings = config;
        this.fileSystem = fileSystem;
    }

    @SuppressWarnings("unused")
    @DependsUpon
    public Class<?> dependsUponCoverageSensors() {
        return CoverageExtension.class;
    }

    public boolean shouldExecuteOnProject(Project project) {

        return project.isRoot() && fileSystem.hasFiles(fileSystem.predicates().hasLanguage(ObjectiveC.KEY));
    }

    public void analyse(Project project, SensorContext context) {
    /*
        GitHub Issue #50
        Formerly we used SurefireUtils.getReportsDirectory(project). It seems that is this one:
        http://grepcode.com/file/repo1.maven.org/maven2/org.codehaus.sonar.plugins/sonar-surefire-plugin/3.3.2/org/sonar/plugins/surefire/api/SurefireUtils.java?av=f#34
        However it turns out that the Java plugin contains its own version of SurefireUtils
        that is very different (and does not contain a matching method).
        That seems to be this one: http://svn.codehaus.org/sonar-plugins/tags/sonar-groovy-plugin-0.5/src/main/java/org/sonar/plugins/groovy/surefire/SurefireSensor.java

        The result is as follows:

        1.  At runtime getReportsDirectory(project) fails if you have the Java plugin installed
        2.  At build time the new getReportsDirectory(project,settings) because I guess something in the build chain doesn't know about the Java plugin version

        So the implementation here reaches into the project properties and pulls the path out by itself.
     */

        final File reportsDir = new File(reportPath());
        LOG.debug("reportsDir: {}", reportsDir.getAbsolutePath());

        collect(project, context, reportsDir);
    }

    private void collect(Project project, final SensorContext context, File reportsDir) {
        LOG.info("parsing {}", reportsDir.getAbsolutePath());
        new AbstractSurefireParser() {
            @Override
            protected Resource getUnitTestResource(String classKey) {
                return getUnitTestResource(classKey, context);
            }

            private Resource getUnitTestResource(String classname, SensorContext context) {
                String fileName = classname.replace('.', '/') + ".m";
                LOG.debug("Getting input file for classname {}, calculated file {}", classname, fileName);
                InputFile inputFile = fileSystem.inputFile(fileSystem.predicates().or(fileSystem.predicates().matchesPathPattern("**/" + fileName),
                        fileSystem.predicates().matchesPathPattern("**/" + fileName.replace("_", "+"))));
                LOG.debug("Result input file {}", inputFile);
                if (inputFile == null) {
                    return null;
                }
                Resource resource = context.getResource(inputFile);
                if(resource instanceof org.sonar.api.resources.File) {
                    org.sonar.api.resources.File sonarFile = (org.sonar.api.resources.File) resource;
                    sonarFile.setQualifier(Qualifiers.UNIT_TEST_FILE);
                }
                LOG.debug("Calculated resource for input file: {}", resource);
                return resource;
            }
        }.collect(project, context, reportsDir);
    }

    @Override
    public String toString() {
        return "Objective-C SurefireSensor";
    }

    private String reportPath() {
        String reportPath = settings.getString(REPORT_PATH_KEY);
        if (reportPath == null) {
            reportPath = DEFAULT_REPORT_PATH;
        }
        return reportPath;
    }
}
