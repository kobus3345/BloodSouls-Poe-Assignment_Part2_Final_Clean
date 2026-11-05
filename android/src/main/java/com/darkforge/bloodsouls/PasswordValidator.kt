package com.darkforge.bloodsouls

object PasswordValidator {

    fun isValidPassword(password: String): Boolean {
        val passwordPattern =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$")

        return password.matches(passwordPattern)
    }

}
