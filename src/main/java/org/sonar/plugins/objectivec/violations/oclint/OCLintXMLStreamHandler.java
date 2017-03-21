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
package org.sonar.plugins.objectivec.violations.oclint;

import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.StaxParser.XmlStreamHandler;

import javax.xml.stream.XMLStreamException;
import java.io.File;

final class OCLintXMLStreamHandler implements XmlStreamHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OCLintParser.class);
    private static final int PMD_MINIMUM_PRIORITY = 5;
    private final Project project;
    private final SensorContext context;
    private final ResourcePerspectives resourcePerspectives;
    private final FileSystem fileSystem;

    public OCLintXMLStreamHandler(final Project p, final SensorContext c, final ResourcePerspectives resourcePerspectives, final FileSystem fileSystem) {
        project = p;
        context = c;
        this.resourcePerspectives = resourcePerspectives;
        this.fileSystem = fileSystem;
    }

    public void stream(final SMHierarchicCursor rootCursor) throws XMLStreamException {

        final SMInputCursor file = rootCursor.advance().childElementCursor("file");
        while (null != file.getNext()) {
            collectIssuesFor(file);
        }
    }

    private void collectIssuesFor(final SMInputCursor file) throws XMLStreamException {
        final String filePath = file.getAttrValue("name");
        LOG.debug("Collection violations for {}", filePath);
        final InputFile inputFile = findResource(filePath);
        LOG.debug("Try to collect issues for input file: {}", inputFile);
        if (fileExists(inputFile)) {
            LOG.debug("File {} was found in the project.", filePath);
            collectFileIssues(inputFile, file);
        } else {
            LOG.debug("Input File was not found: ", inputFile);
        }
    }

    private void collectFileIssues(final InputFile inputFile, final SMInputCursor file) throws XMLStreamException {

        final SMInputCursor line = file.childElementCursor("violation");

        while (null != line.getNext()) {
            recordViolation(inputFile, line);
        }
    }

    private InputFile findResource(final String filePath) {

        File file = new File(filePath);
        LOG.debug("Try to make input file for file rel: {}, abs: {}", file.getPath(), file.getAbsolutePath());
        return fileSystem.inputFile(fileSystem.predicates().hasRelativePath(file.getPath()));
    }

    private void recordViolation(InputFile inputFile, final SMInputCursor line) throws XMLStreamException {

        Issuable issuable = resourcePerspectives.as(Issuable.class, inputFile);

        LOG.debug("Try to create Issuable: {}", issuable);

        if (issuable != null) {

            Issue issue = issuable.newIssueBuilder()
                    .ruleKey(RuleKey.of(OCLintRulesDefinition.REPOSITORY_KEY, line.getAttrValue("rule")))
                    .line(Integer.valueOf(line.getAttrValue("beginline")))
                    .message(line.getElemStringValue())
                    .build();

            LOG.debug("Try to add Issue: {}", issue);

            issuable.addIssue(issue);
        }
    }

    private boolean fileExists(InputFile file) {
        return context.getResource(file) != null;
    }
}
