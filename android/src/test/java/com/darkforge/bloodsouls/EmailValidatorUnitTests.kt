package com.darkforge.bloodsouls

import org.junit.Assert
import org.junit.Test

class EmailValidatorUnitTests {

    @Test
    fun whenEmailIsValid_shouldReturnTrue() {
        val gemail = "someone@gmail.com"
        val gresult = EmailValidator.isValidEmail(gemail)
        Assert.assertTrue(gresult)

        val hemail = "someone@hotmail.com"
        val hresult = EmailValidator.isValidEmail(hemail)
        Assert.assertTrue(hresult)

        val yemail = "someone@yahoo.com"
        val yresult = EmailValidator.isValidEmail(yemail)
        Assert.assertTrue(yresult)
    }

    @Test
    fun whenEmailIsInvalid_shouldReturnFalse() {
        val gemail = "someonegmail.com"
        val gresult = EmailValidator.isValidEmail(gemail)
        Assert.assertFalse(gresult)

        val hemail = "someonehotmail.com"
        val hresult = EmailValidator.isValidEmail(hemail)
        Assert.assertFalse(hresult)

        val yemail = "someoneyahoo.com"
        val yresult = EmailValidator.isValidEmail(yemail)
        Assert.assertFalse(yresult)
    }
}
