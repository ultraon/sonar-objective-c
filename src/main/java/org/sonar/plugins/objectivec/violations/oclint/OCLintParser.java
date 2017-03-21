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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.StaxParser;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

final class OCLintParser {
    private static final Logger LOG = LoggerFactory.getLogger(OCLintParser.class);

    private final Project project;
    private final SensorContext context;
    private final ResourcePerspectives resourcePerspectives;
    private final FileSystem fileSystem;

    public OCLintParser(final Project p, final SensorContext c, final ResourcePerspectives resourcePerspectives, final FileSystem fileSystem) {
        project = p;
        context = c;
        this.resourcePerspectives = resourcePerspectives;
        this.fileSystem = fileSystem;
    }

    public void parseReport(final File file) {

        try {
            final InputStream reportStream = new FileInputStream(file);
            parseReport(reportStream);
            reportStream.close();
        } catch (final IOException e) {
            LOG.error("Error processing file named {}", file, e);
        }

    }

    public void parseReport(final InputStream inputStream) {

        try {
            final OCLintXMLStreamHandler streamHandler = new OCLintXMLStreamHandler(project, context,
                    resourcePerspectives, fileSystem);
            final StaxParser parser = new StaxParser(streamHandler, false);
            parser.parse(inputStream);
        } catch (final XMLStreamException e) {
            LoggerFactory.getLogger(getClass()).error(
                    "Error while parsing XML stream.", e);
        }
    }

}
