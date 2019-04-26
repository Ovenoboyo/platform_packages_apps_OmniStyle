/*
 *  Copyright (C) 2018 The OmniROM Project
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

import android.content.pm.PackageManager;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.content.Intent;
import android.app.PendingIntent;

import java.util.Calendar;


public static class NightDayUtils {
    private static final String TAG = "NightDayUtils";
    
    public void setSunriseThemeAlarm(int hour, int min, Context context, AlarmManager am) {

        long time = getTime(hour,min);
        //Avoid setting alarm in past
        time = checkTime();

        ComponentName receiver = new ComponentName(context, SunriseThemeAlarm.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        Intent i = new Intent(context, SunriseThemeAlarm.class);

        PendingIntent pi = PendingIntent.getBroadcast(context, 1, i, PendingIntent.FLAG_CANCEL_CURRENT);

        am.setRepeating(AlarmManager.RTC_WAKEUP, time, AlarmManager.INTERVAL_DAY, pi);
    }

    public void setSunsetThemeAlarm(int hour, int min, Context context, AlarmManager am) {

        long time = getTime(hour,min);

        ComponentName receiver = new ComponentName(context, SunsetThemeAlarm.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        Intent j = new Intent(context, SunsetThemeAlarm.class);

        PendingIntent pi = PendingIntent.getBroadcast(context, 2, j, PendingIntent.FLAG_CANCEL_CURRENT);

        am.setRepeating(AlarmManager.RTC_WAKEUP, time, AlarmManager.INTERVAL_DAY, pi);
    }
    
     public void cancelPending(AlarmManager am) {
        Intent cancelSunriseIntent = new Intent(getApplicationContext(), SunriseThemeAlarm.class);
        Intent cancelSunsetIntent = new Intent(getApplicationContext(), SunsetThemeAlarm.class);
        PendingIntent cancelSunrisePendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, cancelSunriseIntent,0);
        PendingIntent cancelSunsetPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 2, cancelSunsetIntent,0);
        am.cancel(cancelSunrisePendingIntent);
        am.cancel(cancelSunsetPendingIntent);
     }
     
    public long getTime(int hour, int min) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        return calendar.getTimeInMillis();
    }
    
    public long checkTime(long time) {
        if (time < System.currentTimeMillis()) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, min);
            time = calendar.getTimeInMillis();
        }

}
