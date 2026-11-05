package com.darkforge.bloodsouls

object EmailValidator {
    fun isValidEmail(email:String): Boolean{
        return email.contains("@") && email.contains("gmail") && email.contains(".") && email.contains("com")
            || email.contains("@") && email.contains("hotmail") && email.contains(".") && email.contains("com")
            || email.contains("@") && email.contains("yahoo") && email.contains(".") && email.contains("com")
    }
}
