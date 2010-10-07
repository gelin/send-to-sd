package ru.gelin.android.i18n;

import java.text.ChoiceFormat;
import java.util.Locale;

/**
 *  <p>
 *  Calculates the number of plural form.
 *  Different languages has different number of plural forms.
 *  And the rules to apply the correct form differ.
 *  For English there are just two forms: "1 file" and "2 files".
 *  </p><p>
 *  Instances of this class returns the plural form number based on
 *  the locale and the numeral to which the plural form apply.
 *  Plural forms are numbered starting with zero.
 *  </p><p>
 *  For example, for English:
 *  <ul>
 *  <li><code>PluralForms.getCount()</code> will return 2 (two forms)</li>
 *  <li><code>PluralForms.getForm(1)</code> will return 0 (first form)</li>
 *  <li><code>PluralForms.getForm(2)</code> will return 1 (second form)</li>
 *  </ul>
 *  </p><p>
 *  You should pass the {@link #getForm} result to the {@link ChoiceFormat}.
 *  For example: <code>MessageFormat.format("{0} {1,choice,0#file|1#files}", n, PluralForms.getInstance().getForm(n))</code>
 *  </p>
 *  @see    http://www.gnu.org/software/gettext/manual/gettext.html#Plural-forms
 */
public abstract class PluralForms {

    /**
     *  Creates PluralForms for default locale.
     */
    public static PluralForms getInstance() {
        return getInstance(Locale.getDefault());
    }
    
    /**
     *  Creates PluralForms for specified locale.
     */
    public static PluralForms getInstance(Locale locale) {
        if (locale == null) {
            return new DefaultPluralForms();
        }
        String lang = locale.getLanguage();
        if ("ru".equalsIgnoreCase(lang)) {
            return new RussianPluralForms();
        }
        return new DefaultPluralForms();
    }
    
    /**
     *  Returns the total number of plural forms.
     */
    abstract public int getCount();
    
    /**
     *  Returns the number of the plural form for the numeral.
     */
    abstract public int getForm(int n);
    
    /**
     *  Default plural forms (uses English rules).
     */
    static class DefaultPluralForms extends PluralForms {
        @Override
        public int getCount() {
            return 2;
        }
        @Override
        public int getForm(int n) {
            switch (n) {
            case 1: return 0;
            default: return 1;
            }
        }
    }
    
    /**
     *  Russian plural forms.
     *  Three forms, special cases for numbers ending in 1 and 2, 3, 4, except those ending in 1[1-4]
     */
    static class RussianPluralForms extends PluralForms {
        @Override
        public int getCount() {
            return 3;
        }
        @Override
        public int getForm(int n) {
            int rem10 = n % 10;
            int rem100 = n % 100;
            if (rem10 == 1 && rem100 != 11) {
                return 0;
            } else if (rem10 >= 2 && rem10 <=4 &&
                    (rem100 < 10 || rem100 >= 20)) {
                return 1;
            } else {
                return 2;
            }
        }
    }

}
