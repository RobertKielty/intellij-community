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
package com.intellij.psi.codeStyle.arrangement;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.codeStyle.arrangement.group.ArrangementGroupingRule;
import com.intellij.psi.codeStyle.arrangement.group.ArrangementGroupingType;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.match.DefaultArrangementEntryMatcherSerializer;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.order.ArrangementEntryOrderType;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 7/18/12 10:37 AM
 */
public class DefaultArrangementSettingsSerializer implements ArrangementSettingsSerializer {

  public static final  ArrangementSettingsSerializer INSTANCE = new DefaultArrangementSettingsSerializer();
  private static final Logger                        LOG      =
    Logger.getInstance("#" + DefaultArrangementSettingsSerializer.class.getName());

  @NotNull @NonNls private static final String GROUPS_ELEMENT_NAME     = "groups";
  @NotNull @NonNls private static final String GROUP_ELEMENT_NAME      = "group";
  @NotNull @NonNls private static final String RULES_ELEMENT_NAME      = "rules";
  @NotNull @NonNls private static final String RULE_ELEMENT_NAME       = "rule";
  @NotNull @NonNls private static final String TYPE_ELEMENT_NAME       = "type";
  @NotNull @NonNls private static final String MATCHER_ELEMENT_NAME    = "match";
  @NotNull @NonNls private static final String ORDER_TYPE_ELEMENT_NAME = "order";

  @NotNull private final DefaultArrangementEntryMatcherSerializer myMatcherSerializer = new DefaultArrangementEntryMatcherSerializer();

  @Override
  public void serialize(ArrangementSettings s, @NotNull Element holder) {
    if (!(s instanceof StdArrangementSettings)) {
      return;
    }

    StdArrangementSettings settings = (StdArrangementSettings)s;

    List<ArrangementGroupingRule> groupings = settings.getGroupings();
    if (!groupings.isEmpty()) {
      Element groupingsElement = new Element(GROUPS_ELEMENT_NAME);
      holder.addContent(groupingsElement);
      for (ArrangementGroupingRule group : groupings) {
        Element groupElement = new Element(GROUP_ELEMENT_NAME);
        groupingsElement.addContent(groupElement);
        groupElement.addContent(new Element(TYPE_ELEMENT_NAME).setText(group.getRule().toString()));
        groupElement.addContent(new Element(ORDER_TYPE_ELEMENT_NAME).setText(group.getOrderType().toString()));
      }
    }

    List<StdArrangementMatchRule> rules = settings.getRules();
    if (!rules.isEmpty()) {
      Element rulesElement = new Element(RULES_ELEMENT_NAME);
      holder.addContent(rulesElement);
      for (StdArrangementMatchRule rule : rules) {
        rulesElement.addContent(serialize(rule));
      }
    }
  }

  @Nullable
  @Override
  public ArrangementSettings deserialize(@NotNull Element element) {
    StdArrangementSettings result = new StdArrangementSettings();
    Element groups = element.getChild(GROUPS_ELEMENT_NAME);
    if (groups != null) {
      for (Object group : groups.getChildren(GROUP_ELEMENT_NAME)) {
        Element groupElement = (Element)group;
        try {
          ArrangementGroupingType type = ArrangementGroupingType.valueOf(groupElement.getChildText(TYPE_ELEMENT_NAME));
          ArrangementEntryOrderType orderType = ArrangementEntryOrderType.valueOf(groupElement.getChildText(ORDER_TYPE_ELEMENT_NAME));
          ArrangementGroupingRule groupingRule = new ArrangementGroupingRule(type, orderType);
          result.addGrouping(groupingRule);
        }
        catch (Exception e) {
          LOG.warn(String.format("Can't deserialize grouping rule '%s'", groupElement.getText()), e);
        }
      }
    }

    Element rulesElement = element.getChild(RULES_ELEMENT_NAME);
    if (rulesElement != null) {
      for (Object o : rulesElement.getChildren(RULE_ELEMENT_NAME)) {
        Element ruleElement = (Element)o;
        Element matcherElement = ruleElement.getChild(MATCHER_ELEMENT_NAME);
        if (matcherElement == null) {
          continue;
        }

        StdArrangementEntryMatcher matcher = null;
        for (Object c : matcherElement.getChildren()) {
          matcher = myMatcherSerializer.deserialize((Element)c);
          if (matcher != null) {
            break;
          }
        }

        if (matcher == null) {
          return null;
        }

        Element orderTypeElement = element.getChild(ORDER_TYPE_ELEMENT_NAME);
        ArrangementEntryOrderType orderType = ArrangementMatchRule.DEFAULT_ORDER_TYPE;
        if (orderTypeElement != null) {
          try {
            orderType = ArrangementEntryOrderType.valueOf(orderTypeElement.getText());
          }
          catch (Exception e) {
            LOG.warn(String.format("Can't deserialize matching rule order type for '%s'", ruleElement.getText()), e);
          }
        }
        result.addRule(new StdArrangementMatchRule(matcher, orderType));
      }
    }
    
    return result;
  }

  @Nullable
  public Element serialize(@NotNull ArrangementMatchRule rule) {
    Element matcherElement = myMatcherSerializer.serialize(rule.getMatcher());
    if (matcherElement == null) {
      return null;
    }
    
    Element result = new Element(RULE_ELEMENT_NAME);
    result.addContent(new Element(MATCHER_ELEMENT_NAME).addContent(matcherElement));
    if (rule.getOrderType() != ArrangementMatchRule.DEFAULT_ORDER_TYPE) {
      result.addContent(new Element(ORDER_TYPE_ELEMENT_NAME).setText(rule.getOrderType().toString()));
    }
    return result;
  }
}
