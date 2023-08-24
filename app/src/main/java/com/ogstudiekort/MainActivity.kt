package com.ogstudiekort

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.util.Base64
import android.graphics.BitmapFactory
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Callback
import retrofit2.Response
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import retrofit2.Call

class MainActivity : AppCompatActivity() {

    private lateinit var spinner: ProgressBar
    private lateinit var user: User


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinner = findViewById(R.id.progressBar)
        spinner.visibility = View.GONE

        user = intent.parcelable("user") ?: User.emptyUser()

        if (user.authenticated) {
            displayUserInfo(user)
        } else {
            // Handle not authenticated user or error
        }

        val refreshSaldoButton: Button = findViewById(R.id.refresh_saldo)
        refreshSaldoButton.setOnClickListener {
            if (user.authenticated) {
                updateSaldo(user.userName)
            }
        }

        val btnTopUp: Button = findViewById(R.id.pay_button)
        btnTopUp.setOnClickListener {
            val balanceDouble = user.balance.replace(" ", "").replace(",", ".").toDoubleOrNull()
                ?: 100.0
            val intent = Intent(this, TopUpActivity::class.java)
            intent.putExtra("currentBalance", balanceDouble)
            intent.putExtra("fullname", user.fullUserName)
            intent.putExtra("username", user.userName)
            getResult.launch(intent)
        }


    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK){
                displayUserInfo(user)
            }
        }

    override fun onResume() {
        super.onResume()
        // When the activity becomes visible again, cancel auto-logout.
        logoutHandler.removeCallbacks(logoutRunnable)
    }

    override fun onPause() {
        super.onPause()
        // Schedule automatic logout after 5 minutes of inactivity.
        logoutHandler.postDelayed(logoutRunnable, 300000) // 5*60*1000 = 300000 ms = 5 minutes
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        // Each time the user interacts with the app, reset the timer.
        logoutHandler.removeCallbacks(logoutRunnable)
        logoutHandler.postDelayed(logoutRunnable, 300000)
    }


    private fun displayUserInfo(user: User) {
        val userNameTextView: TextView = findViewById(R.id.userNameTextView)
        val userClassTextView: TextView = findViewById(R.id.userClassTextView)
        val userBirthdayTextView: TextView = findViewById(R.id.userBirthdayTextView)
        val balanceTextView: TextView = findViewById(R.id.balanceTextView)
        val userImageView: ImageView = findViewById(R.id.userImageView)
        val imageUserBarcode: ImageView = findViewById(R.id.imageUserBarcode)
        userNameTextView.text = user.fullUserName
        userClassTextView.text = getString(R.string.user_class_format, user.userClass)
        userBirthdayTextView.text = getString(R.string.user_birthday, user.birthDate)
        balanceTextView.text = getString(R.string.user_balance,user.balance)

        if (!user.photoBase64.isNullOrEmpty()) {
            val imageBytes = Base64.decode(user.photoBase64, Base64.DEFAULT)
            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            userImageView.setImageBitmap(Bitmap.createScaledBitmap(decodedImage, 300,400,false))
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        if (user.barcode.isNotEmpty()) {
            val bitMatrix = MultiFormatWriter().encode(
                user.barcode,
                BarcodeFormat.CODE_128,
                screenWidth,
                300
            )
            val bitmap = BarcodeEncoder().createBitmap(bitMatrix)

            imageUserBarcode.setImageBitmap(bitmap)
        }




        val logoutButton: Button = findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            logoutHandler.removeCallbacks(logoutRunnable)
            resetUserData()
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish() // Terminates MainActivity so the user can't go back to it after logging out
        }
    }

    private fun updateSaldo(username: String) {
        spinner.visibility = View.VISIBLE
        RetrofitClient.instance.getSaldo(username).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                spinner.visibility = View.GONE
                if (response.isSuccessful) {
                    val saldo = response.body()?.trim()
                    val balanceTextView: TextView = findViewById(R.id.balanceTextView)
                    balanceTextView.text = getString(R.string.user_balance,saldo)
                } else {
                    // Handle errors, e.g. display a toast with an error message
                    Toast.makeText(this@MainActivity, "Fejl ved opdatering af saldo", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                spinner.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Netværksfejl", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun resetUserData() {
        val userNameTextView: TextView = findViewById(R.id.userNameTextView)
        val userClassTextView: TextView = findViewById(R.id.userClassTextView)
        val userBirthdayTextView: TextView = findViewById(R.id.userBirthdayTextView)
        val balanceTextView: TextView = findViewById(R.id.balanceTextView)
        val userImageView: ImageView = findViewById(R.id.userImageView)

        userNameTextView.text = ""
        userClassTextView.text = ""
        userBirthdayTextView.text = ""
        balanceTextView.text = ""
        userImageView.setImageDrawable(null)
    }


    private val logoutHandler = Handler(Looper.getMainLooper())
    private val logoutRunnable = Runnable {
        resetUserData()
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    private inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
        SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }


}
