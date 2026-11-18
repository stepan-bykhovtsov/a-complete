package dev.bykhovtsov.acomplete.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object ACompleteCredentials {
    private const val SUBSYSTEM = "AComplete"
    private const val KEY = "OpenRouterApiKey"

    private val credentialAttributes: CredentialAttributes
        get() = CredentialAttributes(generateServiceName(SUBSYSTEM, KEY))

    fun getApiKey(): String? {
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }

    fun setApiKey(apiKey: String?) {
        PasswordSafe.instance.setPassword(credentialAttributes, apiKey?.trim()?.takeIf { it.isNotEmpty() })
    }

    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }
}
