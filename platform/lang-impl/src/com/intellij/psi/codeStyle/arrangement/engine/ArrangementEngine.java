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
package com.intellij.psi.codeStyle.arrangement.engine;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.arrangement.ArrangementEntry;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettings;
import com.intellij.psi.codeStyle.arrangement.Rearranger;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.settings.ArrangementStandardSettingsAware;
import com.intellij.util.containers.HashSet;
import com.intellij.util.containers.Stack;
import com.intellij.util.text.CharArrayUtil;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Encapsulates generic functionality of arranging file elements by the predefined rules.
 * <p/>
 * I.e. the general idea is to have a language-specific rules hidden by generic arrangement API and common arrangement
 * engine which works on top of that API and performs the arrangement.
 *
 * @author Denis Zhdanov
 * @since 7/20/12 1:56 PM
 */
public class ArrangementEngine {

  /**
   * Arranges given PSI root contents that belong to the given ranges.
   *
   * @param file    target PSI root
   * @param ranges  target ranges to use within the given root
   */
  @SuppressWarnings("MethodMayBeStatic")
  public void arrange(@NotNull PsiFile file, @NotNull Collection<TextRange> ranges) {
    final Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
    if (document == null) {
      return;
    }

    final Rearranger<?> rearranger = Rearranger.EXTENSION.forLanguage(file.getLanguage());
    if (rearranger == null) {
      return;
    }

    final CodeStyleSettings settings = CodeStyleSettingsManager.getInstance(file.getProject()).getCurrentSettings();
    ArrangementSettings arrangementSettings = settings.getCommonSettings(file.getLanguage()).getArrangementSettings();
    if (arrangementSettings == null && rearranger instanceof ArrangementStandardSettingsAware) {
      arrangementSettings = ((ArrangementStandardSettingsAware)rearranger).getDefaultSettings();
    }
    
    if (arrangementSettings == null) {
      return;
    }

    final DocumentEx documentEx;
    if (document instanceof DocumentEx && !((DocumentEx)document).isInBulkUpdate()) {
      documentEx = (DocumentEx)document;
    }
    else {
      documentEx = null;
    }

    final Context<? extends ArrangementEntry> context = Context.from(
      rearranger, document, file, ranges, arrangementSettings, settings
    );

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        if (documentEx != null) {
          documentEx.setInBulkUpdate(true);
        }
        try {
          doArrange(context);
        }
        finally {
          if (documentEx != null) {
            documentEx.setInBulkUpdate(false);
          }
        }
      }
    });
  }

  @SuppressWarnings("unchecked")
  private static <E extends ArrangementEntry> void doArrange(Context<E> context) {
    // The general idea is to process entries bottom-up where every processed group belongs to the same parent. We may not bother
    // with entries text ranges then. We use a list and a stack for achieving that than.
    //
    // Example:
    //            Entry1              Entry2
    //            /    \              /    \
    //      Entry11   Entry12    Entry21  Entry22
    //
    //    --------------------------
    //    Stage 1:
    //      list: Entry1 Entry2    <-- entries to process
    //      stack: [0, 0, 2]       <-- holds current iteration info at the following format:
    //                                 (start entry index at the auxiliary list (inclusive); current index; end index (exclusive))
    //    --------------------------
    //    Stage 2:
    //      list: Entry1 Entry2 Entry11 Entry12
    //      stack: [0, 1, 2]
    //             [2, 2, 4]
    //    --------------------------
    //    Stage 3:
    //      list: Entry1 Entry2 Entry11 Entry12
    //      stack: [0, 1, 2]
    //             [2, 3, 4]
    //    --------------------------
    //    Stage 4:
    //      list: Entry1 Entry2 Entry11 Entry12
    //      stack: [0, 1, 2]
    //             [2, 4, 4]
    //    --------------------------
    //      arrange 'Entry11 Entry12'
    //    --------------------------
    //    Stage 5:
    //      list: Entry1 Entry2 
    //      stack: [0, 1, 2]
    //    --------------------------
    //    Stage 6:
    //      list: Entry1 Entry2 Entry21 Entry22
    //      stack: [0, 2, 2]
    //             [2, 2, 4]
    //    --------------------------
    //    Stage 7:
    //      list: Entry1 Entry2 Entry21 Entry22
    //      stack: [0, 2, 2]
    //             [2, 3, 4]
    //    --------------------------
    //    Stage 8:
    //      list: Entry1 Entry2 Entry21 Entry22
    //      stack: [0, 2, 2]
    //             [2, 4, 4]
    //    --------------------------
    //      arrange 'Entry21 Entry22'
    //    --------------------------
    //    Stage 9:
    //      list: Entry1 Entry2
    //      stack: [0, 2, 2]
    //    --------------------------
    //      arrange 'Entry1 Entry2'

    List<ArrangementEntryWrapper<E>> entries = new ArrayList<ArrangementEntryWrapper<E>>();
    Stack<StackEntry> stack = new Stack<StackEntry>();
    entries.addAll(context.wrappers);
    stack.push(new StackEntry(0, context.wrappers.size()));
    while (!stack.isEmpty()) {
      StackEntry stackEntry = stack.peek();
      if (stackEntry.current >= stackEntry.end) {
        List<ArrangementEntryWrapper<E>> subEntries = entries.subList(stackEntry.start, stackEntry.end);
        if (subEntries.size() > 1) {
          doArrange(subEntries, context);
        }
        subEntries.clear();
        stack.pop();
      }
      else {
        ArrangementEntryWrapper<E> wrapper = entries.get(stackEntry.current++);
        List<ArrangementEntryWrapper<E>> children = wrapper.getChildren();
        if (!children.isEmpty()) {
          entries.addAll(children);
          stack.push(new StackEntry(stackEntry.end, children.size()));
        }
      }
    }
  }

  /**
   * Arranges (re-orders) given entries according to the given rules.
   *
   * @param entries  entries to arrange
   * @param rules    rules to use for arrangement
   * @param <E>      arrangement entry type
   * @return         arranged list of the given rules
   */
  @SuppressWarnings("AssignmentToForLoopParameter")
  @NotNull
  public static <E extends ArrangementEntry> List<E> arrange(@NotNull Collection<E> entries,
                                                             @NotNull List<? extends ArrangementMatchRule> rules)
  {
    List<E> arranged = new ArrayList<E>();
    Set<E> unprocessed = new LinkedHashSet<E>();
    List<Pair<Set<ArrangementEntry>, E>> dependent = new ArrayList<Pair<Set<ArrangementEntry>, E>>();
    for (E entry : entries) {
      List<? extends ArrangementEntry> dependencies = entry.getDependencies();
      if (dependencies == null) {
        unprocessed.add(entry);
      }
      else {
        if (dependencies.size() == 1 && dependencies.get(0) == entry.getParent()) {
          // Handle a situation when the entry is condifured to be at the first parent's children.
          arranged.add(entry);
        }
        else {
          Set<ArrangementEntry> first = new HashSet<ArrangementEntry>(dependencies);
          dependent.add(Pair.create(first, entry));
        }
      }
    }

    Set<E> matched = new HashSet<E>();

    for (ArrangementMatchRule rule : rules) {
      matched.clear();
      for (E entry : unprocessed) {
        if (entry.canBeMatched() && rule.getMatcher().isMatched(entry)) {
          arranged.add(entry);
          matched.add(entry);
        }
      }
      unprocessed.removeAll(matched);
    }
    arranged.addAll(unprocessed);

    for (int i = 0; i < arranged.size() && !dependent.isEmpty(); i++) {
      E e = arranged.get(i);
      for (Iterator<Pair<Set<ArrangementEntry>, E>> iterator = dependent.iterator(); iterator.hasNext(); ) {
        Pair<Set<ArrangementEntry>, E> pair = iterator.next();
        pair.first.remove(e);
        if (pair.first.isEmpty()) {
          iterator.remove();
          arranged.add(i + 1, pair.second);
        }
      }
    }

    return arranged;
  }

  @SuppressWarnings("unchecked")
  private static <E extends ArrangementEntry> void doArrange(@NotNull List<ArrangementEntryWrapper<E>> wrappers,
                                                             @NotNull Context<E> context) {
    if (wrappers.isEmpty()) {
      return;
    }
    Map<E, ArrangementEntryWrapper<E>> map = new LinkedHashMap<E, ArrangementEntryWrapper<E>>();
    for (ArrangementEntryWrapper<E> wrapper : wrappers) {
      map.put(wrapper.getEntry(), wrapper);
    }
    List<E> arranged = arrange(map.keySet(), context.rules);


    context.changer.prepare(wrappers, context);
    // We apply changes from the last position to the first position in order not to bother with offsets shifts.
    for (int i = arranged.size() - 1; i >= 0; i--) {
      ArrangementEntryWrapper<E> arrangedWrapper = map.get(arranged.get(i));
      ArrangementEntryWrapper<E> initialWrapper = wrappers.get(i);
      context.changer.replace(arrangedWrapper, initialWrapper, i > 0 ? map.get(arranged.get(i - 1)) : null, context);
    }
  }

  private static class Context<E extends ArrangementEntry> {

    @NotNull public final Rearranger<E>                          rearranger;
    @NotNull public final Collection<ArrangementEntryWrapper<E>> wrappers;
    @NotNull public final Document                               document;
    @NotNull public final List<? extends ArrangementMatchRule>   rules;
    @NotNull public final CodeStyleSettings                      settings;
    @NotNull public final Changer                                changer;

    private Context(@NotNull Rearranger<E> rearranger,
                    @NotNull Collection<ArrangementEntryWrapper<E>> wrappers,
                    @NotNull Document document,
                    @NotNull List<? extends ArrangementMatchRule> rules,
                    @NotNull CodeStyleSettings settings, @NotNull Changer changer)
    {
      this.rearranger = rearranger;
      this.wrappers = wrappers;
      this.document = document;
      this.rules = rules;
      this.settings = settings;
      this.changer = changer;
    }

    public static <T extends ArrangementEntry> Context<T> from(@NotNull Rearranger<T> rearranger,
                                                               @NotNull Document document,
                                                               @NotNull PsiElement root,
                                                               @NotNull Collection<TextRange> ranges,
                                                               @NotNull ArrangementSettings arrangementSettings,
                                                               @NotNull CodeStyleSettings codeStyleSettings)
    {
      Collection<T> entries = rearranger.parse(root, document, ranges, arrangementSettings);
      Collection<ArrangementEntryWrapper<T>> wrappers = new ArrayList<ArrangementEntryWrapper<T>>();
      ArrangementEntryWrapper<T> previous = null;
      for (T entry : entries) {
        ArrangementEntryWrapper<T> wrapper = new ArrangementEntryWrapper<T>(entry);
        if (previous != null) {
          previous.setNext(wrapper);
          wrapper.setPrevious(previous);
        }
        wrappers.add(wrapper);
        previous = wrapper;
      }
      Changer changer;
      if (document instanceof DocumentEx) {
        changer = new RangeMarkerAwareChanger<T>((DocumentEx)document);
      }
      else {
        changer = new DefaultChanger();
      }
      return new Context<T>(rearranger, wrappers, document, arrangementSettings.getRules(), codeStyleSettings, changer);
    }
  }

  private static class StackEntry {

    public int start;
    public int current;
    public int end;

    StackEntry(int start, int count) {
      this.start = start;
      current = start;
      end = start + count;
    }
  }

  private interface Changer<E extends ArrangementEntry> {
    void prepare(@NotNull List<ArrangementEntryWrapper<E>> toArrange, @NotNull Context<E> context);

    /**
     * Replaces given 'old entry' by the given 'new entry'.
     *
     * @param newWrapper  wrapper for an entry which text should replace given 'old entry' range
     * @param oldWrapper  wrapper for an entry which range should be replaced by the given 'new entry'
     * @param previous    wrapper which will be previous for the entry referenced via the given 'new wrapper'
     * @param context     current context
     */
    void replace(@NotNull ArrangementEntryWrapper<E> newWrapper,
                 @NotNull ArrangementEntryWrapper<E> oldWrapper,
                 @Nullable ArrangementEntryWrapper<E> previous,
                 @NotNull Context<E> context);
  }

  private static class DefaultChanger<E extends ArrangementEntry> implements Changer<E> {

    @NotNull private String myParentText;
    private          int    myParentShift;

    @Override
    public void prepare(@NotNull List<ArrangementEntryWrapper<E>> toArrange, @NotNull Context<E> context) {
      ArrangementEntryWrapper<E> parent = toArrange.get(0).getParent();
      if (parent == null) {
        myParentText = context.document.getText();
        myParentShift = 0;
      }
      else {
        myParentText = context.document.getCharsSequence().subSequence(parent.getStartOffset(), parent.getEndOffset()).toString();
        myParentShift = parent.getStartOffset();
      }
    }

    @SuppressWarnings("AssignmentToForLoopParameter")
    @Override
    public void replace(@NotNull ArrangementEntryWrapper<E> newWrapper,
                        @NotNull ArrangementEntryWrapper<E> oldWrapper,
                        @Nullable ArrangementEntryWrapper<E> previous,
                        @NotNull Context<E> context)
    {
      // Calculate blank lines before the arrangement.
      int blankLinesBefore = 0;
      TIntArrayList lineFeedOffsets = new TIntArrayList();
      int oldStartLine = context.document.getLineNumber(oldWrapper.getStartOffset());
      if (oldStartLine > 0) {
        int lastLineFeed = context.document.getLineStartOffset(oldStartLine) - 1;
        lineFeedOffsets.add(lastLineFeed);
        for (int i = lastLineFeed - 1 - myParentShift; i >= 0; i--) {
          i = CharArrayUtil.shiftBackward(myParentText, i, " \t");
          if (myParentText.charAt(i) == '\n') {
            blankLinesBefore++;
            lineFeedOffsets.add(i + myParentShift);
          }
          else {
            break;
          }
        }
      }

      ArrangementEntryWrapper<E> parentWrapper = oldWrapper.getParent();
      int desiredBlankLinesNumber = context.rearranger.getBlankLines(context.settings,
                                                                     parentWrapper == null ? null : parentWrapper.getEntry(),
                                                                     previous == null ? null : previous.getEntry(),
                                                                     newWrapper.getEntry());
      if (desiredBlankLinesNumber == blankLinesBefore && newWrapper.equals(oldWrapper)) {
        return;
      }

      String newEntryText = myParentText.substring(newWrapper.getStartOffset() - myParentShift, newWrapper.getEndOffset() - myParentShift);
      int lineFeedsDiff = desiredBlankLinesNumber - blankLinesBefore;
      if (lineFeedsDiff == 0 || desiredBlankLinesNumber < 0) {
        context.document.replaceString(oldWrapper.getStartOffset(), oldWrapper.getEndOffset(), newEntryText);
        return;
      }

      if (lineFeedsDiff > 0) {
        // Insert necessary number of blank lines.
        StringBuilder buffer = new StringBuilder(StringUtil.repeat("\n", lineFeedsDiff));
        buffer.append(newEntryText);
        context.document.replaceString(oldWrapper.getStartOffset(), oldWrapper.getEndOffset(), buffer);
      }
      else {
        // Cut exceeding blank lines.
        int replacementStartOffset = lineFeedOffsets.get(-lineFeedsDiff) + 1;
        context.document.replaceString(replacementStartOffset, oldWrapper.getEndOffset(), newEntryText);
      }

      // Update wrapper ranges.
      ArrangementEntryWrapper<E> parent = oldWrapper.getParent();
      if (parent == null) {
        return;
      }

      Deque<ArrangementEntryWrapper<E>> parents = new ArrayDeque<ArrangementEntryWrapper<E>>();
      do {
        parents.add(parent);
        parent.setEndOffset(parent.getEndOffset() + lineFeedsDiff);
        parent = parent.getParent();
      }
      while (parent != null);


      while (!parents.isEmpty()) {

        for (ArrangementEntryWrapper<E> wrapper = parents.removeLast().getNext(); wrapper != null; wrapper = wrapper.getNext()) {
          wrapper.applyShift(lineFeedsDiff);
        }
      }
    }
  }

  private static class RangeMarkerAwareChanger<E extends ArrangementEntry> implements Changer<E> {

    @NotNull private final List<ArrangementEntryWrapper<E>> myWrappers = new ArrayList<ArrangementEntryWrapper<E>>();
    @NotNull private final DocumentEx myDocument;

    RangeMarkerAwareChanger(@NotNull DocumentEx document) {
      myDocument = document;
    }

    @Override
    public void prepare(@NotNull List<ArrangementEntryWrapper<E>> toArrange, @NotNull Context<E> context) {
      myWrappers.clear();
      myWrappers.addAll(toArrange);
      for (ArrangementEntryWrapper<E> wrapper : toArrange) {
        wrapper.updateBlankLines(myDocument);
      }
    }

    @SuppressWarnings("AssignmentToForLoopParameter")
    @Override
    public void replace(@NotNull ArrangementEntryWrapper<E> newWrapper,
                        @NotNull ArrangementEntryWrapper<E> oldWrapper,
                        @Nullable ArrangementEntryWrapper<E> previous,
                        @NotNull Context<E> context)
    {
      // Calculate blank lines before the arrangement.
      int blankLinesBefore = oldWrapper.getBlankLinesBefore();

      ArrangementEntryWrapper<E> parentWrapper = oldWrapper.getParent();
      int desiredBlankLinesNumber = context.rearranger.getBlankLines(context.settings,
                                                                     parentWrapper == null ? null : parentWrapper.getEntry(),
                                                                     previous == null ? null : previous.getEntry(),
                                                                     newWrapper.getEntry());
      if ((desiredBlankLinesNumber < 0 || desiredBlankLinesNumber == blankLinesBefore) && newWrapper.equals(oldWrapper)) {
        return;
      }

      int lineFeedsDiff = desiredBlankLinesNumber - blankLinesBefore;
      int insertionOffset = oldWrapper.getStartOffset();
      if (oldWrapper.getStartOffset() > newWrapper.getStartOffset()) {
        insertionOffset -= newWrapper.getEndOffset() - newWrapper.getStartOffset();
      }
      if (newWrapper.getStartOffset() != oldWrapper.getStartOffset() || !newWrapper.equals(oldWrapper)) {
        myDocument.moveText(newWrapper.getStartOffset(), newWrapper.getEndOffset(), oldWrapper.getStartOffset());
        for (int i = myWrappers.size() - 1; i >= 0; i--) {
          ArrangementEntryWrapper<E> w = myWrappers.get(i);
          if (w == newWrapper) {
            continue;
          }
          if (w.getStartOffset() >= oldWrapper.getStartOffset() && w.getStartOffset() < newWrapper.getStartOffset()) {
            w.applyShift(newWrapper.getEndOffset() - newWrapper.getStartOffset());
          }
          else if (w.getStartOffset() < oldWrapper.getStartOffset() && w.getStartOffset() > newWrapper.getStartOffset()) {
            w.applyShift(newWrapper.getStartOffset() - newWrapper.getEndOffset());
          }
        }
      }

      if (desiredBlankLinesNumber >= 0 && lineFeedsDiff > 0) {
        myDocument.insertString(insertionOffset, StringUtil.repeat("\n", lineFeedsDiff));
        shiftOffsets(lineFeedsDiff, insertionOffset);
      }

      if (desiredBlankLinesNumber >= 0 && lineFeedsDiff < 0) {
        // Cut exceeding blank lines.
        int replacementStartOffset = getBlankLineOffset(-lineFeedsDiff, insertionOffset);
        myDocument.deleteString(replacementStartOffset, insertionOffset);
        shiftOffsets(replacementStartOffset - insertionOffset, insertionOffset);
      }

      // Update wrapper ranges.
      if (desiredBlankLinesNumber < 0 || lineFeedsDiff == 0 || parentWrapper == null) {
        return;
      }

      Deque<ArrangementEntryWrapper<E>> parents = new ArrayDeque<ArrangementEntryWrapper<E>>();
      do {
        parents.add(parentWrapper);
        parentWrapper.setEndOffset(parentWrapper.getEndOffset() + lineFeedsDiff);
        parentWrapper = parentWrapper.getParent();
      }
      while (parentWrapper != null);


      while (!parents.isEmpty()) {
        for (ArrangementEntryWrapper<E> wrapper = parents.removeLast().getNext(); wrapper != null; wrapper = wrapper.getNext()) {
          wrapper.applyShift(lineFeedsDiff);
        }
      }
    }

    private int getBlankLineOffset(int blankLinesNumber, int startOffset) {
      int startLine = myDocument.getLineNumber(startOffset);
      if (startLine <= 0) {
        return 0;
      }
      CharSequence text = myDocument.getCharsSequence();
      for (int i = myDocument.getLineStartOffset(startLine - 1) - 1; i >= 0; i = CharArrayUtil.lastIndexOf(text, "\n", i - 1)) {
        if (--blankLinesNumber <= 0) {
          return i + 1;
        }
      }
      return 0;
    }

    private void shiftOffsets(int shift, int changeOffset) {
      for (int i = myWrappers.size() - 1; i >= 0; i--) {
        ArrangementEntryWrapper<E> wrapper = myWrappers.get(i);
        if (wrapper.getStartOffset() < changeOffset) {
          break;
        }
        wrapper.applyShift(shift);
      }
    }
  }
}
