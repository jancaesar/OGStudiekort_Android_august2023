package com.ogstudiekort

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    lateinit var biometricLoginHelper: BiometricLoginHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val biometricLoginButton = findViewById<ImageButton>(R.id.biometricLoginButton)
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        biometricLoginHelper = BiometricLoginHelper(this)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            authenticateUser(username, password)
        }

        if (biometricLoginHelper.isBiometricAvailable() && biometricLoginHelper.isUserDataSaved()) {
            biometricLoginButton.visibility = View.VISIBLE
            biometricLoginButton.setOnClickListener {
                validateBiometricLogin {
                    val userData = biometricLoginHelper.retrieveUserDataFromPreferences()
                    if (userData != null) {
                        val (username, password) = userData
                        authenticateUser(username, password)
                    } else {
                        Toast.makeText(this, "Kunne ikke hente brugeroplysninger", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            biometricLoginButton.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun validateBiometricLogin(successCallback: () -> Unit) {
        biometricLoginHelper.authenticateUserWithBiometric(
            success = {
                successCallback.invoke()
            },
            failure = {
                Toast.makeText(this, "Biometrisk validering mislykkedes", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun authenticateUser(username: String, password: String) {
        val loginProgress = findViewById<ProgressBar>(R.id.loginProgress)
        loginProgress.visibility = View.VISIBLE

        RetrofitClient.instance.authenticateUser(username, password).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                loginProgress.visibility = View.GONE

                if (response.isSuccessful) {
                    val user = User.fromApiResponse(response.body() ?: "")
                    if (user != null && user.authenticated) {
                        biometricLoginHelper.saveUserDataToPreferences(username, password)

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("user", user)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Forkert brugernavn og/eller adgangskode", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    //Log.e("LoginActivity", "Error response from server: ${response.code()} - ${response.message()}")
                    Toast.makeText(this@LoginActivity, "Forkert brugernavn og/eller adgangskode", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                loginProgress.visibility = View.GONE
                //Log.e("LoginActivity", "Network error: ${t.localizedMessage}", t)
            }
        })
    }
}
