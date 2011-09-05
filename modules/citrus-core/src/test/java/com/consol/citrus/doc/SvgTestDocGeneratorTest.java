/*
 * Copyright 2006-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.doc;

import java.io.*;

import org.junit.Assert;
import org.springframework.core.io.FileSystemResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.consol.citrus.testng.AbstractTestNGUnitTest;
import com.consol.citrus.util.FileUtils;
import com.consol.citrus.util.TestCaseCreator;
import com.consol.citrus.util.TestCaseCreator.UnitFramework;

/**
 * @author Christoph Deppisch
 */
public class SvgTestDocGeneratorTest extends AbstractTestNGUnitTest {

    @BeforeClass
    public void createSampleTest() {
        TestCaseCreator creator = TestCaseCreator.build()
                .withAuthor("Christoph")
                .withDescription("This is a sample test")
                .withName("SampleTest")
                .usePackage("com.consol.citrus.sample")
                .withFramework(UnitFramework.TESTNG);

        creator.createTestCase();
    }
    
    @Test
    public void testSvgDocGeneration() throws IOException {
        SvgTestDocGenerator generator = SvgTestDocGenerator.build();
        
        generator.generateDoc();
        
        String docContent = FileUtils.readToString(new FileSystemResource(HtmlTestDocGenerator.getOutputDirectory() + "/SampleTest.svg"));
        
        Assert.assertTrue(docContent.contains("<title>SampleTest</title>"));
        Assert.assertTrue(docContent.contains("<desc>This is a sample test"));
        Assert.assertTrue(docContent.contains("TestCase: SampleTest"));
    }
}
