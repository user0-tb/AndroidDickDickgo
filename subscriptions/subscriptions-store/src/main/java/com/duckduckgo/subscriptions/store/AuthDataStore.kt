/*
 * Copyright (c) 2023 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.subscriptions.store

import android.content.SharedPreferences
import androidx.core.content.edit

interface AuthDataStore {
    var token: String?
    fun canUseEncryption(): Boolean
}

class AuthEncryptedDataStore(
    private val sharedPrefsProv: SharedPrefsProvider,
) : AuthDataStore {

    private val encryptedPreferences: SharedPreferences? by lazy { encryptedPreferences() }

    @Synchronized
    private fun encryptedPreferences(): SharedPreferences? {
        return sharedPrefsProv.getSharedPrefs(FILENAME)
    }

    override var token: String?
        get() = encryptedPreferences?.getString(KEY_TOKEN, null)
        set(value) {
            encryptedPreferences?.edit(commit = true) {
                if (value == null) {
                    remove(KEY_TOKEN)
                } else {
                    putString(KEY_TOKEN, value)
                }
            }
        }

    override fun canUseEncryption(): Boolean = encryptedPreferences != null

    companion object {
        const val FILENAME = "com.duckduckgo.subscriptions.store"
        const val KEY_TOKEN = "KEY_TOKEN"
    }
}
