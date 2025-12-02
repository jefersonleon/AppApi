package br.ulbra.appapi;

public class Aluno {
    private int id;
    private String nome;
    private String email;

    // --- AGORA SIM, OS MÉTODOS REAIS ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    // O nome da variável tem que ser igual ao que vem no JSON da API ('foto_url')
    @com.google.gson.annotations.SerializedName("foto_url")
    private String fotoUrl;

    public String getFotoUrl() {
        return fotoUrl;
    }

}