/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.idea.maven.compiler

import com.intellij.compiler.CompilerConfiguration
import com.intellij.compiler.CompilerConfigurationImpl
import org.jetbrains.idea.maven.MavenImportingTestCase
import org.jetbrains.idea.maven.importing.MavenModuleImporter
/**
 * @author Sergey Evdokimov
 */
@SuppressWarnings("GroovyPointlessBoolean")
class AnnotationProcessorImportingTest extends MavenImportingTestCase {

  public void testSettingTargetLevel() throws Exception {
    createModulePom("module1", """
<groupId>test</groupId>
<artifactId>module1</artifactId>
<version>1</version>
""")

    createModulePom("module2", """
<groupId>test</groupId>
<artifactId>module2</artifactId>
<version>1</version>

  <build>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <annotationProcessors>
            <annotationProcessor>com.test.SourceCodeGeneratingAnnotationProcessor2</annotationProcessor>
          </annotationProcessors>
        </configuration>
      </plugin>
    </plugins>
  </build>
""")

    createModulePom("module3", """
<groupId>test</groupId>
<artifactId>module3</artifactId>
<version>1</version>

  <build>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
           <proc>none</proc>
        </configuration>
      </plugin>
    </plugins>
  </build>
""")

    createModulePom("module4", """
<groupId>test</groupId>
<artifactId>module4</artifactId>
<version>1</version>

  <build>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <annotationProcessors>
          </annotationProcessors>
        </configuration>
      </plugin>
    </plugins>
  </build>
""")

    importProject """
<groupId>test</groupId>
<artifactId>project</artifactId>
<version>1</version>
<packaging>pom</packaging>

<modules>
  <module>module1</module>
  <module>module2</module>
  <module>module3</module>
  <module>module4</module>
</modules>

""";

    def compilerConfiguration = ((CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject))

    assert compilerConfiguration.findModuleProcessorProfile(MavenModuleImporter.MAVEN_DEFAULT_ANNOTATION_PROFILE).getModuleNames() == new HashSet<String>(["module1"])
    assert compilerConfiguration.findModuleProcessorProfile(MavenModuleImporter.PROFILE_PREFIX + 'module2').getProcessors() == new HashSet<String>(["com.test.SourceCodeGeneratingAnnotationProcessor2"])
    assert compilerConfiguration.findModuleProcessorProfile(MavenModuleImporter.PROFILE_PREFIX + 'module3') == null
  }

  public void testOverrideGeneratedOutputDir() {
    importProject """
<groupId>test</groupId>
<artifactId>project</artifactId>
<version>1</version>

<build>
  <plugins>
    <plugin>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <generatedSourcesDirectory>out/generated</generatedSourcesDirectory>

      </configuration>
    </plugin>
  </plugins>
</build>
""";

    def compilerConfiguration = ((CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject))

    assert compilerConfiguration.findModuleProcessorProfile(MavenModuleImporter.MAVEN_DEFAULT_ANNOTATION_PROFILE) == null
    assert compilerConfiguration.findModuleProcessorProfile(MavenModuleImporter.PROFILE_PREFIX + "project").getGeneratedSourcesDirectoryName(false).replace('\\', '/').endsWith("out/generated")

    assert compilerConfiguration.excludedEntriesConfiguration.excludeEntryDescriptions.find {it.url.endsWith("out/generated")} != null
  }

  public void testImportAnnotationProcessorOptions() {
    importProject """
<groupId>test</groupId>
<artifactId>project</artifactId>
<version>1</version>

<build>
  <plugins>
    <plugin>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <compilerArgument>-Aopt1=111 -Xmx512Mb -Aopt2=222</compilerArgument>
        <compilerArguments>
          <Aopt3>333</Aopt3>
          <opt>666</opt>
        </compilerArguments>
      </configuration>
    </plugin>
  </plugins>
</build>
""";

    def compilerConfiguration = ((CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject))

    assert compilerConfiguration.findModuleProcessorProfile(MavenModuleImporter.MAVEN_DEFAULT_ANNOTATION_PROFILE) == null
    assert compilerConfiguration.findModuleProcessorProfile(MavenModuleImporter.PROFILE_PREFIX + "project").getProcessorOptions() == ['opt1': '111', 'opt2': '222', 'opt3': '333']
  }

}
