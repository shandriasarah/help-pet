package com.example.helppet

import org.junit.Assert.assertEquals
import org.junit.Test

class FormValidatorTest {

    @Test
    fun `quando o nome esta vazio, deve retornar falso`() {
        val resultado = FormValidator.validarNomePet("")
        assertEquals(false, resultado)
    }

    @Test
    fun `quando o nome tem apenas um espaco, deve retornar falso`() {
        val resultado = FormValidator.validarNomePet("   ")
        assertEquals(false, resultado)
    }

    @Test
    fun `quando o nome e valido, deve retornar verdadeiro`() {
        val resultado = FormValidator.validarNomePet("Rex")
        assertEquals(true, resultado)
    }

    @Test
    fun `quando o nome tem so uma letra, deve retornar falso`() {
        val resultado = FormValidator.validarNomePet("R")
        assertEquals(false, resultado)
    }

    @Test
    fun `quando o telefone tem menos de 9 digitos, deve retornar falso`() {
        val resultado = FormValidator.validarTelefone("12345")
        assertEquals(false, resultado)
    }

    @Test
    fun `quando o telefone e valido, deve retornar verdadeiro`() {
        val resultado = FormValidator.validarTelefone("81999999999")
        assertEquals(true, resultado)
    }

    @Test
    fun `senha sem numero nao e valida e diz o que falta`() {
        assertEquals(false, FormValidator.senhaValida("abcdefg"))
        assertEquals("A senha precisa ter pelo menos um número", FormValidator.erroSenha("abcdefg"))
    }

    @Test
    fun `senha curta diz que precisa de 6 caracteres`() {
        assertEquals("A senha precisa ter pelo menos 6 caracteres", FormValidator.erroSenha("ab1"))
    }

    @Test
    fun `senha sem letra diz que precisa de letra`() {
        assertEquals("A senha precisa ter pelo menos uma letra", FormValidator.erroSenha("123456"))
    }

    @Test
    fun `senha com letras e numeros e valida`() {
        assertEquals(true, FormValidator.senhaValida("helppet1"))
        assertEquals(null, FormValidator.erroSenha("helppet1"))
    }

    @Test
    fun `email valido e invalido`() {
        assertEquals(true, FormValidator.validarEmail("ana@email.com"))
        assertEquals(false, FormValidator.validarEmail("ana@email"))
        assertEquals(false, FormValidator.validarEmail("anaemail.com"))
    }

    @Test
    fun `telefone com DDD precisa de 10 ou 11 digitos`() {
        assertEquals(true, FormValidator.validarTelefoneComDdd("81999998888"))
        assertEquals(true, FormValidator.validarTelefoneComDdd("8133334444"))
        assertEquals(false, FormValidator.validarTelefoneComDdd("999998888"))
    }
}
