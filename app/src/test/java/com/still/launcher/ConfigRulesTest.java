package com.still.launcher;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ConfigRulesTest {
    @Test public void duplicateAppsPreserveFirstChosenOrder() {
        assertEquals(Arrays.asList("tv.one.app", "tv.two.app"),
                ConfigRules.packages(Arrays.asList("tv.one.app", "tv.two.app", "tv.one.app")));
    }
    @Test public void backupCannotInjectUrisPathsOrMalformedPackageIds() {
        for (String invalid : new String[]{"../prefs", "content://document", "com..app", "", "com.app|tv.other", "1.com.app"})
            assertFalse(invalid, ConfigRules.packageId(invalid));
    }
    @Test public void unicodeCollectionNamesArePreserved() { assertEquals("فیملی", ConfigRules.groupName(" فیملی ")); }
    @Test(expected = IllegalArgumentException.class) public void controlCharactersAreRejected() { ConfigRules.groupName("Movies\nGames"); }
    @Test(expected = IllegalArgumentException.class) public void longCollectionNamesAreRejected() { ConfigRules.groupName("123456789012345678901234567890123"); }
    @Test(expected = IllegalArgumentException.class) public void unknownLayoutValuesAreRejected() { ConfigRules.range(5, 0, 2); }
}
