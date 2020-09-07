/*
 * Copyright (C) 2017 Shobhit
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.gdrive;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;

import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.utilities.ThemeUtils;

import java.io.IOException;
import java.util.Collections;

import javax.inject.Inject;

public class GoogleAccountsManager {

    @Nullable
    private HttpTransport transport;
    @Nullable
    private JsonFactory jsonFactory;

    private Intent intentChooseAccount;
    private Context context;
    private GoogleAccountCredential credential;
    private GeneralSharedPreferences preferences;
    private ThemeUtils themeUtils;

    @Inject
    public GoogleAccountsManager(@NonNull Context context) {
        initCredential(context);
    }

    /**
     * This constructor should be used only for testing purposes
     */
    public GoogleAccountsManager(@NonNull GoogleAccountCredential credential,
                                 @NonNull GeneralSharedPreferences preferences,
                                 @NonNull Intent intentChooseAccount,
                                 @NonNull ThemeUtils themeUtils
    ) {
        this.credential = credential;
        this.preferences = preferences;
        this.intentChooseAccount = intentChooseAccount;
        this.themeUtils = themeUtils;
    }

    public boolean isAccountSelected() {
        return credential.getSelectedAccountName() != null;
    }

    @NonNull
    public String getLastSelectedAccountIfValid() {
        Account[] googleAccounts = credential.getAllAccounts();
        String account = (String) preferences.get(GeneralKeys.KEY_SELECTED_GOOGLE_ACCOUNT);

        if (googleAccounts != null && googleAccounts.length > 0) {
            for (Account googleAccount : googleAccounts) {
                if (googleAccount.name.equals(account)) {
                    return account;
                }
            }

            preferences.reset(GeneralKeys.KEY_SELECTED_GOOGLE_ACCOUNT);
        }

        return "";
    }

    public void selectAccount(String accountName) {
        if (accountName != null) {
            preferences.save(GeneralKeys.KEY_SELECTED_GOOGLE_ACCOUNT, accountName);
            credential.setSelectedAccountName(accountName);
        }
    }

    public DriveApi getDriveApi() {
        Drive drive = new Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("ODK-Collect")
                .build();

        return new GoogleDriveApi(drive);
    }

    public String getToken() throws IOException, GoogleAuthException {
        String token = credential.getToken();

        // Immediately invalidate so we get a different one if we have to try again
        GoogleAuthUtil.invalidateToken(context, token);
        return token;
    }

    public Intent getAccountChooserIntent() {
        Account selectedAccount = getAccountPickerCurrentAccount();
        intentChooseAccount.putExtra("selectedAccount", selectedAccount);
        intentChooseAccount.putExtra("overrideTheme", themeUtils.getAccountPickerTheme());
        intentChooseAccount.putExtra("overrideCustomTheme", 0);
        return intentChooseAccount;
    }

    public SheetsApi getSheetsApi() {
        Sheets sheets = new Sheets.Builder(transport, jsonFactory, credential)
                .setApplicationName("ODK-Collect")
                .build();

        return new GoogleSheetsApi(sheets);
    }

    private Account getAccountPickerCurrentAccount() {
        String selectedAccountName = getLastSelectedAccountIfValid();
        if (selectedAccountName.isEmpty()) {
            Account[] googleAccounts = credential.getAllAccounts();
            if (googleAccounts != null && googleAccounts.length > 0) {
                selectedAccountName = googleAccounts[0].name;
            } else {
                return null;
            }
        }
        return new Account(selectedAccountName, "com.google");
    }

    private void initCredential(@NonNull Context context) {
        this.context = context;

        transport = AndroidHttp.newCompatibleTransport();
        jsonFactory = JacksonFactory.getDefaultInstance();
        preferences = GeneralSharedPreferences.getInstance();

        credential = GoogleAccountCredential
                .usingOAuth2(context, Collections.singletonList(DriveScopes.DRIVE))
                .setBackOff(new ExponentialBackOff());

        intentChooseAccount = credential.newChooseAccountIntent();
        themeUtils = new ThemeUtils(context);
    }
}
