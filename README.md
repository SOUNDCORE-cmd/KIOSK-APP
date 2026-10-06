# Kiosk Launcher – מדריך התקנה

פרויקט Android Studio מלא ללאנצ'ר קיוסק (Kotlin, minSdk 26, targeted ל-Android 14).

**יש שני מסלולי הפעלה, באותו קוד בדיוק:**

| | מסלול A – החלפת לאנצ'ר (Home App) | מסלול B – קיוסק קשוח (Device Owner) |
|---|---|---|
| התקנה | build ו-install רגילים, שום דבר מיוחד | דורש מכשיר "נקי" + פקודת `adb dpm set-device-owner` |
| חוסם Recents / Notification Shade / Safe Mode | **לא** | כן |
| ניתן לעקוף ע"י משתמש שמבין אנדרואיד | **כן**, דרך Settings > Default apps > Home | לא, ברמת המערכת |
| מתאים ל... | מכשיר אישי, הגבלה למשתמש לא-טכני | קיוסק ציבורי/עמיד בפני התעקשות |

הקוד זהה בשני המקרים: כל הקריאות ל-Device Policy Manager (`KioskPolicy`) מותנות ב-
`isDeviceOwner` ופשוט "לא עושות כלום" אם המכשיר לא הוגדר כ-Device Owner. כלומר **אפשר להתחיל
במסלול A (הכי פשוט), ולשדרג למסלול B מאוחר יותר בלי לשנות שורת קוד אחת** — רק להריץ את פקודת ה-adb.

## מה כלול
- **KioskAdminReceiver / KioskPolicy** – רלוונטיים רק למסלול B (Device Owner). לא נדרשים למסלול A.
- **PinManager** – שומר PIN (כ-hash מלוח) ורשימת אפליקציות מורשות ב-`EncryptedSharedPreferences`.
- **MainActivity** – הלאנצ'ר שהמשתמש רואה: גריד אפליקציות + סמל הגדרות בפינה השמאלית העליונה שדורש PIN.
- **ManageAppsActivity** – מוגן PIN, מציג צ'קליסט של כל האפליקציות המותקנות לבחירת אילו יוצגו,
  שינוי PIN, וכפתור **"החלף בחזרה ללאנצ'ר המקורי"** (פותח את מסך בחירת ה-Home app של המערכת).
- **BootReceiver** – מפעיל מחדש את הקיוסק לאחר ריסטארט למכשיר.
- **KioskWatchdogService** – מחזיר את האפליקציה לחזית אם היא נהרגת (עובד בשני המסלולים).

**PIN התחלתי (ברירת מחדל): `1234`** — יש לשנות אותו מיד דרך מסך הניהול לאחר ההתקנה הראשונה!

## מסלול A – החלפת לאנצ'ר (הכי פשוט, מומלץ להתחיל כאן)

1. בנה והתקן APK רגיל:
   ```
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
2. לחץ על כפתור ה-Home במכשיר. אנדרואיד יציג דיאלוג "עם מה תרצה לפתוח?" עם רשימת לאנצ'רים —
   בחר את **KioskApp** וסמן "תמיד" / "Always". (אם הדיאלוג לא מופיע כי כבר יש ברירת מחדל, לך
   ל-Settings > Apps > Default apps > Home app ובחר את KioskApp משם.)
3. פתח את האפליקציה, הקש על סמל ההגדרות, הזן `1234`, סמן את אפליקציות הבנקים הרצויות, שמור,
   ושנה PIN.
4. מהרגע הזה, לחיצה על Home תמיד תחזיר לגריד של הקיוסק.

**כדי לצאת/לבטל:** סמל הגדרות → PIN → "החלף בחזרה ללאנצ'ר המקורי" → נבחר את הלאנצ'ר
המקורי (Pixel Launcher / One UI Home / וכו') במסך שנפתח.

**חשוב לזכור** (ראה טבלת ההשוואה למעלה): מי שיודע איך לגשת ל-Settings > Default apps > Home
יכול לשנות בעצמו את ברירת המחדל בלי צורך ב-PIN שלך בכלל. במסלול הזה ה-PIN מגן בעיקר מפני
שימוש בשוגג או ממשתמש לא-טכני, לא מפני מישהו שמתעקש ומכיר אנדרואיד. ה-Recents, ה-notification
shade וה-Quick Settings גם נשארים נגישים כרגיל (לא חסומים).

## מסלול B – קיוסק קשוח (Device Owner)
מסלול זה מוסיף מעל מסלול A הגנות אמיתיות ברמת המערכת: בלי Recents, בלי notification shade,
בלי Safe Mode, בלי Factory Reset מה-Settings. מפורט בהמשך המסמך.

### חסימה קבועה של Settings ולאנצ'רים אחרים
`com.android.settings` (ולאנצ'רים מובנים כמו `com.android.launcher3`) **לעולם לא יכולים להיכנס**
לרשימת האפליקציות המורשות — הם מוגדרים ב-`PERMANENTLY_BLOCKED_PACKAGES` ומסוננים בשלוש שכבות
בלתי-תלויות: הם לא מוצגים בכלל בצ'קליסט של מסך הניהול, `PinManager.setAllowedPackages` מסנן אותם
בזמן שמירה, ו-`KioskPolicy.applyAllowedPackages` מסנן אותם שוב לפני הקריאה בפועל ל-
`setLockTaskPackages`. כלומר גם טעות אנוש במסך הניהול לא יכולה בטעות "לפתוח" גישה ל-Settings.

**זו הסיבה שבמסלול B אין צורך "לחסום את וילון ההתראות" בנפרד מ-Settings**: כש-`enterFullLockdown()`
פעיל (Lock Task + `setStatusBarDisabled`), וילון ההתראות לא נפתח כלל; וגם אם איכשהו היה נפתח,
Settings לא ברשימת האפליקציות המורשות ל-Lock Task, כך שהמערכת פשוט חוסמת מעבר אליו.


כדי שהחסימות (בלי Home, בלי Recents, בלי status bar) יעבדו באמת ברמת המערכת, האפליקציה
**חייבת** להיות מוגדרת כ-Device Owner. זה אפשרי רק אם **אין חשבון גוגל מוגדר** במכשיר.

### אפשרות א' – מכשיר "נקי" לגמרי (מומלץ)
1. עשה Factory Reset למכשיר.
2. **לפני** שמתחברים לחשבון גוגל בתהליך ה-setup, התחבר ב-adb (USB debugging עדיין זמין בשלב הזה
   במכשירים רבים, או השתמש ב-QR provisioning).
3. הרץ:
   ```
   adb shell dpm set-device-owner com.example.kioskapp/.KioskAdminReceiver
   ```

### אפשרות ב' – אם כבר יש חשבון גוגל במכשיר
צריך להסיר את כל חשבונות הגוגל מהמכשיר (הגדרות > חשבונות > הסר), ואז להריץ את אותה פקודת adb.

אימות שהצליח:
```
adb shell dumpsys device_policy | grep "Device Owner"
```

## שלב 3: הגדרת רשימת האפליקציות
1. פתח את האפליקציה במכשיר.
2. הקש על סמל ההגדרות בפינה, הזן `1234`.
3. סמן את אפליקציות הבנקים הרצויות (הן צריכות להיות כבר מותקנות במכשיר).
4. לחץ "שמור", ואז "שינוי PIN" כדי להגדיר קוד קבוע במקום ברירת המחדל.
5. חזור למסך הראשי (כפתור "חזרה") — מרגע זה הקיוסק ננעל אוטומטית (`startLockTask`).

## הגנות שנוספו: Safe Mode, Factory Reset, ו-Watchdog

מרגע ש-`MainActivity` עולה בפעם הראשונה כ-Device Owner, הוא מפעיל אוטומטית:

- **`disableSafeBoot()`** – חוסם כניסה ל-**Safe Mode** לגמרי (`UserManager.DISALLOW_SAFE_BOOT`).
  זו נקודת העקיפה הכי נפוצה לקיוסקים: Safe Mode מכבה את כל אפליקציות הצד השלישי (כולל את
  אפליקציית הקיוסק עצמה) ומחזיר גישה מלאה למכשיר. **זו ההגנה הכי חשובה במצב שבו ה-bootloader
  פתוח**, כי היא סוגרת את דרך העקיפה הכי נגישה שלא דורשת fastboot/מחשב בכלל – רק החזקת כפתור
  Power במסך הכיבוי.
- **`disableFactoryReset()`** – חוסם Factory Reset מתוך תפריט ההגדרות (Settings > System > Reset).
  שים לב: זה לא חוסם Factory Reset שנעשה דרך fastboot/recovery ברמת bootloader – זה עדיין תלוי
  בנעילת ה-bootloader עצמה (`fastboot flashing lock`), שנשארת ההמלצה המרכזית לפני פריסה סופית.
- **`disableAddingUsers()`** – חוסם הוספת משתמש/פרופיל נוסף למכשיר. בלי זה, משתמש טכני יכול
  ליצור פרופיל אנדרואיד חדש (Settings > System > Multiple users) שבו הקיוסק לא פעיל בכלל.

בנוסף, יש מתודה מוכנה נוספת ב-`KioskPolicy` שלא מופעלת אוטומטית (כדי לא לחסום התקנת עדכונים
לאפליקציות הבנק בטעות):
- `disableAppInstallUninstall()` – חוסם התקנה/הסרה של אפליקציות מחוץ למסך הניהול שלך.

### KioskWatchdogService – התאוששות מקריסה
השירות `KioskWatchdogService` מוגדר ב-Manifest עם `android:process=":watchdog"`, כלומר הוא
רץ ב**פרוססס לינוקס נפרד** מהאפליקציה הראשית. המשמעות: אם הפרוססס הראשי קורס (crash, OOM kill
ע"י המערכת, וכו'), ה-watchdog נשאר בחיים.

כל 5 שניות הוא בודק (`ActivityManager.getAppTasks()`) האם קיימת עדיין משימה חיה של האפליקציה;
אם לא – הוא מפעיל מחדש את `MainActivity` באופן מיידי. השירות רץ כ-foreground service עם התראה
קבועה בעדיפות מינימלית (חובה מ-Android 8 ומעלה, ומוצהר עם `foregroundServiceType="specialUse"`
כנדרש ב-Android 14).

הוא מופעל אוטומטית גם מ-`MainActivity.onCreate` וגם מ-`BootReceiver`, כך שהוא תמיד רץ ברקע.

## הגנות נוספות מומלצות (לא כלולות בקוד, ברמת המכשיר/חומרה)
- **נעילת Bootloader**: `fastboot flashing lock` (או `fastboot oem lock`, תלוי ביצרן) —
  מונע פלאשינג/שחזור/recovery מותאם אישית לא מורשה כשיש גישה פיזית למכשיר. יש לעשות זאת
  **אחרי** שהמכשיר סופי ומוכן, כי נעילת bootloader עלולה למחוק את המכשיר.
- בדוק אם יצרן המכשיר הספציפי חוסם USB debugging אוטומטית כברירת מחדל (מומלץ לכבות Developer
  Options + USB debugging לגמרי אחרי שסיימת את כל שלבי ההגדרה).

## הסרה / שחזור המכשיר לשימוש רגיל
```
adb shell dpm remove-active-admin com.example.kioskapp/.KioskAdminReceiver
```
או Factory Reset מלא.
