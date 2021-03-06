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
package org.jetbrains.idea.svn;

import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.Consumer;
import com.intellij.util.containers.Convertor;
import com.intellij.util.containers.MultiMap;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.jetbrains.idea.svn.commandLine.SvnInfoHandler;
import org.jetbrains.idea.svn.commandLine.SvnStatusHandler;
import org.jetbrains.idea.svn.portable.IdeaSVNInfo;
import org.jetbrains.idea.svn.portable.PortableStatus;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 11/9/12
 * Time: 7:03 PM
 */
public class SvnParseCommandLineParseTest extends TestCase {
  public void testInfo() throws Exception {
    final String s = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                     "<info>\n" +
                     "<entry\n" +
                     "   path=\"ReduceReturnOrYieldBreakTransformation.cs\"\n" +
                     "   revision=\"91603\"\n" +
                     "   kind=\"file\">\n" +
                     "<url>http://svn.labs.intellij.net/resharper/trunk/ReSharper/src/Decompiler.Core/Src/Transformations/StatementStructure/ReduceReturnOrYieldBreakTransformation.cs</url>\n" +
                     "<repository>\n" +
                     "<root>http://svn.labs.intellij.net/resharper</root>\n" +
                     "<uuid>ed0594e5-7722-0410-9c76-949374689613</uuid>\n" +
                     "</repository>\n" +
                     "<wc-info>\n" +
                     "<wcroot-abspath>C:/TestProjects/sortedProjects/Subversion/Resharper17short</wcroot-abspath>\n" +
                     "<schedule>normal</schedule>\n" +
                     "<depth>infinity</depth>\n" +
                     "<text-updated>2012-01-20T11:25:32.625000Z</text-updated>\n" +
                     "<checksum>7af8adacb93afaa48b2cfb76de605824c220983a</checksum>\n" +
                     "</wc-info>\n" +
                     "<commit\n" +
                     "   revision=\"87972\">\n" +
                     "<author>Slava.Trenogin</author>\n" +
                     "<date>2011-10-06T21:27:41.539022Z</date>\n" +
                     "</commit>\n" +
                     "</entry>\n" +
                     "</info>";

    final SVNInfo[] info = new SVNInfo[1];
    final SvnInfoHandler handler = new SvnInfoHandler(new File("C:/base/"), new Consumer<SVNInfo>() {
      @Override
      public void consume(SVNInfo info1) {
        info[0] = info1;
      }
    });
    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
    parser.parse(new ByteArrayInputStream(s.getBytes(CharsetToolkit.UTF8_CHARSET)), handler);

    Assert.assertNotNull(info[0]);
  }

  public void testStatus() throws Exception {
    final String s = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                     "<status>\n" +
                     "<target\n" +
                     "   path=\".\">\n" +
                     "<entry\n" +
                     "   path=\".\">\n" +
                     "<wc-status\n" +
                     "   props=\"normal\"\n" +
                     "   item=\"incomplete\"\n" +
                     "   revision=\"92339\">\n" +
                     "<commit\n" +
                     "   revision=\"91672\">\n" +
                     "<author>qx</author>\n" +
                     "<date>2012-01-27T16:11:06.069351Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"Decompiler\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   tree-conflicted=\"true\"\n" +
                     "   item=\"missing\">\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"Help\">\n" +
                     "<wc-status\n" +
                     "   props=\"normal\"\n" +
                     "   item=\"incomplete\"\n" +
                     "   revision=\"92339\">\n" +
                     "<commit\n" +
                     "   revision=\"91555\">\n" +
                     "<author>Egor.Malyshev</author>\n" +
                     "<date>2012-01-18T18:05:07.328519Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"Help\\XML\">\n" +
                     "<wc-status\n" +
                     "   props=\"normal\"\n" +
                     "   item=\"incomplete\"\n" +
                     "   revision=\"92339\">\n" +
                     "<commit\n" +
                     "   revision=\"91555\">\n" +
                     "<author>Egor.Malyshev</author>\n" +
                     "<date>2012-01-18T18:05:07.328519Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"Help\\XML\\images\">\n" +
                     "<wc-status\n" +
                     "   props=\"normal\"\n" +
                     "   item=\"incomplete\"\n" +
                     "   revision=\"92339\">\n" +
                     "<commit\n" +
                     "   revision=\"91170\">\n" +
                     "<author>Maria.Egorkina</author>\n" +
                     "<date>2011-12-20T13:26:52.217550Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"Platform\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   item=\"external\">\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"ReSharper.ipr\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"91677\"\n" +
                     "   props=\"normal\">\n" +
                     "<commit\n" +
                     "   revision=\"87631\">\n" +
                     "<author>Alexey.Kuptsov</author>\n" +
                     "<date>2011-09-30T11:25:10.391467Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"ReSharper.iws\">\n" +
                     "<wc-status\n" +
                     "   item=\"ignored\"\n" +
                     "   props=\"none\">\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"measure.bat\">\n" +
                     "<wc-status\n" +
                     "   item=\"unversioned\"\n" +
                     "   props=\"none\">\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"src\\Daemon\\src\\HighlightingBase.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"deleted\"\n" +
                     "   revision=\"91677\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"79264\">\n" +
                     "<author>xvost</author>\n" +
                     "<date>2011-02-05T16:06:12.116814Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"src\\Daemon\\src\\HighlightingBase1.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   copied=\"true\"\n" +
                     "   item=\"added\">\n" +
                     "<commit\n" +
                     "   revision=\"79264\">\n" +
                     "<author>xvost</author>\n" +
                     "<date>2011-02-05T16:06:12.116814Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"src\\Decompiler.Core\">\n" +
                     "<wc-status\n" +
                     "   item=\"added\"\n" +
                     "   props=\"normal\"\n" +
                     "   copied=\"true\"\n" +
                     "   tree-conflicted=\"true\">\n" +
                     "<commit\n" +
                     "   revision=\"91559\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2012-01-18T19:17:40.876383Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"src\\Decompiler.Core\\Src\\Transformations\\Loops\\EliminateRedundantContinueTransformation.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"normal\"\n" +
                     "   copied=\"true\"\n" +
                     "   item=\"modified\">\n" +
                     "<commit\n" +
                     "   revision=\"87972\">\n" +
                     "<author>Slava.Trenogin</author>\n" +
                     "<date>2011-10-06T21:27:41.539022Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"src\\Decompiler.Core\\Src\\Transformations\\StatementStructure\\ReduceReturnOrYieldBreakTransformation.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   props=\"normal\"\n" +
                     "   copied=\"true\">\n" +
                     "<commit\n" +
                     "   revision=\"87972\">\n" +
                     "<author>Slava.Trenogin</author>\n" +
                     "<date>2011-10-06T21:27:41.539022Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"stat.txt\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   item=\"unversioned\">\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\CommonServices\\src\\Services\\CommonServices.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"73708\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2010-09-10T14:14:04.090943Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Document.Tests\\AssemblyInfo.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"86795\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2011-09-11T13:54:16.917943Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Document.Tests\\RangeMarkerTest.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\">\n" +
                     "<commit\n" +
                     "   revision=\"77688\">\n" +
                     "<author>Alexander.Zverev</author>\n" +
                     "<date>2010-12-14T15:56:38.322018Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\AssemblyInfo.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"82127\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2011-04-13T20:57:30.828600Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\AssemblyNameInfo.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"90865\">\n" +
                     "<author>xvost</author>\n" +
                     "<date>2011-12-13T13:10:30.902950Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\BinaryBlobExtensions.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"74075\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2010-09-17T09:32:30.827654Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\BinaryBlobReader.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\">\n" +
                     "<commit\n" +
                     "   revision=\"74075\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2010-09-17T09:32:30.827654Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\BinaryBlobStream.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\">\n" +
                     "<commit\n" +
                     "   revision=\"74075\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2010-09-17T09:32:30.827654Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\BlobOnReader.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"74075\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2010-09-17T09:32:30.827654Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\ComStreamWrapper.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"91348\">\n" +
                     "<author>Sergey.Shkredov</author>\n" +
                     "<date>2012-01-09T11:26:53.770349Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\EmptyBlob.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\">\n" +
                     "<commit\n" +
                     "   revision=\"74075\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2010-09-17T09:32:30.827654Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\GAC.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"normal\"\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\">\n" +
                     "<commit\n" +
                     "   revision=\"75626\">\n" +
                     "<author>Slava.Trenogin</author>\n" +
                     "<date>2010-10-21T07:41:45.036722Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\GacUtil.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\">\n" +
                     "<commit\n" +
                     "   revision=\"91646\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2012-01-21T19:08:04.108471Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\IBinaryReader.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"71390\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2010-07-12T18:29:27.763006Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\IntInterval.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"55567\">\n" +
                     "<author>qx</author>\n" +
                     "<date>2009-06-03T10:11:14.985037Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\ManifestResourceUtil.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\">\n" +
                     "<commit\n" +
                     "   revision=\"87972\">\n" +
                     "<author>Slava.Trenogin</author>\n" +
                     "<date>2011-10-06T21:27:41.539022Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\MarshalSpecParser.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"76982\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2010-11-28T10:16:36.309593Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\MetadataHelpers.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\">\n" +
                     "<commit\n" +
                     "   revision=\"91348\">\n" +
                     "<author>Sergey.Shkredov</author>\n" +
                     "<date>2012-01-09T11:26:53.770349Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\ModuleQualificationUtil.cs\">\n" +
                     "<wc-status\n" +
                     "   props=\"none\"\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\">\n" +
                     "<commit\n" +
                     "   revision=\"60918\">\n" +
                     "<author>xvost</author>\n" +
                     "<date>2009-11-03T10:42:37.363952Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\StreamBinaryReader.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"71390\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2010-07-12T18:29:27.763006Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "<entry\n" +
                     "   path=\"C:\\TestProjects\\sortedProjects\\Subversion\\Resharper17short\\Platform\\src\\Metadata\\src\\Utils\\SubStream.cs\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"92564\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"76725\">\n" +
                     "<author>Leonid.Shalupov</author>\n" +
                     "<date>2010-11-20T13:23:44.172899Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "</target>\n" +
                     "<changelist\n" +
                     "   name=\"ads\">\n" +
                     "<entry\n" +
                     "   path=\"BuildVsix.cmd\">\n" +
                     "<wc-status\n" +
                     "   item=\"modified\"\n" +
                     "   revision=\"91677\"\n" +
                     "   props=\"none\">\n" +
                     "<commit\n" +
                     "   revision=\"77579\">\n" +
                     "<author>Victor.Kropp</author>\n" +
                     "<date>2010-12-13T11:06:36.141754Z</date>\n" +
                     "</commit>\n" +
                     "</wc-status>\n" +
                     "</entry>\n" +
                     "</changelist>\n" +
                     "</status>\n";

    final SvnStatusHandler[] handlerArr = new SvnStatusHandler[1];
    final SvnStatusHandler handler = new SvnStatusHandler(new SvnStatusHandler.ExternalDataCallback() {
      @Override
      public void switchPath() {
        handlerArr[0].getPending().getKind();
      }

      @Override
      public void switchChangeList(String newList) {
      }
    }, new File("C:/base/"), new Convertor<File, SVNInfo>() {
      @Override
      public SVNInfo convert(File o) {
        try {
          o.getCanonicalFile();
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
        final int idx = o.getPath().indexOf(":");
        Assert.assertTrue(idx > 0);
        final int secondIdx = o.getPath().indexOf(":", idx + 1);
        Assert.assertTrue(o.getPath(), secondIdx == -1);
        try {
          return new IdeaSVNInfo("C:/base/1", SVNURL.parseURIEncoded("http://a.b.c"), SVNRevision.HEAD, SVNNodeKind.FILE, "",
                                 SVNURL.parseURIEncoded("http://a.b.c"), 1, new Date(), "me", null, SVNDepth.EMPTY, 1);
        }
        catch (SVNException e) {
          //
          throw new RuntimeException(e);
        }
      }
    });
    handlerArr[0] = handler;

    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
    parser.parse(new ByteArrayInputStream(s.getBytes(CharsetToolkit.UTF8_CHARSET)), handler);
    final MultiMap<String,PortableStatus> changes = handler.getCurrentListChanges();
  }
}
