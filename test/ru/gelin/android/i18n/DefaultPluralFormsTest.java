package ru.gelin.android.i18n;


import static org.junit.Assert.*;

import java.text.MessageFormat;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

public class DefaultPluralFormsTest {

    PluralForms plurals;
    
    @Before
    public void setUp() throws Exception {
        plurals = PluralForms.getInstance(Locale.US);
    }
    
    @Test
    public void testFiles() {
        String template = "{0} {1,choice,0#file|1#files}";
        assertEquals("1 file", MessageFormat.format(template, 1, plurals.getForm(1)));
        assertEquals("2 files", MessageFormat.format(template, 2, plurals.getForm(2)));
    }
    
    @Test
    public void testCopied() {
        String template = "{0} {1,choice,0#file has|1#files have} been copied";
        assertEquals("1 file has been copied", 
                MessageFormat.format(template, 1, plurals.getForm(1)));
        assertEquals("2 files have been copied", 
                MessageFormat.format(template, 2, plurals.getForm(2)));
    }

}
