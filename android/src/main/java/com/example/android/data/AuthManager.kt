import android.content.Context
import android.preference.PreferenceManager

object AuthManager {
    private const val AUTH_ID_KEY = "auth_id"
    private const val USER_ID_KEY = "user_id"

    fun setAuthIdAndUserId(context: Context, authId: String, userId: Int) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit()
            .putString(AUTH_ID_KEY, authId)
            .putInt(USER_ID_KEY, userId)
            .apply()
    }

    fun getAuthId(context: Context): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(AUTH_ID_KEY, null)
    }

    fun getUserId(context: Context): Int? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val userId = preferences.getInt(USER_ID_KEY, -1) // -1 if not found
        return if (userId == -1) null else userId
    }

    fun clearAuthIdAndUserId(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit()
            .remove(AUTH_ID_KEY)
            .remove(USER_ID_KEY)
            .apply()
    }
}
