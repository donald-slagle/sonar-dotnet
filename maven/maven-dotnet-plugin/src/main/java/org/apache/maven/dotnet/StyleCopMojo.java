/*
 * Maven and Sonar plugin for .Net
 * Copyright (C) 2010 Jose Chillan and Alexandre Victoor
 * mailto: jose.chillan@codehaus.org or alexvictoor@codehaus.org
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

/*
 * Created on Apr 14, 2009
 */
package org.apache.maven.dotnet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.dotnet.commons.project.ArtifactType;
import org.apache.maven.dotnet.commons.project.VisualStudioProject;
import org.apache.maven.dotnet.commons.project.VisualStudioSolution;
import org.apache.maven.dotnet.stylecop.StyleCopGenerator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates a quality report for a .Net project or solution using StyleCop
 * 
 * @goal stylecop
 * @phase site
 * @description generates a StyleCop report on a C# project
 * 
 * @author Jose CHILLAN Apr 14, 2009
 */
public class StyleCopMojo extends AbstractDotNetBuildMojo {
  /**
   * Name of the resource folder that contains the StyleCop exe
   */
  private final static String RESOURCE_DIR = "stylecop";
  private final static String EXPORT_PATH = "stylecop-runtime";
  private final static String STYLECOP_RULE_FILE = "default-rules.stylecop";
  private final static String STYLECOP_BUILD_FILE = "stylecop-msbuild.xml";

  /**
   * Name of the folder that will contain the stylecop installation
   * 
   * @parameter expression="${stylecop.directory}"
   */
  private String styleCopRootPath;
  
  /** Stylecop root folder */
  private File styleCopRoot;

  /**
   * Name of the file that contains the StyleCop rules to use
   * 
   * @parameter expression="${stylecop.config}"
   */
  private String styleCopConfigPath;

  /**
   * Name of the generated StyleCop report
   * 
   * @parameter alias="${styleCopReportName}"
   *            default-value="stylecop-report.xml"
   */
  private String styleCopReportName;

  /**
   * Patterns for ignored files
   * 
   * @parameter alias="${ignores}"
   */
  private String[] ignores;
  
  /**
   * Root directory of the solution. Stylecop will analyse 
   * C# source files located in this directory and sub directories.
   * 
   * @parameter expression="${dotnet.source.directory}"  
   *            default-value="${project.basedir}"
   */
  private File projectRoot;


  /**
   * Launches the style cop generation for a visual studio project.
   * 
   * @param visualProject
   *          the project
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  @Override
  public void executeSolution(VisualStudioSolution solution)
      throws MojoExecutionException, MojoFailureException {
    File solutionFile = solution.getSolutionFile();
    List<File> analyzedProjects = new ArrayList<File>();
    List<VisualStudioProject> allProjects = solution.getProjects();
    
    Set<String> skippedProjectSet = new HashSet<String>();
    if (skippedProjects!=null) {
      skippedProjectSet.addAll(Arrays.asList(StringUtils.split(skippedProjects,",")));
    }
    
    for (VisualStudioProject visualStudioProject : allProjects) {
      if (visualStudioProject.isTest() || skippedProjectSet.contains(visualStudioProject.getName())) {
        continue;
      }
      File file = visualStudioProject.getProjectFile();
      if (file == null) {
        // the project is an ASP project without any csproj file
        file = visualStudioProject.getDirectory();
      }
      analyzedProjects.add(file);

    }
    launchReport(solutionFile, analyzedProjects);
  }

  /**
   * Launches the style cop generation for a visual studio project.
   * 
   * @param visualProject
   *          the project
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  @Override
  public void executeProject(VisualStudioProject visualProject)
      throws MojoExecutionException, MojoFailureException {
    // Cannot launch stylecop on a web project alone
    if (visualProject.getType() != ArtifactType.WEB) {
      File projectFile = visualProject.getProjectFile();
      launchReport(projectFile, Collections.singletonList(projectFile));
    }
  }

  /**
   * @param log
   * @param solutionFile
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  private void launchReport(File solutionFile, List<File> projectFiles)
      throws MojoExecutionException, MojoFailureException {
    Log log = getLog();
    extractExecutable();

    File reportDirectory = getReportDirectory();
    // Defines the rule file if necessary
    final File styleCopConfigFile;
    if (StringUtils.isEmpty(styleCopConfigPath)) {
      styleCopConfigFile = extractResource(reportDirectory, STYLECOP_RULE_FILE,
          STYLECOP_RULE_FILE, "stylecop rule file");
    } else {
      styleCopConfigFile = new File(styleCopConfigPath);
      if (!styleCopConfigFile.exists()) {
        throw new MojoExecutionException(
            "Could not find the stylecop project file: " + styleCopConfigFile);
      }
    }

    // Initializes the parameters
    File reportFile = getReportFile(styleCopReportName);
    File msbuildFile = getReportFile(STYLECOP_BUILD_FILE);

    // Logs what is generated
    if (log.isDebugEnabled()) {
      log.debug("StyleCop configuration :");
      log.debug(" - Solution file      : " + solutionFile);
      
      if ((projectFiles != null) && (!projectFiles.isEmpty())) {
        log.debug(" - Project files      :  " + projectFiles);
      }
      log.debug(" - Config file        : " + styleCopConfigFile);
      log.debug(" - Generated report   : " + reportFile);
      log.debug(" - Excluded files     : " + Arrays.toString(ignores));
      log.debug(" - StyleCop directory : " + styleCopRootPath);
    }

    // Generates the build file
    StyleCopGenerator generator = new StyleCopGenerator();
    generator.setOutput(reportFile);
    generator.setProjectRoot(projectRoot);
    generator.setSettings(styleCopConfigFile);
    generator.setStyleCopRoot(styleCopRoot);
    generator.setVisualSolution(solutionFile);

    // Adds the analysed visual studio projects
    if (projectFiles != null) {
      for (File projectFile : projectFiles) {
        generator.addVisualProject(projectFile);
      }
    }
    // We write into the file (and override the previous one)
    try {
      FileOutputStream msBuildStream = new FileOutputStream(msbuildFile, false);
      generator.generate(msBuildStream);
      msBuildStream.close();
    } catch (IOException exc) {
      throw new MojoExecutionException(
          "Could not generate the MSBuild file for StyleCop", exc);
    }

    log.info("StyleCop MsBuild file generated!");
    File executable = getMsBuildCommand();

    List<String> arguments = new ArrayList<String>();

    // Adds the configuration
    arguments.add("/p:AppRoot=" + toCommandPath(projectRoot));
    arguments.add("/target:CheckStyle");
    arguments.add(toCommandPath(msbuildFile));

    log.info("Launching the build of " + msbuildFile);
    log.debug(" - Tool Version  : " + toolVersion);
    log.debug(" - MsBuild exe   : " + executable);

    // We launch the compile command (the logs are put in debug because they may
    // be verbose)
    launchCommand(executable, arguments, "build", 0);
    log.info("StyleCop report generated");
  }

  /**
   * Extracts the executables for StyleCop.
   * 
   * @throws MojoExecutionException
   */
  private void extractExecutable() throws MojoExecutionException {
    if (StringUtils.isEmpty(styleCopRootPath)) {
      styleCopRoot = extractFolder(RESOURCE_DIR, EXPORT_PATH, "StyleCop");
    } else {
      styleCopRoot = new File(styleCopRootPath);
    }
  }
}
