package com.darkforge.bloodsouls.util

import java.security.MessageDigest

object SecurityUtils {

    fun hashPassword(password: String): String {
        val salt = "BloodSouls_DarkForge_Salt_2025!"
        val saltedPassword = password + salt
        val bytes = MessageDigest.getInstance("SHA-256").digest(saltedPassword.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
