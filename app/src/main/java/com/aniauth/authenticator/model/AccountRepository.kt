package com.aniauth.authenticator.model

import android.content.Context
import com.aniauth.authenticator.crypto.KeyStoreHelper

class AccountRepository(context: Context) {
    
    private val prefs = context.getSharedPreferences("ani_auth_prefs", Context.MODE_PRIVATE)
    
    fun getAccounts(): List<Account> {
        val json = prefs.getString("accounts_list", "[]") ?: "[]"
        return AccountSerializer.fromJson(json)
    }
    
    fun saveAccounts(accounts: List<Account>) {
        val json = AccountSerializer.toJson(accounts)
        prefs.edit().putString("accounts_list", json).apply()
    }
    
    fun addAccount(label: String, secret: String, issuer: String?, username: String? = null) {
        val encryptedSecret = KeyStoreHelper.encrypt(secret)
        val newAccount = Account(
            label = label,
            encryptedSecret = encryptedSecret,
            issuer = issuer,
            username = username
        )
        val current = getAccounts().toMutableList()
        current.add(newAccount)
        saveAccounts(current)
    }
    
    fun updateAccount(id: String, label: String, username: String?) {
        val current = getAccounts().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(label = label, username = username)
            saveAccounts(current)
        }
    }

    fun deleteAccount(accountId: String) {
        val current = getAccounts().toMutableList()
        current.removeAll { it.id == accountId }
        saveAccounts(current)
    }
}
