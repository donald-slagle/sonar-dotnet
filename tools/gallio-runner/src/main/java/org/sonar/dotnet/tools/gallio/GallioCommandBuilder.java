/*
 * .NET tools :: Gallio Runner
 * Copyright (C) 2010 Jose Chillan, Alexandre Victoor and SonarSource
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
package org.sonar.dotnet.tools.gallio;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.command.Command;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class used to build the command line to run Gallio.
 */
public class GallioCommandBuilder { // NOSONAR class not final to allow mocking

  private static final Logger LOG = LoggerFactory.getLogger(GallioCommandBuilder.class);

  private static final GallioRunnerType DEFAULT_GALLIO_RUNNER = GallioRunnerType.ISOLATED_PROCESS;
  private static final String PART_COVER_EXE = "PartCover.exe";
  private static final String OPEN_COVER_EXE = "OpenCover.Console.exe";
  private static final String DOT_COVER_EXE = "dotCover.exe";

  private VisualStudioSolution solution;
  // Information needed for simple Gallio execution
  private File gallioExecutable;
  private File gallioReportFile;
  private String filter;
  private File workDir;
  private GallioRunnerType gallioRunnerType;
  // Information needed for coverage execution
  private CoverageTool coverageTool;
  private File partCoverInstallDirectory;
  private File openCoverInstallDirectory;
  private File dotCoverInstallDirectory;
  private String[] coverageExcludes;
  private String attributeExcludes;
  private File absoluteBaseDirectory;
  private File coverageReportFile;

  private List<File> testAssemblies;

  private GallioCommandBuilder() {
  }

  /**
   * Constructs a {@link GallioCommandBuilder} object for the given Visual Studio solution.
   * 
   * @param solution
   *          the solution to analyse
   * @return a Gallio builder for this solution
   */
  public static GallioCommandBuilder createBuilder(VisualStudioSolution solution) {
    GallioCommandBuilder builder = new GallioCommandBuilder();
    builder.solution = solution;
    return builder;
  }

  public void setTestAssemblies(List<File> testAssemblies) {
    this.testAssemblies = testAssemblies;
  }

  /**
   * Sets the install dir for Gallio
   * 
   * @param gallioExecutable
   *          the executable
   * @return the current builder
   */
  public void setExecutable(File gallioExecutable) {
    this.gallioExecutable = gallioExecutable;
  }

  /**
   * Sets the report file to generate
   * 
   * @param reportFile
   *          the report file
   * @return the current builder
   */
  public void setReportFile(File reportFile) {
    this.gallioReportFile = reportFile;
  }

  /**
   * Sets Gallio test filter. <br/>
   * This can be used to execute only a specific test category (i.e. CategotyName:unit to consider only tests from the 'unit' category)
   * 
   * @param gallioFilter
   *          the filter for Gallio
   * @return the current builder
   */
  public void setFilter(String gallioFilter) {
    this.filter = gallioFilter;
  }

  /**
   * Set the working directory
   * 
   * @param workDir
   *          the working directory
   */
  public void setWorkDir(File workDir) {
    this.workDir = workDir;
  }

  /**
   * Sets the coverage tool to use, given its name (insensitive, for instance "ncover" or "NCover"). If none corresponding to the given name
   * is found, or if an empty string is passed, then no coverage tool will be used and no coverage report will be generated. <br>
   * <br/>
   * To know which tools are currently supported, check {@link CoverageTool}
   * 
   * @see CoverageTool
   * @param coverageToolName
   *          the name of the tool
   */
  public void setCoverageTool(String coverageToolName) {
    this.coverageTool = CoverageTool.findFromName(coverageToolName);
  }

  /**
   * Set the Gallio runner type in order to specify how the test 
   * executions will be isolated from each others.  
   * @param runner
   */
  public void setGallioRunnerType(String runner) {
    if (StringUtils.isEmpty(runner)) {
      return;
    }
    this.gallioRunnerType = GallioRunnerType.parse(runner);
  }

  /**
   * Sets PartCover installation directory.
   * 
   * @param partCoverInstallDirectory
   *          the install dir
   */
  public void setPartCoverInstallDirectory(File partCoverInstallDirectory) {
    this.partCoverInstallDirectory = partCoverInstallDirectory;
  }

  /**
   * Sets OpenCover installation directory.
   * 
   * @param openCoverInstallDirectory
   *          the install dir
   */
  public void setOpenCoverInstallDirectory(File openCoverInstallDirectory) {
    this.openCoverInstallDirectory = openCoverInstallDirectory;
  }

  /**
   * Sets dotCover installation directory.
   * 
   * @param dotCoverInstallDirectory
   *          the install dir
   */
  public void setDotCoverInstallDirectory(File dotCoverInstallDirectory) {
    this.dotCoverInstallDirectory = dotCoverInstallDirectory;
  }

  /**
   * Sets the namespaces and assemblies excluded from the code coverage, seperated by a comma. The format for an exclusion is the PartCover
   * format: "[assembly]namespace".
   * 
   * @param coverageExcludes
   *          the excludes
   */
  public void setCoverageExcludes(String[] coverageExcludes) {
    this.coverageExcludes = coverageExcludes;
  }
  
  
  /**
   * Sets the attributes that exclude methods/properties/classes
   * 
   * @param attributeExclude
   * 		excluded attributes
   */
  public void setOpenCoverAttributeExcludes(String attributeExclude) {
	  this.attributeExcludes = attributeExclude;
  }
  
  /**
   * Sets the abd parameter for Gallio
   * 
   * @param absoluteBaseDirectory
   *        project directory
   */
  public void setAbsoluteBaseDirectory(File absoluteBaseDirectory) {
	  this.absoluteBaseDirectory = absoluteBaseDirectory;
  }

  /**
   * Sets the coverage report file to generate
   * 
   * @param coverageReportFile
   *          the report file
   * @return the current builder
   */
  public void setCoverageReportFile(File coverageReportFile) {
    this.coverageReportFile = coverageReportFile;
  }

  /**
   * Transforms this command object into a Command object that can be passed to the CommandExecutor.
   * 
   * @return the Command object that represents the command to launch.
   */
  public Command toCommand() throws GallioException {
    validateGallioInfo(testAssemblies);

    Command command = createCommand();
    List<String> gallioArguments = generateGallioArguments(testAssemblies);

    if (CoverageTool.PARTCOVER.equals(coverageTool)) {
      addPartCoverArguments(command, gallioArguments);
    } else if (CoverageTool.NCOVER.equals(coverageTool)) {
      addNCoverArguments(command, gallioArguments);
    } else if (CoverageTool.OPENCOVER.equals(coverageTool)) {
      addOpenCoverArguments(command, gallioArguments);
    } else if (CoverageTool.DOTCOVER.equals(coverageTool)) {
      addDotCoverArguments(command, gallioArguments);
    } else {
      command.addArguments(gallioArguments);
    }

    return command;
  }

  protected Command createCommand() throws GallioException {
    final Command command;
    LOG.debug("- Gallio executable   : " + gallioExecutable);

    if (CoverageTool.PARTCOVER.equals(coverageTool)) {
      // In case of PartCover, the executable is not Gallio but PartCover itself
      File partCoverExecutable = new File(partCoverInstallDirectory, PART_COVER_EXE);
      validateCoverExecutable(partCoverExecutable, coverageTool);
      LOG.debug("- PartCover executable: {}", partCoverExecutable);
      command = Command.create(partCoverExecutable.getAbsolutePath());
    } else if (CoverageTool.OPENCOVER.equals(coverageTool)) {
      // samething with opencover
      File openCoverExecutable = new File(openCoverInstallDirectory, OPEN_COVER_EXE);
      validateCoverExecutable(openCoverExecutable, coverageTool);
      LOG.debug("- OpenCover executable: {}", openCoverExecutable);
      command = Command.create(openCoverExecutable.getAbsolutePath());
    } else if (CoverageTool.DOTCOVER.equals(coverageTool)) {
      // and again with dotcover
      File openCoverExecutable = new File(dotCoverInstallDirectory, DOT_COVER_EXE);
      validateCoverExecutable(openCoverExecutable, coverageTool);
      LOG.debug("- OpenCover executable: {}", openCoverExecutable);
      command = Command.create(openCoverExecutable.getAbsolutePath());
    } else {
      command = Command.create(gallioExecutable.getAbsolutePath());
    }

    command.setDirectory(workDir);

    return command;
  }

  protected List<String> generateGallioArguments(List<File> testAssemblies) {
    List<String> gallioArguments = Lists.newArrayList();

    GallioRunnerType runner = DEFAULT_GALLIO_RUNNER;
    if (coverageTool != null) {
      LOG.debug("- Coverage tool       : {}", coverageTool.getName());
      runner = coverageTool.getGallioRunner();
    }
    if (gallioRunnerType != null) {
      runner = gallioRunnerType;
    }
    LOG.debug("- Runner              : {}", runner);
    gallioArguments.add("/r:" + runner.getValue());
    
    if(absoluteBaseDirectory != null && absoluteBaseDirectory.length() > 0) {
    	LOG.debug("- Absolute base directory : {}", absoluteBaseDirectory.getAbsolutePath());
    	gallioArguments.add("/abd:" + absoluteBaseDirectory.getAbsolutePath());
    }

    File reportDirectory = gallioReportFile.getParentFile();
    LOG.debug("- Report directory    : {}", reportDirectory.getAbsolutePath());
    gallioArguments.add("/report-directory:" + reportDirectory.getAbsolutePath());

    String reportName = trimFileReportName();
    LOG.debug("- Report file         : {}", reportName);
    gallioArguments.add("/report-name-format:" + reportName);

    gallioArguments.add("/report-type:Xml");

    if (StringUtils.isNotEmpty(filter)) {
      LOG.debug("- Filter              : {}", filter);
      gallioArguments.add("/f:" + filter);
    }

    LOG.debug("- Test assemblies     :");
    for (File testAssembly : testAssemblies) {
      LOG.debug("   o {}", testAssembly);
      gallioArguments.add(testAssembly.getAbsolutePath());
    }
    return gallioArguments;
  }

  private void addPartCoverArguments(Command command, List<String> gallioArguments) {
    // DEBUG info has already been printed out for "--target"
    command.addArgument("--target");
    command.addArgument(gallioExecutable.getAbsolutePath());

    LOG.debug("- Working directory   : {}", workDir.getAbsolutePath());
    command.addArgument("--target-work-dir");
    command.addArgument(workDir.getAbsolutePath());

    // DEBUG info has already been printed out for "--target-args"
    command.addArgument("--target-args");
    command.addArgument(escapeGallioArguments(gallioArguments));

    // We add all the covered assemblies
    for (String assemblyName : listCoveredAssemblies()) {
      LOG.debug("- Partcover include   : [{}]*", assemblyName);
      command.addArgument("--include");
      command.addArgument("[" + assemblyName + "]*");
    }

    // We add all the configured exclusions
    if (coverageExcludes != null) {
      for (String exclusion : coverageExcludes) {
        LOG.debug("- Partcover exclude   : {}", exclusion.trim());
        command.addArgument("--exclude");
        command.addArgument(exclusion.trim());
      }
    }

    LOG.debug("- Coverage report     : {}", coverageReportFile.getAbsolutePath());
    command.addArgument("--output");
    command.addArgument(coverageReportFile.getAbsolutePath());
  }

  private void addOpenCoverArguments(Command command, List<String> gallioArguments) {
    command.addArgument("-register:user");
    command.addArgument("-target:" + gallioExecutable.getAbsolutePath());

    LOG.debug("- Working directory   : {}", workDir.getAbsolutePath());
    command.addArgument("-targetdir:" + workDir.getAbsolutePath());

    command.addArgument("\"-targetargs:" + escapeGallioArguments(gallioArguments) + "\"");

    final StringBuilder filterBuilder = new StringBuilder("\"-filter:");

    // We add all the covered assemblies
    for (String assemblyName : listCoveredAssemblies()) {
      LOG.debug("- Opencover include   : [{}]*", assemblyName);
      filterBuilder.append("+[" + assemblyName + "]* ");
    }

    // We add all the configured exclusions
    if (coverageExcludes != null) {
      for (String exclusion : coverageExcludes) {
        LOG.debug("- Opencover exclude   : {}", exclusion.trim());
        filterBuilder.append(exclusion.trim());
      }
    }
    filterBuilder.append("\"");
    command.addArgument(filterBuilder.toString());

    command.addArgument("-mergebyhash");
    
    if(attributeExcludes != null && attributeExcludes.trim().length() > 0) {
    	LOG.debug("- Opencover attribute exclude   : {}", attributeExcludes);
    	command.addArgument("-excludebyattribute:" + attributeExcludes);
    }

    LOG.debug("- Coverage report     : {}", coverageReportFile.getAbsolutePath());
    command.addArgument("-output:" + coverageReportFile.getAbsolutePath());
  }

  private void addDotCoverArguments(Command command, List<String> gallioArguments) {
    command.addArgument("a");
    command.addArgument("/TargetExecutable=" + gallioExecutable.getAbsolutePath());

    LOG.debug("- Working directory   : {}", workDir.getAbsolutePath());
    command.addArgument("/TargetWorkingDir=" + workDir.getAbsolutePath());

    command.addArgument("\"/TargetArguments=" + escapeGallioArguments(gallioArguments) + "\"");

    final StringBuilder filterBuilder = new StringBuilder("/Filters=");

    // We add all the covered assemblies
    for (String assemblyName : listCoveredAssemblies()) {
      LOG.debug("- dotCover include: ", assemblyName);
      filterBuilder.append("+:module=" + assemblyName + ";class=*;function=*;");
    }

    // We add all the configured exclusions
    if (coverageExcludes != null) {
      String exclusion = Joiner.on(';').join(coverageExcludes); // HACK ';' should not be a splitter in CsharpConfiguration
      LOG.debug("- dotCover exclude   : {}", exclusion);
      filterBuilder.append(exclusion);
    }

    command.addArgument(filterBuilder.toString());

    command.addArgument("/ReportType=TeamCityXML");

    LOG.debug("- Coverage report: {}", coverageReportFile.getAbsolutePath());
    command.addArgument("/Output=" + coverageReportFile.getAbsolutePath());
  }

  private void addNCoverArguments(Command command, List<String> gallioArguments) {
    command.addArguments(gallioArguments);

    LOG.debug("- Coverage report     : {}", coverageReportFile.getAbsolutePath());
    command.addArgument("/runner-property:NCoverCoverageFile=" + coverageReportFile.getAbsolutePath());

    List<String> coveredAssembliesList = listCoveredAssemblies();
    if (coverageExcludes != null) {
      for (String exclusion : coverageExcludes) {
        LOG.debug("- NCover POTENTIAL exclude   : {}", exclusion.trim());
        if (coveredAssembliesList.contains(exclusion.trim())) {
          LOG.debug("- NCover ACTUAL exclude   : {}", exclusion.trim());
          coveredAssembliesList.remove(exclusion.trim());
        }
      }
    }

    String coveredAssemblies = StringUtils.join(coveredAssembliesList.toArray(), ";");
    LOG.debug("- NCover arguments    : {}", coveredAssemblies);
    command.addArgument("/runner-property:NCoverArguments=//ias " + coveredAssemblies);
  }

  protected List<String> listCoveredAssemblies() {
    List<String> coveredAssemblyNames = new ArrayList<String>();
    for (VisualStudioProject visualProject : solution.getProjects()) {
      if (!visualProject.isTest()) {
        coveredAssemblyNames.add(visualProject.getAssemblyName());
      }
    }
    return coveredAssemblyNames;
  }

  // TODO : try to refactor this
  protected String escapeGallioArguments(List<String> gallioArguments) {
    StringBuilder targetArgsBuilder = new StringBuilder();
    boolean isFirst = true;
    for (String currentArg : gallioArguments) {
      if (isFirst) {
        isFirst = false;
      } else {
        targetArgsBuilder.append(' ');
      }
      String escapedArg = escapeQuotes(currentArg);
      targetArgsBuilder.append(escapedArg);
    }
    return targetArgsBuilder.toString();
  }

  /*
   * Escapes the quotes of a string. TODO : try to refactor this
   */
  protected String escapeQuotes(String input) {
    StringBuilder result = new StringBuilder(input.length());
    for (int idxChar = 0, len = input.length(); idxChar < len; idxChar++) {
      char currentChar = input.charAt(idxChar);
      if (currentChar == '"') {
        result.append('\\');
      } else if (idxChar == 0) {
        result.append("\\\"");
      }

      result.append(currentChar);

      if (currentChar != '"' && idxChar == len - 1) {
        result.append("\\\"");
      }
    }
    return result.toString();
  }

  protected String trimFileReportName() {
    String reportName = gallioReportFile.getName();
    if (StringUtils.endsWithIgnoreCase(reportName, ".xml")) {
      // We remove the terminal .xml that will be added by the Gallio runner
      reportName = reportName.substring(0, reportName.length() - 4);
    }
    return reportName;
  }

  protected void validateGallioInfo(Collection<File> testAssemblies) throws GallioException {
    if (gallioExecutable == null || !gallioExecutable.isFile()) {
      throw new GallioException("Gallio executable cannot be found at the following location:" + gallioExecutable);
    }
    if (gallioReportFile == null) {
      throw new GallioException("Gallio report file has not been specified.");
    }
    if (workDir == null || !workDir.isDirectory()) {
      throw new GallioException("The working directory cannot be found at the following location:" + workDir);
    }
    if (testAssemblies.isEmpty()) {
      throw new GallioException("No test assembly was found. Please check your project's Gallio plugin configuration.");
    }
  }

  protected void validateCoverExecutable(File coverExecutable, CoverageTool tool) throws GallioException {
    if (coverExecutable == null || !coverExecutable.isFile()) {
      throw new GallioException(tool.getName() + " executable cannot be found at the following location:" + coverExecutable);
    }
    if (coverageReportFile == null) {
      throw new GallioException("Gallio coverage report file has not been specified.");
    }
  }
}
