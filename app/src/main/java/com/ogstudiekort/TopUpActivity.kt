package com.ogstudiekort

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import eu.nets.mia.MiASDK
import eu.nets.mia.data.MiAPaymentInfo
import eu.nets.mia.data.MiAResult
import eu.nets.mia.data.MiAResultCode
import retrofit2.Call
import java.net.URLEncoder

class TopUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_up)

        val currentBalance = intent.getDoubleExtra("currentBalance", 0.0)
        val fullname = intent.getStringExtra("fullname") ?: ""
        val username = intent.getStringExtra("username") ?: ""


        val etAmount: EditText = findViewById(R.id.etAmount)
        val btn100dkk: Button = findViewById(R.id.btn100dkk)
        val btn200dkk: Button = findViewById(R.id.btn200dkk)
        val btn300dkk: Button = findViewById(R.id.btn300dkk)
        val btnTopUp: Button = findViewById(R.id.btnTopUp)

        btn100dkk.setOnClickListener { etAmount.setText("100") }
        btn200dkk.setOnClickListener { etAmount.setText("200") }
        btn300dkk.setOnClickListener { etAmount.setText("300") }

        btnTopUp.setOnClickListener {
            val amount = etAmount.text.toString().toDoubleOrNull()
            if (amount != null) {
                if (amount >= 1 && (amount + currentBalance) <= 1000) {

                    // Indkod fullname
                    val encodedFullname = URLEncoder.encode(fullname, "UTF-8") // Erstat 'fullname' med den faktiske værdi.

                    // Udfør anmodningen
                    val call = PaymentRetrofitClient.instance.initiatePayment(username, encodedFullname, amount)
                    call.enqueue(object: retrofit2.Callback<String> {
                        override fun onResponse(call: Call<String>, response: retrofit2.Response<String>) {
                            if (response.isSuccessful) {
                                val getResponse = response.body() ?: return
                                val paymentId = getResponse.split("#")[0]
                                val checkoutUrl = getResponse.split("#")[1]
                                val returnUrl = "com.ogstudiekort://payment"
                                val cancelUrl = "com.ogstudiekort://payment"

                                // Start MiA Nets Easy SDK her
                                MiASDK.getInstance().startSDK(this@TopUpActivity, MiAPaymentInfo(paymentId, checkoutUrl, returnUrl, cancelUrl))
                            } else {
                                // Handle error
                                Toast.makeText(this@TopUpActivity, "Error initiating payment", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<String>, t: Throwable) {
                            // Handle error
                            Toast.makeText(this@TopUpActivity, "Network error", Toast.LENGTH_SHORT).show()
                        }
                    })

                } else if (amount + currentBalance > 1000) {
                    Toast.makeText(this, "Samlet saldo må ikke overstige 1000 DKK", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Beløbet skal være mindst 100 DKK", Toast.LENGTH_SHORT).show()
                }
            }
        }


        val btnMainActivity: Button = findViewById(R.id.btnBack)
        btnMainActivity.setOnClickListener {
            val returnIntent = Intent()
            returnIntent.putExtra("updatedBalance", "ny saldo her som string")
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MiASDK.EASY_SDK_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val result = data?.getParcelableExtra<MiAResult>(MiASDK.BUNDLE_COMPLETE_RESULT)
                when (result?.miaResultCode) {
                    MiAResultCode.RESULT_PAYMENT_COMPLETED -> {
                        Toast.makeText(this, "Din betaling er gennemført!", Toast.LENGTH_SHORT).show()
                    }
                    MiAResultCode.RESULT_PAYMENT_CANCELLED -> {
                        AlertDialog.Builder(this)
                            .setTitle("Betalingsstatus")
                            .setMessage("Betalingsprocessen blev annulleret.")
                            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                    MiAResultCode.RESULT_PAYMENT_FAILED -> {
                        AlertDialog.Builder(this)
                            .setTitle("Betalingsstatus")
                            .setMessage("Der opstod en fejl under betalingen. Prøv igen.")
                            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                    else -> { }
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }



}
