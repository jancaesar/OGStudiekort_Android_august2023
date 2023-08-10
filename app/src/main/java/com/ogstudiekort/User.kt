package com.ogstudiekort

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDate

@Parcelize
data class User(
    val authenticated: Boolean,
    val userName: String = "",
    val userClass: String = "",
    val fullUserName: String = "",
    val phone: String? = null,
    val mobilePhone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val zipCode: String? = null,
    val initials: String? = null,
    val cprNr: String = "",
    val barcode: String = "",
    var balance: String = "",
    val photoBase64: String? = null) : Parcelable {
    val birthDate: String
        get() = getBirthDateFromCpr(cprNr)

    companion object {

        fun fromApiResponse(response: String): User? {
            val parts = response.split(";")
            return if (parts.size == 14 && parts[0].equals("True", ignoreCase = true)) {
                User(
                    authenticated = true,
                    userName = parts[1].ifEmpty { "" },
                    userClass = parts[2].ifEmpty { "" },
                    fullUserName = parts[3].ifEmpty { "" },
                    phone = parts[4].takeIf { it.isNotEmpty() },
                    mobilePhone = parts[5].takeIf { it.isNotEmpty() },
                    email = parts[6].takeIf { it.isNotEmpty() },
                    address = parts[7].takeIf { it.isNotEmpty() },
                    zipCode = parts[8].takeIf { it.isNotEmpty() },
                    initials = parts[9].ifEmpty { "" },
                    cprNr = parts[10].ifEmpty { "" },
                    barcode = parts[11].ifEmpty { "" },
                    balance = parts[12].ifEmpty { "" },
                    photoBase64 = parts[13].takeIf { it.isNotEmpty() }
                )
            } else null
        }


        fun getBirthDateFromCpr(cpr: String): String {
            val day = cpr.substring(0, 2)
            val month = cpr.substring(2, 4)
            val year = cpr.substring(4, 6)
            val currentYearInYYFormat = LocalDate.now().year.toString().substring(2, 4)

            val monthNames = arrayOf("januar", "februar", "marts", "april", "maj", "juni", "juli", "august", "september", "oktober", "november", "december")

            val fullYear = if (year.toInt() > currentYearInYYFormat.toInt()) {
                "19$year"
            } else {
                "20$year"
            }

            return day + ". " + "${monthNames[month.toInt() - 1]} $fullYear"
        }


        fun emptyUser(): User {
            return User(
                authenticated = false,
                userName="",
                userClass = "",
                fullUserName ="",
                phone = null,
                mobilePhone = null,
                email = null,
                address = null,
                zipCode = null,
                initials = null,
                cprNr = "",
                barcode = "",
                balance = "",
                photoBase64 = null
            )
        }
    }
}