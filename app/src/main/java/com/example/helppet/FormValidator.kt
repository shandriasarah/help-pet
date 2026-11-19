package com.example.helppet

object FormValidator {

    private val REGEX_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validarNomePet(nome: String): Boolean {
        return nome.trim().isNotEmpty() && nome.trim().length >= 2
    }

    fun validarTelefone(telefone: String): Boolean {
        val apenasNumeros = telefone.filter { it.isDigit() }
        return apenasNumeros.length >= 9
    }

    /** Telefone brasileiro com DDD: 10 (fixo) ou 11 (celular) digitos. */
    fun validarTelefoneComDdd(telefone: String): Boolean {
        return telefone.filter { it.isDigit() }.length in 10..11
    }

    fun validarEmail(email: String): Boolean {
        return REGEX_EMAIL.matches(email.trim())
    }

    /** Lista de regras da senha e se cada uma ja foi cumprida (usada no checklist do cadastro). */
    fun requisitosSenha(senha: String): List<Pair<String, Boolean>> = listOf(
        "Pelo menos 6 caracteres" to (senha.length >= 6),
        "Pelo menos uma letra" to senha.any { it.isLetter() },
        "Pelo menos um número" to senha.any { it.isDigit() }
    )

    fun senhaValida(senha: String): Boolean = requisitosSenha(senha).all { it.second }

    /** Mensagem especifica do que falta na senha, ou null se ela estiver valida. */
    fun erroSenha(senha: String): String? = when {
        senha.isEmpty() -> "Digite uma senha"
        senha.length < 6 -> "A senha precisa ter pelo menos 6 caracteres"
        senha.none { it.isLetter() } -> "A senha precisa ter pelo menos uma letra"
        senha.none { it.isDigit() } -> "A senha precisa ter pelo menos um número"
        else -> null
    }
}
