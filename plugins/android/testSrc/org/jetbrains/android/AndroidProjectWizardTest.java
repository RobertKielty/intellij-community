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
package org.jetbrains.android;

import com.intellij.ide.projectWizard.ProjectWizardTestCase;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import org.jetbrains.android.newProject.AndroidModuleBuilder;
import org.jetbrains.android.newProject.AndroidModuleWizardStep;
import org.jetbrains.android.newProject.AndroidProjectTemplatesFactory;
import org.jetbrains.android.sdk.AndroidSdkType;

import java.util.Arrays;

/**
 * @author Dmitry Avdeev
 *         Date: 11/8/12
 */
public class AndroidProjectWizardTest extends ProjectWizardTestCase {

  public void testCreateProject() throws Exception {
    createProjectFromTemplate(AndroidProjectTemplatesFactory.ANDROID, "Android Application", new Consumer<ModuleWizardStep>() {
      @Override
      public void consume(ModuleWizardStep step) {
        if (step instanceof AndroidModuleWizardStep) {
          ProjectBuilder builder = myWizard.getProjectBuilder();
          assertTrue(builder instanceof AndroidModuleBuilder);
          String name = ((AndroidModuleBuilder)builder).getName();
          assertTrue(name, StringUtil.isNotEmpty(name));
        }
      }
    });
  }

  public void testCreateLibrary() throws Exception {
    createProjectFromTemplate(AndroidProjectTemplatesFactory.ANDROID, "Android Library Module", new Consumer<ModuleWizardStep>() {
      @Override
      public void consume(ModuleWizardStep step) {
        if (step instanceof AndroidModuleWizardStep) {
          ProjectBuilder builder = myWizard.getProjectBuilder();
          assertTrue(builder instanceof AndroidModuleBuilder);
          String name = ((AndroidModuleBuilder)builder).getName();
          assertTrue(name, StringUtil.isNotEmpty(name));
        }
      }
    });
  }

  public void testCreateEmptyProject() throws Exception {
    createProjectFromTemplate(AndroidProjectTemplatesFactory.ANDROID, "Empty Android Module", null);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
        Sdk defaultJdk = jdkTable.createSdk("Android", AndroidSdkType.getInstance());
        jdkTable.addJdk(defaultJdk);
        mySdks.add(defaultJdk);

        Sdk[] jdks = jdkTable.getAllJdks();
        System.out.println(Arrays.asList(jdks));

        Project defaultProject = ProjectManager.getInstance().getDefaultProject();
        ProjectRootManager.getInstance(defaultProject).setProjectSdk(defaultJdk);
      }
    });

  }
}
