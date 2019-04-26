/*
 *  Copyright (C) 2018 The OmniROM Project
 *  Copyright (C) 2019 The Project Pearl
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omnistyle;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.omnirom.omnistyle.NightDayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NightDayActivity extends Activity {
    private static final String TAG = "NightDayActivity";
    private static final String PREFS_NAME = "SharedPrefs";
    private static final boolean DEBUG = true;
    private static final String NOTIFICATION_OVERLAY_PRIMARY = "org.omnirom.theme.notification.primary";
    private AlarmManager am;

    private Spinner mAccentSpinnerDay;
    private Spinner mPrimarySpinnerDay;
    private Spinner mNotificationSpinnerDay;
    private Spinner mAccentSpinnerNight;
    private Spinner mPrimarySpinnerNight;
    private Spinner mNotificationSpinnerNight;
    private RadioGroup mTimeSelect;
    private List<ThemeInfo> mAccentOverlays;
    private List<ThemeInfo> mPrimaryOverlays;
    private List<ThemeInfo> mNotificationOverlays;
    public List<String> mAppOverlays;
    private OverlayUtils mOverlayUtils;
    private NightDayUtils mNightDayUtils;
    private List<String> mOverlayComposeDay = new ArrayList<>();
    private List<String> mOverlayComposeNight = new ArrayList<>();
    private String mCurrentAccent;
    private String mCurrentPrimary;
    private String mCurrentNotification;
    private boolean mHasDefaultNotification;

    private int clockDayHour,clockDayMin, clockNightHour, clockNightMin;

    private class ThemeInfo implements Comparable<ThemeInfo> {
        public String mPackageName;
        public String mName;

        @Override
        public int compareTo(ThemeInfo o) {
            return mName.compareTo(o.mName);
        }
    }

    private class OverlayAdapter extends ArrayAdapter<CharSequence> {
        private List<CharSequence> mOverlayNames;
        private List<Integer> mOverlayColors;
        private boolean mWithColor;
        private String mColorResource;
        private boolean mWithDefault;

        public OverlayAdapter(Context context, List<ThemeInfo> overlayList, boolean withDefault,
                              boolean withColor, String colorResource) {
            super(context, R.layout.color_spinner_item);
            mWithColor = withColor;
            mWithDefault = withDefault;
            mColorResource = colorResource;
            int currentPrimaryColor = 0;
            if (mCurrentPrimary != null) {
                currentPrimaryColor = mOverlayUtils.getThemeColor(mCurrentPrimary, "omni_color2");
            }

            mOverlayNames = new ArrayList<>();
            mOverlayColors = new ArrayList<>();

            for (ThemeInfo overlay : overlayList){
                mOverlayNames.add(overlay.mName);
                if (mWithColor) {
                    if (overlay.mPackageName.equals(NOTIFICATION_OVERLAY_PRIMARY)) {
                        // hacky
                        mOverlayColors.add(currentPrimaryColor);
                    } else {
                        int themeColor = mOverlayUtils.getThemeColor(overlay.mPackageName, mColorResource);
                        mOverlayColors.add(themeColor);
                    }
                }
            }
            if (mWithDefault) {
                mOverlayNames.add(0, getResources().getString(R.string.theme_disable));
                if (mWithColor) {
                    mOverlayColors.add(0, 0);
                }
            }
            if (DEBUG) Log.d(TAG, "OverlayAdapter = " + mOverlayNames);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView;
            final ViewHolder holder;

            if (convertView == null) {
                rowView = inflater.inflate(R.layout.color_spinner_item, null);
                holder = new ViewHolder();
                holder.swatch = (LayerDrawable) getDrawable(R.drawable.color_dot);
                holder.label = (TextView) rowView.findViewById(R.id.color_text);
                holder.icon = (ImageView) rowView.findViewById(R.id.color_icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            holder.label.setText(getItem(position));
            if (mWithColor && mOverlayColors.get(position) != 0) {
                GradientDrawable inner = (GradientDrawable) holder.swatch.findDrawableByLayerId(R.id.dot_inner);
                inner.setColor(mOverlayColors.get(position));
                holder.icon.setVisibility(View.VISIBLE);
                holder.icon.setImageDrawable(holder.swatch);
            } else {
                holder.icon.setVisibility(View.INVISIBLE);
            }
            return rowView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rowView = getView(position, convertView, parent);
            return rowView;
        }

        @Override
        public int getCount() {
            return mOverlayNames.size();
        }

        @Override
        public CharSequence getItem(int position) {
            return mOverlayNames.get(position);
        }

        private class ViewHolder {
            TextView label;
            ImageView icon;
            LayerDrawable swatch;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.night_day);
        
        final SharedPreferences.Editor PrefsWriter = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        final SharedPreferences PrefsReader = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (PrefsReader.getBoolean("hints", true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Hints")
                    .setMessage(this.getText(R.string.dialog_text))
                    .setCancelable(false)
                    .setPositiveButton("Cool", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            PrefsWriter.putBoolean("hints", false);
                            dialog.cancel();
                        }
                    });
            AlertDialog disc = builder.create();
            disc.show();
        }

        Intent intent = new Intent();
        String packageName1 = this.getPackageName();
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName1)) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName1));
            this.startActivity(intent);
        }

        am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        mOverlayUtils = new OverlayUtils(this);

        final LinearLayout linear1 = findViewById(R.id.Linear1);
        final RelativeLayout linear2 = findViewById(R.id.Linear2);
        final View divider = findViewById(R.id.divider);
        final View divider3 = findViewById(R.id.divider3);
        Switch themeSwitch = findViewById(R.id.themeSwitch);

        themeSwitch.setChecked(PrefsReader.getBoolean("switch", false));
        if (themeSwitch.isChecked()) {
            linear1.setVisibility(View.VISIBLE);
            linear2.setVisibility(View.VISIBLE);
        }

        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    linear1.setVisibility(View.VISIBLE);
                    linear2.setVisibility(View.VISIBLE);
                    divider.setVisibility(View.VISIBLE);
                    PrefsWriter.putBoolean("switch", true);
                    PrefsWriter.commit();
                } else {
                    linear1.setVisibility(View.GONE);
                    linear2.setVisibility(View.GONE);
                    divider.setVisibility(View.GONE);
                    PrefsWriter.putBoolean("switch", false);
                    PrefsWriter.remove("AccentDay");
                    PrefsWriter.remove("PrimaryDay");
                    PrefsWriter.remove("NotificationDay");
                    PrefsWriter.remove("AccentNight");
                    PrefsWriter.remove("PrimaryNight");
                    PrefsWriter.remove("NotificationNight");
                    PrefsWriter.commit();
                    mNightDayUtils.cancelPending();
                }
            }
        });

        mOverlayComposeDay.add(OverlayUtils.KEY_THEMES_DISABLED);
        mOverlayComposeDay.add(OverlayUtils.KEY_THEMES_DISABLED);
        mOverlayComposeDay.add(OverlayUtils.KEY_THEMES_DISABLED);
        mOverlayComposeNight.add(OverlayUtils.KEY_THEMES_DISABLED);
        mOverlayComposeNight.add(OverlayUtils.KEY_THEMES_DISABLED);
        mOverlayComposeNight.add(OverlayUtils.KEY_THEMES_DISABLED);

        mAccentSpinnerDay = (Spinner) findViewById(R.id.accent_select_day);
        mPrimarySpinnerDay = (Spinner) findViewById(R.id.primary_select_day);
        mNotificationSpinnerDay = (Spinner) findViewById(R.id.notification_select_day);
        mAccentSpinnerNight = (Spinner) findViewById(R.id.accent_select_night);
        mPrimarySpinnerNight = (Spinner) findViewById(R.id.primary_select_night);
        mNotificationSpinnerNight = (Spinner) findViewById(R.id.notification_select_night);
        mTimeSelect = (RadioGroup) findViewById(R.id.time_select);

        final FrameLayout TimeDayFrame = findViewById(R.id.frame_time_button1);
        final FrameLayout TimeNightFrame = findViewById(R.id.frame_time_button2);
        final TextView TimeDay = findViewById(R.id.editext1);
        final TextView TimeNight = findViewById(R.id.editext2);
        TimeDayFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(NightDayActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        clockDayHour = i;
                        clockDayMin = i1;
                        String mins;
                        if (i1 < 10) {
                            mins = "0"+String.valueOf(i1);
                        } else {
                            mins = String.valueOf(i1);
                        }
                        TimeDay.setText(i + ":" + mins);
                    }
                }, PrefsReader.getInt("clockDayHour", 6),PrefsReader.getInt("clockDayMin", 0),true);
                timePickerDialog.show();
            }
        });
        TimeNightFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog1 = new TimePickerDialog(NightDayActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int j, int j1) {
                        clockNightHour = j;
                        clockNightMin = j1;
                        String mins;
                        if (j1 < 10) {
                            mins = "0"+String.valueOf(j1);
                        } else {
                            mins = String.valueOf(j1);
                        }
                        TimeNight.setText(j + ":" + mins);
                    }
                }, PrefsReader.getInt("clockNightHour", 19),PrefsReader.getInt("clockNightMin", 0),true);
                timePickerDialog1.show();
            }
        });

        final LinearLayout lineartime = findViewById(R.id.lineartime);
        mTimeSelect.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId){
                    case R.id.radio0:
                        clockDayHour = 6;
                        clockDayMin = 0;
                        clockNightHour = 19;
                        clockNightMin = 0;
                        lineartime.setVisibility(View.VISIBLE);
                        divider3.setVisibility(View.VISIBLE);
                        break;
                    case R.id.radio1:
                        clockDayHour = 6;
                        clockDayMin = 0;
                        clockNightHour = 19;
                        clockNightMin = 0;
                        lineartime.setVisibility(View.GONE);
                        divider3.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
            }
        });


        mAccentOverlays = new ArrayList();
        final List<String> accentOverlays = new ArrayList();
        accentOverlays.addAll(Arrays.asList(mOverlayUtils.getAvailableThemes(OverlayUtils.OMNI_ACCENT_THEME_PREFIX)));
        if (DEBUG) Log.d(TAG, "accentOverlays = " + accentOverlays);
        for (String packageName : accentOverlays) {
            ThemeInfo ti = new ThemeInfo();
            ti.mPackageName = packageName;
            ti.mName = mOverlayUtils.getPackageLabel(packageName).toString();
            mAccentOverlays.add(ti);
        }
        Collections.sort(mAccentOverlays);

        mPrimaryOverlays = new ArrayList();
        final List<String> primaryOverlays = new ArrayList();
        primaryOverlays.addAll(Arrays.asList(mOverlayUtils.getAvailableThemes(OverlayUtils.OMNI_PRIMARY_THEME_PREFIX)));
        if (DEBUG) Log.d(TAG, "primaryOverlays = " + primaryOverlays);
        for (String packageName : primaryOverlays) {
            ThemeInfo ti = new ThemeInfo();
            ti.mPackageName = packageName;
            ti.mName = mOverlayUtils.getPackageLabel(packageName).toString();
            mPrimaryOverlays.add(ti);
        }
        Collections.sort(mPrimaryOverlays);

        mNotificationOverlays = new ArrayList();
        final List<String> notificationOverlays = new ArrayList();
        notificationOverlays.addAll(Arrays.asList(mOverlayUtils.getAvailableThemes(OverlayUtils.OMNI_NOTIFICATION_THEME_PREFIX)));
        if (DEBUG) Log.d(TAG, "notificationOverlays = " + notificationOverlays);
        for (String packageName : notificationOverlays) {
            ThemeInfo ti = new ThemeInfo();
            ti.mPackageName = packageName;
            ti.mName = mOverlayUtils.getPackageLabel(packageName).toString();
            mNotificationOverlays.add(ti);
        }
        Collections.sort(mNotificationOverlays);

        mAppOverlays = new ArrayList();
        mAppOverlays.addAll(Arrays.asList(mOverlayUtils.getAvailableThemes(OverlayUtils.OMNI_APP_THEME_PREFIX)));
        if (DEBUG) Log.d(TAG, "appOverlays = " + mAppOverlays);

        final List<String> targetPackages = new ArrayList();
        targetPackages.addAll(Arrays.asList(mOverlayUtils.getAvailableTargetPackages(
        OverlayUtils.OMNI_APP_THEME_PREFIX, OverlayUtils.ANDROID_TARGET_PACKAGE)));
        for (String targetPackage : targetPackages) {
            if (DEBUG) Log.d(TAG, "targetPackage = " + mOverlayUtils.getPackageLabel(targetPackage));
        }

        mCurrentAccent = mOverlayUtils.getCurrentTheme(OverlayUtils.OMNI_ACCENT_THEME_PREFIX);
        if (mCurrentAccent != null) {
            mOverlayComposeDay.set(0, mCurrentAccent);
            mOverlayComposeNight.set(0, mCurrentAccent);
        }
        mCurrentPrimary = mOverlayUtils.getCurrentTheme(OverlayUtils.OMNI_PRIMARY_THEME_PREFIX);
        if (mCurrentPrimary != null) {
            mOverlayComposeDay.set(1, mCurrentPrimary);
            mOverlayComposeNight.set(1, mCurrentPrimary);
        }
        mCurrentNotification = mOverlayUtils.getCurrentTheme(OverlayUtils.OMNI_NOTIFICATION_THEME_PREFIX);
        if (mCurrentNotification != null) {
            mOverlayComposeDay.set(2, mCurrentNotification);
            mOverlayComposeNight.set(2, mCurrentNotification);
        }
        mAccentSpinnerDay.setAdapter(new OverlayAdapter(this, mAccentOverlays, true, true, "omni_color5"));
        mAccentSpinnerNight.setAdapter(new OverlayAdapter(this, mAccentOverlays, true, true, "omni_color5"));
        if (mCurrentAccent != null) {
            mAccentSpinnerDay.setSelection(getOverlaySpinnerPosition(mAccentOverlays, PrefsReader.getString("AccentDay", mCurrentAccent)) + 1, false);
            mAccentSpinnerNight.setSelection(getOverlaySpinnerPosition(mAccentOverlays, PrefsReader.getString("AccentNight", mCurrentAccent)) + 1, false);
        } else {
            mAccentSpinnerDay.setSelection(0, false);
            mAccentSpinnerNight.setSelection(0, false);
        }
        mAccentSpinnerDay.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String packageName = OverlayUtils.KEY_THEMES_DISABLED;
                if (position != 0) {
                    packageName = mAccentOverlays.get(position - 1).mPackageName;
                    mCurrentAccent = packageName;
                } else {
                    mCurrentAccent = null;
                }
                mOverlayComposeDay.set(0, packageName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        mAccentSpinnerNight.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String packageName = OverlayUtils.KEY_THEMES_DISABLED;
                if (position != 0) {
                    packageName = mAccentOverlays.get(position - 1).mPackageName;
                    mCurrentAccent = packageName;
                } else {
                    mCurrentAccent = null;
                }
                mOverlayComposeNight.set(0, packageName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mPrimarySpinnerDay.setAdapter(new OverlayAdapter(this, mPrimaryOverlays, true, true, "omni_color2"));
        mPrimarySpinnerNight.setAdapter(new OverlayAdapter(this, mPrimaryOverlays, true, true, "omni_color2"));
        if (mCurrentPrimary != null) {
            mPrimarySpinnerDay.setSelection(getOverlaySpinnerPosition(mPrimaryOverlays, PrefsReader.getString("PrimaryDay", mCurrentPrimary)) + 1, false);
            mPrimarySpinnerNight.setSelection(getOverlaySpinnerPosition(mPrimaryOverlays, PrefsReader.getString("PrimaryNight", mCurrentPrimary)) + 1, false);
        } else {
            mPrimarySpinnerDay.setSelection(0, false);
            mPrimarySpinnerNight.setSelection(0, false);
        }

        mPrimarySpinnerDay.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String packageName = OverlayUtils.KEY_THEMES_DISABLED;
                if (position != 0) {
                    packageName = mPrimaryOverlays.get(position - 1).mPackageName;
                    mCurrentPrimary = packageName;
                } else {
                    mCurrentPrimary = null;
                }
                updateNotificationChoicesDay();
                mOverlayComposeDay.set(1, packageName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        mPrimarySpinnerNight.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String packageName = OverlayUtils.KEY_THEMES_DISABLED;
                if (position != 0) {
                    packageName = mPrimaryOverlays.get(position - 1).mPackageName;
                    mCurrentPrimary = packageName;
                } else {
                    mCurrentPrimary = null;
                }
                updateNotificationChoicesNight();
                mOverlayComposeNight.set(1, packageName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        updateNotificationChoicesDay();
        updateNotificationChoicesNight();

        mNotificationSpinnerDay.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String packageName = OverlayUtils.KEY_THEMES_DISABLED;
                if (mHasDefaultNotification) {
                    if (position != 0) {
                        packageName = mNotificationOverlays.get(position - 1).mPackageName;
                        mCurrentNotification = packageName;
                    } else {
                        mCurrentNotification = null;
                    }
                } else {
                    packageName = mNotificationOverlays.get(position).mPackageName;
                    mCurrentNotification = packageName;
                }
                mOverlayComposeDay.set(2, packageName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mNotificationSpinnerNight.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String packageName = OverlayUtils.KEY_THEMES_DISABLED;
                if (mHasDefaultNotification) {
                    if (position != 0) {
                        packageName = mNotificationOverlays.get(position - 1).mPackageName;
                        mCurrentNotification = packageName;
                    } else {
                        mCurrentNotification = null;
                    }
                } else {
                    packageName = mNotificationOverlays.get(position).mPackageName;
                    mCurrentNotification = packageName;
                }
                mOverlayComposeNight.set(2, packageName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        Button applyButton = (Button) findViewById(R.id.overlay_apply);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getTime(clockNightHour, clockNightMin) > getTime(clockDayHour, clockDayMin)) {
                    //Had cases where strings wern't overwritten
                    PrefsWriter.remove("AccentDay");
                    PrefsWriter.remove("PrimaryDay");
                    PrefsWriter.remove("NotificationDay");
                    PrefsWriter.remove("AccentNight");
                    PrefsWriter.remove("PrimaryNight");
                    PrefsWriter.remove("NotificationNight");
                    PrefsWriter.putString("AccentDay",  mOverlayComposeDay.get(0));
                    PrefsWriter.putString("PrimaryDay", mOverlayComposeDay.get(1));
                    PrefsWriter.putString("NotificationDay", mOverlayComposeDay.get(2));
                    PrefsWriter.putString("AccentNight", mOverlayComposeNight.get(0));
                    PrefsWriter.putString("PrimaryNight", mOverlayComposeNight.get(1));
                    PrefsWriter.putString("NotificationNight", mOverlayComposeNight.get(2));
                    PrefsWriter.putInt("clockDayHour", clockDayHour);
                    PrefsWriter.putInt("clockNightHour", clockNightHour);
                    PrefsWriter.putInt("clockDayMin", clockDayMin);
                    PrefsWriter.putInt("clockNightMin", clockNightMin);
                    PrefsWriter.apply();
                    
                    mNightDayUtils.cancelPending(am);
    
                    long time = System.currentTimeMillis();
                    long timeexpnight = getTime(clockNightHour, clockNightMin);
                    long timeexpday = getTime(clockDayHour, clockDayMin);
                    if(time >= timeexpday && time < timeexpnight) {
                        List<String> allOverlays = new ArrayList<String>();
                        allOverlays.addAll(mOverlayComposeDay);
    
                        if (!mOverlayComposeDay.get(0).equals(OverlayUtils.KEY_THEMES_DISABLED)
                                || !mOverlayComposeDay.get(1).equals(OverlayUtils.KEY_THEMES_DISABLED)) {
                            allOverlays.addAll(mAppOverlays);
                        }
                        mOverlayUtils.enableThemeList(allOverlays);
                    } else {
                        List<String> allOverlays = new ArrayList<String>();
                        allOverlays.addAll(mOverlayComposeNight);
    
                        if (!mOverlayComposeNight.get(0).equals(OverlayUtils.KEY_THEMES_DISABLED)
                                || !mOverlayComposeNight.get(1).equals(OverlayUtils.KEY_THEMES_DISABLED)) {
                            allOverlays.addAll(mAppOverlays);
                        }
                        mOverlayUtils.enableThemeList(allOverlays);
                    }
    
                    mNightDayUtils.setSunriseThemeAlarm(clockDayHour, clockDayMin, this, am);
                    mNightDayUtils.setSunsetThemeAlarm(clockNightHour, clockNightMin, this, am);
                    Toast.makeText(getApplicationContext(), "Themes set successfully!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Time for Theme1 should be less than Time for Theme2", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.hints);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        switch (item.getItemId()) {
            case 0:
                showAlertDialog();
                return true;
            default:
                return false;
        }
    }

    private int getOverlaySpinnerPosition(List<ThemeInfo> overlays, String packageName) {
        int i = 0;
        for (ThemeInfo ti : overlays) {
            if (ti.mPackageName.equals(packageName)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private void updateNotificationChoicesDay() {
        final SharedPreferences PrefsReader = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mHasDefaultNotification = mCurrentPrimary == null;
        mNotificationSpinnerDay.setAdapter(new OverlayAdapter(this, mNotificationOverlays,
                mHasDefaultNotification ? true : false, true, "omni_theme_color"));
        if (mCurrentNotification != null) {
            mNotificationSpinnerDay.setSelection(getOverlaySpinnerPosition(mNotificationOverlays, PrefsReader.getString("NotificationDay", mCurrentNotification)) +
                    (mHasDefaultNotification ? 1 : 0), false);
            mOverlayComposeDay.set(2, mCurrentNotification);
        } else {
            if (mCurrentPrimary == null) {
                mNotificationSpinnerDay.setSelection(0, false);
                mOverlayComposeDay.set(2, OverlayUtils.KEY_THEMES_DISABLED);
                mCurrentNotification = null;
            } else {
                mNotificationSpinnerDay.setSelection(getOverlaySpinnerPosition(mNotificationOverlays, NOTIFICATION_OVERLAY_PRIMARY), false);
                mOverlayComposeDay.set(2, NOTIFICATION_OVERLAY_PRIMARY);
                mCurrentNotification = NOTIFICATION_OVERLAY_PRIMARY;
            }
        }
    }

    private void updateNotificationChoicesNight() {
        final SharedPreferences PrefsReader = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mHasDefaultNotification = mCurrentPrimary == null;
        mNotificationSpinnerNight.setAdapter(new OverlayAdapter(this, mNotificationOverlays,
                mHasDefaultNotification ? true : false, true, "omni_theme_color"));
        if (mCurrentNotification != null) {
            mNotificationSpinnerNight.setSelection(getOverlaySpinnerPosition(mNotificationOverlays, PrefsReader.getString("NotificationNight",mCurrentNotification)) +
                    (mHasDefaultNotification ? 1 : 0), false);
            mOverlayComposeNight.set(2, mCurrentNotification);
        } else {
            if (mCurrentPrimary == null) {
                mNotificationSpinnerDay.setSelection(0, false);
                mOverlayComposeNight.set(2, OverlayUtils.KEY_THEMES_DISABLED);
                mCurrentNotification = null;
            } else {
                mNotificationSpinnerDay.setSelection(getOverlaySpinnerPosition(mNotificationOverlays, NOTIFICATION_OVERLAY_PRIMARY), false);
                mOverlayComposeNight.set(2, NOTIFICATION_OVERLAY_PRIMARY);
                mCurrentNotification = NOTIFICATION_OVERLAY_PRIMARY;
            }
        }
    }


    
    public void showAlertDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.hints_dialog);
        builder.setTitle("Hints")
                .setCancelable(false)
                .setPositiveButton("Cool", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog disc = builder.create();
        disc.show();
    }
}

