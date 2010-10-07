package ru.gelin.android.i18n;


import java.util.Locale;

import org.junit.Before;

public class RussianPluralFormsTest {

    PluralForms plurals;
    
    @Before
    public void setUp() throws Exception {
        plurals = PluralForms.getInstance(new Locale("ru"));
    }
    
    

}
