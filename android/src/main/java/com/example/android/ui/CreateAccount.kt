package com.example.android.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.android.R
import com.example.android.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class CreateAccount : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var registerButton: Button
    private lateinit var auth: FirebaseAuth

    data class User(
        val email: String,
        val username: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setContentView(R.layout.activity_create_account)

        val backButton = findViewById<ImageView>(R.id.backButtonImageView)
        backButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        email = findViewById(R.id.email)
        username = findViewById(R.id.username2)
        password = findViewById(R.id.password2)
        registerButton = findViewById(R.id.register)
        registerButton.isEnabled = false
        email.addTextChangedListener(textWatcher)
        username.addTextChangedListener(textWatcher)
        password.addTextChangedListener(textWatcher)

        registerButton.setOnClickListener {
            val emailValue = email.text.toString()
            val usernameValue = username.text.toString()
            val passwordValue = password.text.toString()

            if (passwordValue.length < 6) {
                Toast.makeText(baseContext, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailValue, passwordValue)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("createUser", "createUserWithEmail:success")
                        val user = auth.currentUser
                        val uid = user?.uid ?: ""
                        Log.d("New User UID", "$uid")

                        // Create new account in backend
                        createUserRequest(usernameValue, emailValue, uid)

                        val database = FirebaseDatabase.getInstance()
                        val usersRef = database.getReference("users")
                        val userRef = usersRef.child(user?.uid ?: "")
                        val userInformation = User(emailValue, usernameValue)
                        userRef.setValue(userInformation)
                        val intent = Intent(this@CreateAccount, LoginActivity::class.java)
                        startActivity(intent)
                    } else {
                        Log.w("createUser", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Account With Email Already Exists. Please Sign In",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            // Check if both fields are non-empty
            val email = email.text.toString()
            val username = username.text.toString()
            val password = password.text.toString()
            registerButton.isEnabled = email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
        }
    }

    private fun createUserRequest(name: String, email: String, authId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val requestBody = """
                {
                    "name": "$name",
                    "email": "$email",
                    "authId": "$authId"
                }
            """.trimIndent()

                val outputStream: OutputStream = connection.outputStream
                outputStream.write(requestBody.toByteArray())
                outputStream.flush()

                val responseCode = connection.responseCode
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                Log.d("ResponseData", "Response code: $responseCode, Response body: $response")
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("ResponseData", "Error making request: ${e.message}", e)
            }
        }
    }
}