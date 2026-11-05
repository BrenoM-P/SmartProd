package br.com.smartprod.usuario;

import java.time.Instant;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    /** O e-mail é sempre gravado e consultado sem espaços nas pontas e em minúsculas. */
    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    /** RF002: edição dos dados cadastrais. O e-mail é normalizado como no cadastro. */
    public void atualizar(String nome, String email, Perfil perfil) {
        this.nome = nome;
        this.email = normalizarEmail(email);
        this.perfil = perfil;
    }

    /** Recebe o hash BCrypt já calculado; a senha em texto nunca chega à entidade. */
    public void alterarSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    /** RF002: usuários são inativados, nunca removidos. */
    public void inativar() {
        this.ativo = false;
    }
}
