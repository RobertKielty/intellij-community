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
package com.intellij.application.options.codeStyle.arrangement;

import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProviderImpl;
import com.intellij.application.options.codeStyle.arrangement.match.ArrangementMatchingRulesPanel;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.arrangement.Rearranger;
import com.intellij.psi.codeStyle.arrangement.StdArrangementSettings;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.settings.ArrangementColorsAware;
import com.intellij.psi.codeStyle.arrangement.settings.ArrangementStandardSettingsAware;
import com.intellij.psi.codeStyle.arrangement.settings.ArrangementStandardSettingsRepresentationAware;
import com.intellij.psi.codeStyle.arrangement.settings.DefaultArrangementSettingsRepresentationManager;
import com.intellij.util.ui.GridBag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 10/30/12 5:17 PM
 */
public abstract class ArrangementSettingsPanel extends CodeStyleAbstractPanel {

  @NotNull private final JPanel myContent = new JPanel(new GridBagLayout());

  @NotNull private final Language                         myLanguage;
  @NotNull private final ArrangementStandardSettingsAware mySettingsAware;
  @NotNull private final ArrangementMatchingRulesPanel myMatchingRulesPanel;

  public ArrangementSettingsPanel(@NotNull CodeStyleSettings settings, @NotNull Language language) {
    super(settings);
    myLanguage = language;
    Rearranger<?> rearranger = Rearranger.EXTENSION.forLanguage(language);

    assert rearranger instanceof ArrangementStandardSettingsAware;
    mySettingsAware = (ArrangementStandardSettingsAware)rearranger;

    final ArrangementColorsProvider colorsProvider;
    if (rearranger instanceof ArrangementColorsAware) {
      colorsProvider = new ArrangementColorsProviderImpl((ArrangementColorsAware)rearranger);
    }
    else {
      colorsProvider = new ArrangementColorsProviderImpl(null);
    }

    ArrangementStandardSettingsRepresentationAware representationManager = DefaultArrangementSettingsRepresentationManager.INSTANCE;
    if (mySettingsAware instanceof ArrangementStandardSettingsRepresentationAware) {
      representationManager = (ArrangementStandardSettingsRepresentationAware)mySettingsAware;
    }

    final ArrangementNodeDisplayManager displayManager = new ArrangementNodeDisplayManager(
      mySettingsAware, colorsProvider, representationManager
    );
    
    myMatchingRulesPanel = new ArrangementMatchingRulesPanel(displayManager, colorsProvider, mySettingsAware);
    myContent.add(myMatchingRulesPanel, new GridBag().fillCell().weightx(1).weighty(1));
  }

  @Nullable
  @Override
  public JComponent getPanel() {
    return myContent;
  }

  @Nullable
  @Override
  protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private StdArrangementSettings getSettings(@NotNull CodeStyleSettings settings) {
    StdArrangementSettings result = (StdArrangementSettings)settings.getCommonSettings(myLanguage).getArrangementSettings();
    if (result == null) {
      result = mySettingsAware.getDefaultSettings();
    }
    return result;
  }
  
  @Override
  public void apply(CodeStyleSettings settings) {
    // TODO den implement 
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    StdArrangementSettings s = new StdArrangementSettings(myMatchingRulesPanel.getRules());
    return false;
    // TODO den add grouping rules support
    //return !Comparing.equal(getSettings(settings), s);
  }

  @Override
  protected void resetImpl(CodeStyleSettings settings) {
    StdArrangementSettings s = getSettings(settings);
    if (s == null) {
      myMatchingRulesPanel.setRules(null);
    }
    else {
      myMatchingRulesPanel.setRules(copy(s.getRules()));
    }
  }

  @NotNull
  private static List<StdArrangementMatchRule> copy(@NotNull List<StdArrangementMatchRule> rules) {
    List<StdArrangementMatchRule> result = new ArrayList<StdArrangementMatchRule>();
    for (StdArrangementMatchRule rule : rules) {
      result.add(rule.clone());
    }
    return result;
  }

  @Override
  protected String getTabTitle() {
    return ApplicationBundle.message("arrangement.title.settings.tab");
  }
}
