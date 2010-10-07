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
        String template = "{0} {1,choice,0#file is|1#files are} copied. {2} {3,choice,0#error has|1#errors have} appeared.";
        assertEquals("1 file copied", 
                MessageFormat.format(template, 1, plurals.getForm(1), 0, plurals.getForm(0)));
        assertEquals("2 files copied", 
                MessageFormat.format(template, 2, plurals.getForm(2), 0, plurals.getForm(0)));
        assertEquals("1 file copied. 1 error has appeared.", 
                MessageFormat.format(template, 1, plurals.getForm(1), 1, plurals.getForm(1)));
        assertEquals("2 files copied. 1 error has appeared.", 
                MessageFormat.format(template, 2, plurals.getForm(2), 1, plurals.getForm(1)));
        assertEquals("1 file copied. 2 errors have appeared.", 
                MessageFormat.format(template, 1, plurals.getForm(1), 2, plurals.getForm(2)));
        assertEquals("2 files copied. 2 errors have appeared.", 
                MessageFormat.format(template, 2, plurals.getForm(2), 2, plurals.getForm(2)));
    }

}
