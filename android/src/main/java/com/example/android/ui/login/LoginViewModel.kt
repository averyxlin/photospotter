package com.example.android.ui.login

import AuthManager
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.R
import com.example.android.data.LoginRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class LoginViewModel(private val loginRepository: LoginRepository, private val context: Context) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun login(username: String, password: String) {
        // can be launched in a separate asynchronous job
        auth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("LoginStatus", "Login Success")
                    val user = auth.currentUser
                    val authId = user?.uid ?: ""
                    GlobalScope.launch(Dispatchers.IO) {
                        val userId = getUserByAuthIdRequest(authId) ?: 1
                        withContext(Dispatchers.Main) {
                            AuthManager.setAuthIdAndUserId(context, authId, userId)
                            _loginResult.value = LoginResult(success = LoggedInUserView(displayName = user?.displayName ?: ""))
                        }
                    }
                } else {
                    Log.d("LoginStatus", "Login Fail")
                    // If sign in fails, display a message to the user.
                    _loginResult.value = LoginResult(error = R.string.login_failed)
                }
            }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

    private suspend fun getUserByAuthIdRequest(authId: String?): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/auth/login/$authId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                // Get response code
                val responseCode = connection.responseCode

                // Read response body
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                // Parse JSON response and return name field
                val jsonObject = Gson().fromJson(response.toString(), Map::class.java)
                val id = (jsonObject?.get("id") as? Double)?.toInt()
                Log.d("currentUserId", "$responseCode")
                Log.d("currentUserId", "$jsonObject")
                Log.d("currentUserId", "$id")
                id
            } catch (e: Exception) {
                null
            }
        }
    }
}