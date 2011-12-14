package org.jetbrains.jet.completion;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.LightCompletionTestCase;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikolay.Krasko
 */
public abstract class JetCompletionTestBase extends LightCompletionTestCase {
    private final String myPath;
    private final String myName;

    protected JetCompletionTestBase(@NotNull String path, @NotNull String name) {
        myPath = path;
        myName = name;

        // Set name explicitly because otherwise there will be "TestCase.fName cannot be null"
        setName("testCompletionExecute");
    }

    public void testCompletionExecute() throws Exception {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), myPath).getPath() +
               File.separator;
    }

    @NotNull
    @Override
    public String getName() {
        return "test" + myName;
    }

    private CompletionType type;

    protected void doTest() throws Exception {
        final String testName = getTestName(false);

        type = (testName.startsWith("Smart")) ? CompletionType.SMART : CompletionType.BASIC;

        configureByFile(testName + ".kt");

        assertContainsItems(itemsShouldExist(getFile().getText()));
        assertNotContainItems(itemsShouldAbsent(getFile().getText()));
        
        Integer itemsNumber = getExpectedNumber(getFile().getText());
        if (itemsNumber != null) {
            assertEquals(itemsNumber.intValue(), myItems.length);
        }
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    @Override
    protected void complete(final int time) {
        new CodeCompletionHandlerBase(type, false, false, true).invokeCompletion(getProject(), getEditor(), time, false);

        LookupImpl lookup = (LookupImpl) LookupManager.getActiveLookup(myEditor);
        myItems = lookup == null ? null : lookup.getItems().toArray(LookupElement.EMPTY_ARRAY);
        myPrefix = lookup == null ? null : lookup.itemPattern(lookup.getItems().get(0));
    }

    @NotNull
    private static String[] itemsShouldExist(String fileText) {
        return findListWithPrefix("// EXIST:", fileText);
    }

    @NotNull
    private static String[] itemsShouldAbsent(String fileText) {
        return findListWithPrefix("// ABSENT:", fileText);
    }

    @Nullable
    private static Integer getExpectedNumber(String fileText) {
        final String[] numberStrings = findListWithPrefix("// NUMBER:", fileText);
        if (numberStrings.length > 0) {
            return Integer.parseInt(numberStrings[0]);
        }

        return null;
    }
    
    @NotNull
    private static String[] findListWithPrefix(String prefix, String fileText) {
        ArrayList<String> result = new ArrayList<String>();

        for (String line : findLinesWithPrefixRemoved(prefix, fileText)) {
            String[] completions = line.split(",");

            for (String completion : completions) {
                result.add(completion.trim());
            }
        }

        return result.toArray(new String[result.size()]);
    }

    @NotNull
    private static List<String> findLinesWithPrefixRemoved(String prefix, String fileText) {
        ArrayList<String> result = new ArrayList<String>();

        for (String line : fileNonEmptyLines(fileText)) {
            if (line.startsWith(prefix)) {
                result.add(line.substring(prefix.length()).trim());
            }
        }

        return result;
    }

    @NotNull
    private static List<String> fileNonEmptyLines(String fileText) {
        ArrayList<String> result = new ArrayList<String>();

        BufferedReader reader = new BufferedReader(new StringReader(fileText));

        try {
            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    result.add(line.trim());
                }
            }
        } catch(IOException e) {
            assert false;
        }

        return result;
    }
}
