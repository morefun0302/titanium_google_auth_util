/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package com.sitata.googleauthutil;

import java.util.HashMap;
import java.util.logging.Logger;

import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiActivityResultHandler;
import org.appcelerator.titanium.util.TiActivitySupport;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.sitata.googleauthutil.FetchUserToken.UserTokenHandler;

@Kroll.module(name = "TitaniumGoogleAuthUtil", id = "com.sitata.googleauthutil")
public class TitaniumGoogleAuthUtilModule extends KrollModule implements
		TiActivityResultHandler, UserTokenHandler
{
	static final int REQUEST_CODE_PICK_ACCOUNT = 1000;

	// Standard Debugging variables
	private static final String TAG = "TiGoogleAuthUtilModule";
	protected int requestCode;
	protected int recoveryRequestCode;
	protected KrollFunction resultCallback;
	// private static final String SCOPE =
	// "oauth2:https://www.googleapis.com/auth/userinfo.profile";
	// private static final String SCOPE = "oauth2:profile email";
	String mScope;
	String mEmail;

	// picker is closed without choosing an account
	@Kroll.constant
	public static final String PICKER_CLOSED = "tgau:pickerClosed";
	// could be network is down
	@Kroll.constant
	public static final String IO_EXCEPTION = "tgau:ioException";
	// If user intervention is required to provide consent, enter a password,
	// etc, a UserRecoverableAuthException will be thrown
	@Kroll.constant
	public static final String USER_RECOVERABLE_EXCEPTION = "tgau:recoverException";
	// Some other type of unrecoverable exception has occurred.
	// Report and log the error as appropriate for your app.
	@Kroll.constant
	public static final String FATAL_EXCEPTION = "tgau:fatalException";
	// Error when launching activity for picker
	@Kroll.constant
	public static final String ACTIVITY_ERROR = "tgau:activityError";
	// No email given so we can't fetch token
	@Kroll.constant
	public static final String NO_EMAIL = "tgau:noEmailError";

	public TitaniumGoogleAuthUtilModule()
	{
		super();
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		Logger.getLogger(TAG).info("inside onAppCreate");
	}

	// Methods
	@Kroll.method
	public void pickUserAccount(KrollFunction handler)
	{
		this.resultCallback = handler;

		String[] accountTypes = new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE };
		Intent intent = AccountPicker.newChooseAccountIntent(null, null,
				accountTypes, false, null, null, null, null);

		Activity activity = TiApplication.getAppCurrentActivity();
		TiActivitySupport support = (TiActivitySupport) activity;
		requestCode = support.getUniqueResultCode();
		support.launchActivityForResult(intent, requestCode, this);

	}

	@Kroll.getProperty @Kroll.method
	public void setScope(String value)
	{
		mScope = value;
	}

	@Kroll.getProperty @Kroll.method
	public String getScope() {
		return mScope;
	}

	@Override
	public void onError(Activity activity, int requestCode, Exception e) {
		Logger.getLogger(TAG).info("ON ERROR called.");
		handleError(ACTIVITY_ERROR);
	}

	@Override
	public void onResult(Activity activity, int thisRequestCode,
			int resultCode, Intent data) {
		Logger.getLogger(TAG).info(
				"On Result - Request Code is:" + thisRequestCode);

		if (resultCallback == null)
			return;

		if (thisRequestCode == requestCode) {
			Logger.getLogger(TAG).info(
					"Handling Fetch User Token Request Code.");
			// Receiving a result from the AccountPicker
			if (resultCode == Activity.RESULT_OK) {

				mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				// With the account name acquired, go get the auth token
				getUserToken();
			} else if (resultCode == Activity.RESULT_CANCELED) {
				// The account picker dialog closed without selecting an
				// account.
				handleError(PICKER_CLOSED);
			}
		} else if (thisRequestCode == recoveryRequestCode) {
			Logger.getLogger(TAG).info("Handling Recovery Request Result.");
			if (resultCode == Activity.RESULT_OK) {
				Bundle extra = data.getExtras();
				String oneTimeToken = extra.getString("authtoken");
				handleToken(mEmail, oneTimeToken);
			}
		}
	}

	@Override
	public void handleToken(String email, String token) {
		HashMap<String, String> event = new HashMap<String, String>();
		event.put("email", mEmail);
		event.put("token", token);
		resultCallback.call(getKrollObject(), event);
	}

	@Override
	public void handleTokenError(String errorCode) {
		handleError(errorCode);
	}

	@Override
	public void handleRecoverableException(Intent recoveryIntent) {
		Logger.getLogger(TAG).info("Launchng recoverable intent.");

		// Use the intent in a custom dialog or just startActivityForResult.
		Activity activity = TiApplication.getAppCurrentActivity();
		TiActivitySupport support = (TiActivitySupport) activity;
		recoveryRequestCode = support.getUniqueResultCode();
		support.launchActivityForResult(recoveryIntent, recoveryRequestCode,
				this);

	}

	// Google play is probably not available
	// TODO: Test this...
	@Override
	public void handleGooglePlayException(
			GooglePlayServicesAvailabilityException playEx) {
		// Use the dialog to present to the user.
		Activity activity = TiApplication.getAppCurrentActivity();
		Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
				playEx.getConnectionStatusCode(), activity, requestCode);
		dialog.show();
	}

	private void getUserToken() {
		if (mEmail == null) {
			handleError(NO_EMAIL);
		} else {
			FetchUserToken ft = new FetchUserToken(TiApplication.getAppCurrentActivity(), mEmail,
					mScope, this);
			ft.execute();
		}
	}

	private void handleError(String code) {
		HashMap<String, String> event = new HashMap<String, String>();
		event.put("error", code);
		resultCallback.call(getKrollObject(), event);
	}


}

