/*
 * This is the source code of Emm for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package info.emm.messenger;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.text.format.DateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import info.emm.ui.ApplicationLoader;
import info.emm.yuanchengcloudb.R;

public class LocaleController {

    public static boolean isRTL = false;
    public static FastDateFormat formatterDay;
    public static FastDateFormat formatterWeek;
    public static FastDateFormat formatterMonth;
    public static FastDateFormat formatterYear;
    public static FastDateFormat formatterYearMax;
    public static FastDateFormat chatDate;
    public static FastDateFormat chatFullDate;

    private Locale currentLocale;
    private Locale systemDefaultLocale;
    private LocaleInfo currentLocaleInfo;
    private HashMap<String, String> localeValues = new HashMap<String, String>();
    private String languageOverride;
    private boolean changingConfiguration = false;

    public static class LocaleInfo {
        public String name;
        public String nameEnglish;
        public String shortName;
        public String country;
    }

    public ArrayList<LocaleInfo> sortedLanguages = new ArrayList<LocaleController.LocaleInfo>();
    public HashMap<String, LocaleInfo> languagesDict = new HashMap<String, LocaleInfo>();

    private static volatile LocaleController Instance = null;
    public static LocaleController getInstance() {
        LocaleController localInstance = Instance;
        if (localInstance == null) {
            synchronized (LocaleController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new LocaleController();
                }
            }
        }
        return localInstance;
    }

    public LocaleController() {
        LocaleController.LocaleInfo localeInfo = new LocaleController.LocaleInfo();
        localeInfo.name = "English";
        localeInfo.nameEnglish = "English";
        localeInfo.shortName = "en";
        localeInfo.country = "";
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);
/*
        localeInfo = new LocaleController.LocaleInfo();
        localeInfo.name = "Italiano";
        localeInfo.nameEnglish = "Italian";
        localeInfo.shortName = "it";
        localeInfo.country = "";
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleController.LocaleInfo();
        localeInfo.name = "Espa帽ol";
        localeInfo.nameEnglish = "Spanish";
        localeInfo.shortName = "es";
        localeInfo.country = "";
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleController.LocaleInfo();
        localeInfo.name = "Deutsch";
        localeInfo.nameEnglish = "German";
        localeInfo.shortName = "de";
        localeInfo.country = "";
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleController.LocaleInfo();
        localeInfo.name = "Nederlands";
        localeInfo.nameEnglish = "Dutch";
        localeInfo.shortName = "nl";
        localeInfo.country = "";
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleController.LocaleInfo();
        localeInfo.name = "丕賱毓乇亘賷丞";
        localeInfo.nameEnglish = "Arabic";
        localeInfo.shortName = "ar";
        localeInfo.country = "";
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);
        */
        localeInfo = new LocaleController.LocaleInfo();
        localeInfo.name = "简体中文";
        localeInfo.nameEnglish = "Simplified Chinese";
        localeInfo.shortName = "CN";
        localeInfo.country = "zh";
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        //wangxm delete
        localeInfo = new LocaleController.LocaleInfo();
        localeInfo.name = "繁體中文";
        localeInfo.nameEnglish = "Traditional Chinese";
        localeInfo.shortName = "TW"; // getcountry 为锟斤拷map key唯一
        localeInfo.country = "zh"; // getlanguage
        sortedLanguages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        Collections.sort(sortedLanguages, new Comparator<LocaleInfo>() {
            @Override
            public int compare(LocaleController.LocaleInfo o, LocaleController.LocaleInfo o2) {
                return o.name.compareTo(o2.name);
            }
        });

        localeInfo = new LocaleController.LocaleInfo();
        localeInfo.name = "System default";
        localeInfo.nameEnglish = "System default";
        localeInfo.shortName = null;
        sortedLanguages.add(0, localeInfo);

        // jenf for language
        GetConfigLanguage();

    }

    // jenf for language
    public void GetConfigLanguage()
    {
        systemDefaultLocale = Locale.getDefault();
        LocaleInfo currentInfo = null;
        boolean override = false;

        try {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig_" + UserConfig.clientUserId, Activity.MODE_PRIVATE);
            String lang = preferences.getString("language", null);
            if (lang != null) {
                currentInfo = languagesDict.get(lang);
                if (currentInfo != null) {
                    override = true;
                }
            }

            if (currentInfo == null && systemDefaultLocale.getLanguage() != null) {
                currentInfo = languagesDict.get(systemDefaultLocale.getLanguage());
            }
            if (currentInfo == null)
            {
                currentInfo = languagesDict.get(systemDefaultLocale.getCountry());
            }
            if (currentInfo == null) {
                currentInfo = languagesDict.get("en");
            }
            applyLanguage(currentInfo, override);
        } catch (Exception e) {
            FileLog.e("emm", e);
        }

    }

    public void applyLanguage(LocaleInfo localeInfo, boolean override) {
        if (localeInfo == null) {
            return;
        }
        try {
            Locale newLocale = null;
            if (localeInfo.shortName != null) {
                if(localeInfo.country.equalsIgnoreCase("zh"))
                {
                    newLocale = new Locale(localeInfo.country, localeInfo.shortName);
                }
                else
                {
                    newLocale = new Locale(localeInfo.shortName);
                }

                if (newLocale != null) {
                    if (override) {
                        languageOverride = localeInfo.shortName;
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig_" + UserConfig.clientUserId, Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("language", localeInfo.shortName);
                        editor.commit();
                    }
                }
            } else {
                newLocale = systemDefaultLocale;
                languageOverride = null;
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig_" + UserConfig.clientUserId, Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("language");
                editor.commit();
            }
            if (newLocale != null) {
                currentLocale = newLocale;
                currentLocaleInfo = localeInfo;
                MessagesController.getInstance().currentLocaleInfo = localeInfo;// jenf for language
                changingConfiguration = true;
                Locale.setDefault(currentLocale);
                android.content.res.Configuration config = new android.content.res.Configuration();
                config.locale = currentLocale;

                if (currentLocaleInfo.shortName.equalsIgnoreCase("CN"))
                {
                    config.locale = Locale.SIMPLIFIED_CHINESE;
                }
                else if (currentLocaleInfo.shortName.equalsIgnoreCase("TW"))
                {
                    config.locale = Locale.TRADITIONAL_CHINESE;
                }
                ApplicationLoader.applicationContext.getResources().updateConfiguration(config, ApplicationLoader.applicationContext.getResources().getDisplayMetrics());
                changingConfiguration = false;
            }
        } catch (Exception e) {
            FileLog.e("emm", e);
            changingConfiguration = false;
        }
        recreateFormatters();
    }

    private void loadCurrentLocale() {
        localeValues.clear();
    }

    public static String getCurrentLanguageName() {
        // jenf for language
        if (null != MessagesController.getInstance().currentLocaleInfo)
        {
            return MessagesController.getInstance().currentLocaleInfo.name;
        }
        return getString("LanguageName", R.string.LanguageName);

    }

    public static String getString(String key, int res) {
        String value = getInstance().localeValues.get(key);
        if (value == null) {
            value = ApplicationLoader.applicationContext.getString(res);
        }
        return value;
    }

    public static String formatString(String key, int res, Object... args) {
        String value = getInstance().localeValues.get(key);
        if (value == null) {
            value = ApplicationLoader.applicationContext.getString(res);
        }
        try {
            if (getInstance().currentLocale != null) {
                return String.format(getInstance().currentLocale, value, args);
            } else {
                return String.format(value, args);
            }
        } catch (Exception e) {
            FileLog.e("emm", e);
            return "LOC_ERR: " + key;
        }
    }

    public void onDeviceConfigurationChange(Configuration newConfig) {
        if (changingConfiguration) {
            return;
        }
        systemDefaultLocale = newConfig.locale;
        if (languageOverride != null) {
            LocaleInfo toSet = currentLocaleInfo;
            currentLocaleInfo = null;
            applyLanguage(toSet, false);
        } else {
            Locale newLocale = newConfig.locale;
            if (newLocale != null) {
                String d1 = newLocale.getDisplayName();
                String d2 = currentLocale.getDisplayName();
                if (d1 != null && d2 != null && !d1.equals(d2)) {
                    recreateFormatters();
                }
                currentLocale = newLocale;
            }
        }
    }

    public static String formatDateChat(long date) {
        Calendar rightNow = Calendar.getInstance();
        int year = rightNow.get(Calendar.YEAR);

        rightNow.setTimeInMillis(date * 1000);
        int dateYear = rightNow.get(Calendar.YEAR);

        if (year == dateYear) {
            return chatDate.format(date * 1000);
        }
        return chatFullDate.format(date * 1000);
    }

    public static String formatDate(long date) {
        Calendar rightNow = Calendar.getInstance();
        int day = rightNow.get(Calendar.DAY_OF_YEAR);
        int year = rightNow.get(Calendar.YEAR);
        rightNow.setTimeInMillis(date * 1000);
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);

        if (dateDay == day && year == dateYear) {
            return formatterDay.format(new Date(date * 1000));
        } else if (dateDay + 1 == day && year == dateYear) {
            return ApplicationLoader.applicationContext.getResources().getString(R.string.Yesterday);
        } else if (year == dateYear) {
            return formatterMonth.format(new Date(date * 1000));
        } else {
            return formatterYear.format(new Date(date * 1000));
        }
    }

    public static String formatDateOnline(long date) {
        Calendar rightNow = Calendar.getInstance();
        int day = rightNow.get(Calendar.DAY_OF_YEAR);
        int year = rightNow.get(Calendar.YEAR);
        rightNow.setTimeInMillis(date * 1000);
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);

        if (dateDay == day && year == dateYear) {
            return String.format("%s %s %s", LocaleController.getString("LastSeen", R.string.LastSeen), LocaleController.getString("TodayAt", R.string.TodayAt), formatterDay.format(new Date(date * 1000)));
        } else if (dateDay + 1 == day && year == dateYear) {
            return String.format("%s %s %s", LocaleController.getString("LastSeen", R.string.LastSeen), LocaleController.getString("YesterdayAt", R.string.YesterdayAt), formatterDay.format(new Date(date * 1000)));
        } else if (year == dateYear) {
            return String.format("%s %s %s %s", LocaleController.getString("LastSeenDate", R.string.LastSeenDate), formatterMonth.format(new Date(date * 1000)), LocaleController.getString("OtherAt", R.string.OtherAt), formatterDay.format(new Date(date * 1000)));
        } else {
            return String.format("%s %s %s %s", LocaleController.getString("LastSeenDate", R.string.LastSeenDate), formatterYear.format(new Date(date * 1000)), LocaleController.getString("OtherAt", R.string.OtherAt), formatterDay.format(new Date(date * 1000)));
        }
    }

    public static void recreateFormatters() {
        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();
        if (lang == null) {
            lang = "en";
        }
        isRTL = lang.toLowerCase().equals("ar");
        if (lang.equals("en")) {
            formatterMonth = FastDateFormat.getInstance("MMM dd", locale);
            formatterYear = FastDateFormat.getInstance("dd.MM.yy", locale);
            formatterYearMax = FastDateFormat.getInstance("dd.MM.yyyy", locale);
            chatDate = FastDateFormat.getInstance("MMMM d", locale);
            chatFullDate = FastDateFormat.getInstance("MMMM d, yyyy", locale);
        } else if (lang.startsWith("es")) {
            formatterMonth = FastDateFormat.getInstance("dd 'de' MMM", locale);
            formatterYear = FastDateFormat.getInstance("dd.MM.yy", locale);
            formatterYearMax = FastDateFormat.getInstance("dd.MM.yyyy", locale);
            chatDate = FastDateFormat.getInstance("d 'de' MMMM", locale);
            chatFullDate = FastDateFormat.getInstance("d 'de' MMMM 'de' yyyy", locale);
        } else {
            formatterMonth = FastDateFormat.getInstance("dd MMM", locale);
            formatterYear = FastDateFormat.getInstance("dd.MM.yy", locale);
            formatterYearMax = FastDateFormat.getInstance("dd.MM.yyyy", locale);
            chatDate = FastDateFormat.getInstance("d MMMM", locale);
            chatFullDate = FastDateFormat.getInstance("d MMMM yyyy", locale);
        }
        formatterWeek = FastDateFormat.getInstance("EEE", locale);

        if (lang != null) {
            if (DateFormat.is24HourFormat(ApplicationLoader.applicationContext)) {
                formatterDay = FastDateFormat.getInstance("HH:mm", locale);
            } else {
                if (lang.toLowerCase().equals("ar")) {
                    formatterDay = FastDateFormat.getInstance("h:mm a", locale);
                } else {
                    formatterDay = FastDateFormat.getInstance("h:mm a", Locale.US);
                }
            }
        } else {
            formatterDay = FastDateFormat.getInstance("h:mm a", Locale.US);
        }
    }

    public static String stringForMessageListDate(long date) {
        Calendar rightNow = Calendar.getInstance();
        int day = rightNow.get(Calendar.DAY_OF_YEAR);
        int year = rightNow.get(Calendar.YEAR);
        rightNow.setTimeInMillis(date * 1000);
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);

        if (year != dateYear) {
            return formatterYear.format(new Date(date * 1000));
        } else {
            int dayDiff = dateDay - day;
            if(dayDiff == 0 || dayDiff == -1 && (int)(System.currentTimeMillis() / 1000) - date < 60 * 60 * 8) {
                return formatterDay.format(new Date(date * 1000));
            } else if(dayDiff > -7 && dayDiff <= -1) {
                return formatterWeek.format(new Date(date * 1000));
            } else {
                return formatterMonth.format(new Date(date * 1000));
            }
        }
    }

    public boolean IsChineseMainLand()
    {
        String lan = LocaleController.getCurrentLanguageName();
        String result = "";
        if( lan.contains("锟斤拷锟斤拷锟斤拷锟斤拷") )
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
