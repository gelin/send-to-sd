package ru.gelin.android.i18n;


import static org.junit.Assert.assertEquals;

import java.text.MessageFormat;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

public class RussianPluralFormsTest {

    PluralForms plurals;
    
    @Before
    public void setUp() throws Exception {
        plurals = PluralForms.getInstance(new Locale("ru"));
    }
    
    @Test
    public void testFiles() {
        String template = "{0} {1,choice,0#файл|1#файла|2#файлов}";
        assertEquals("1 файл", MessageFormat.format(template, 1, plurals.getForm(1)));
        assertEquals("2 файла", MessageFormat.format(template, 2, plurals.getForm(2)));
        assertEquals("5 файлов", MessageFormat.format(template, 5, plurals.getForm(5)));
    }
    
    @Test
    public void testCopied() {
        String template = "{0} {1,choice,0#файл скопирован|1#файла скопировано|2#файлов скопировано}";
        assertEquals("1 файл скопирован", MessageFormat.format(template, 1, plurals.getForm(1)));
        assertEquals("2 файла скопировано", MessageFormat.format(template, 2, plurals.getForm(2)));
        assertEquals("5 файлов скопировано", MessageFormat.format(template, 5, plurals.getForm(5)));
    }
    
    @Test
    public void testErrors() {
        String template = "{1,choice,0#Произошла {0} ошибка|1#Произошли {0} ошибки|2#Произошло {0} ошибок}";
        assertEquals("Произошла 1 ошибка", MessageFormat.format(template, 1, plurals.getForm(1)));
        assertEquals("Произошли 2 ошибки", MessageFormat.format(template, 2, plurals.getForm(2)));
        assertEquals("Произошло 5 ошибок", MessageFormat.format(template, 5, plurals.getForm(5)));
    }

}
