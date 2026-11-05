package com.darkforge.bloodsouls

import org.junit.Assert
import org.junit.Test

class PasswordValidatorUnitTests {

    @Test
    fun whenPasswordIsValid_shouldReturnTrue() {
        val password = "Password1!"
        val result = PasswordValidator.isValidPassword(password)

        Assert.assertTrue(result)
    }

    @Test
    fun whenPasswordHasNoUppercase_shouldReturnFalse() {
        val password = "password1!"
        val result = PasswordValidator.isValidPassword(password)

        Assert.assertFalse(result)
    }

    @Test
    fun whenPasswordHasNoLowercase_shouldReturnFalse() {
        val password = "PASSWORD1!"
        val result = PasswordValidator.isValidPassword(password)

        Assert.assertFalse(result)
    }

    @Test
    fun whenPasswordHasNoNumber_shouldReturnFalse() {
        val password = "Password!"
        val result = PasswordValidator.isValidPassword(password)

        Assert.assertFalse(result)
    }

    @Test
    fun whenPasswordHasNoSpecialCharacter_shouldReturnFalse() {
        val password = "Password1"
        val result = PasswordValidator.isValidPassword(password)

        Assert.assertFalse(result)
    }
}
