/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
 * 
 * This file is part of RxDroid.
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */

package at.caspase.rxdroid;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

public class TimePreference extends DialogPreference implements OnTimeSetListener, OnClickListener, OnSharedPreferenceChangeListener
{
	private static final String TAG = TimePreference.class.getName();
		
	private MyTimePickerDialog mDialog;
			
	private String mAfterTimeKey;
	private String mBeforeTimeKey;
	
	private DumbTime mTime;
				
	private String mDefaultValue = "00:00";
		
	public TimePreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public TimePreference(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);		
				
		// FIXME
		for(int i = 0; i != attrs.getAttributeCount(); ++i)
		{
			final String name = attrs.getAttributeName(i);
			String value = attrs.getAttributeValue(i);
				
			if(name.equals("defaultValue"))
			{
				if(value.charAt(0) == '@')
				{
					final Resources res = context.getResources();										
					value = res.getString(Integer.parseInt(value.substring(1), 10));
				}			
				mDefaultValue = value;
			}
			else if(name.equals("isAfter"))
				mAfterTimeKey = value;
			else if(name.equals("isBefore"))
				mBeforeTimeKey = value;			
		}
		
		Log.d(TAG, "init: after=" + mAfterTimeKey + ", before=" + mBeforeTimeKey);
	}
	
	@Override
	public Dialog getDialog() 
	{
		return mDialog;
	}
	
	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute)
	{	
		// FIXME
		if(!mDialog.checkConstraints(hourOfDay, minute))
		{
			updateTimePicker();
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			builder.setTitle(R.string._title_error);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setMessage(R.string._msg_timepreference_constraint_failed);
			builder.setNeutralButton(android.R.string.ok, this);
			builder.show();
		}
		else
		{			
			final DumbTime time = new DumbTime(hourOfDay, minute, 0);
			
			final String timeString = time.toString();
			mTime = time;
			persistString(timeString);
			setSummary(timeString);
			Log.d(TAG, "onTimeSet: persisting");
			updateTimePicker();
		}		
	}
		
	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == Dialog.BUTTON_NEUTRAL)
		{
			// dialog is the AlertDialog created above. clicking OK should bring back
			// the TimePickerDialog
			mDialog.show();
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key)
	{
		Log.d(TAG, "key=" + key);
		
		if(key.equals(getKey()) || key.equals(mAfterTimeKey) || key.equals(mBeforeTimeKey))
			updateTimePicker();
	}
	
	@Override
	protected void onAttachedToActivity()
	{
		super.onAttachedToActivity();
		
		// getPersistedString returns null in the constructor, so we have to set the summary here
		final String persisted = getPersistedString(mDefaultValue);
		setSummary(persisted);
		mTime = DumbTime.valueOf(persisted);
					
		updateTimePicker();
		
		getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPrepareForRemoval()
	{
		getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void showDialog(Bundle state)
	{
		updateTimePicker();
		getDialog().show();
	}
		
	private void updateTimePicker()
	{
		final boolean is24HourFormat = DateFormat.is24HourFormat(getContext());
		mDialog = new MyTimePickerDialog(getContext(), this, mTime.getHours(), mTime.getMinutes(), is24HourFormat);
		
		mDialog.setConstraintAfter(Settings.INSTANCE.getTimePreference(mAfterTimeKey));
		mDialog.setConstraintBefore(Settings.INSTANCE.getTimePreference(mBeforeTimeKey));		
	}
	
	private static class MyTimePickerDialog extends TimePickerDialog
	{
		private DumbTime mAfter = null;
		private DumbTime mBefore = null;
		
		private int mHourOfDay;
		private int mMinute;
		
		public MyTimePickerDialog(Context context, int theme, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView)
		{
			super(context, theme, callBack, hourOfDay, minute, is24HourView);			
			mHourOfDay = hourOfDay;
			mMinute = minute;
		}
		
		public MyTimePickerDialog(Context context, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) 
		{
			super(context, callBack, hourOfDay, minute, is24HourView);
			mHourOfDay = hourOfDay;
			mMinute = minute;
		}		
		
		public void setConstraintAfter(DumbTime after)
		{
			mAfter = after;
			updateMessage();
		}
		
		public void setConstraintBefore(DumbTime before)
		{
			mBefore = before;
			updateMessage();
		}
		
		@Override
		public void dismiss()
		{
			Log.d(TAG, "dismiss");
			super.dismiss();
		}
		
		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
		{
			Log.d(TAG, "onTimeChanged");
			
			if(checkConstraints(hourOfDay, minute))
				super.onTimeChanged(view, hourOfDay, minute);
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			Log.d(TAG, "onClick");
			super.onClick(dialog, which);
		}
				
		public boolean checkConstraints(int hourOfDay, int minute)
		{
			final DumbTime time = new DumbTime(hourOfDay, minute);
			
			boolean isValid;
			
			if((mAfter != null && time.compareTo(mAfter) == -1) || (mBefore != null && !time.before(mBefore)))
				isValid = false;
			else
				isValid = true;
			
			Log.d(TAG, "checkConstraints: time=" + time + ", enabled=" + isValid);
			
			getButton(BUTTON_POSITIVE).setEnabled(isValid);
			
			return isValid;
		}
		
		private void updateMessage()
		{
			String message = null;
			
			if(mAfter != null && mBefore != null)
			{
				message = "Choose a time after %1 and before %2.";
				message = message.replace("%1", mAfter.toString());
				message = message.replace("%2", mBefore.toString());
			}
			else if(mAfter != null)
			{
				message = "Choose a time after %1.";
				message = message.replace("%1", mAfter.toString());			
			}
			else if(mBefore != null)
			{
				message = "Choose a time before %1.";
				message = message.replace("%1", mBefore.toString());		
			}
			
			setMessage(message);
		}

		
	}
	
}
