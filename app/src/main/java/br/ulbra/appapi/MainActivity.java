package br.ulbra.appapi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView; // Importante

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerAlunos;
    private AlunoAdapter adapter;
    private List<Aluno> listaAlunos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Configurar o RecyclerView
        recyclerAlunos = findViewById(R.id.recyclerAlunos);
        recyclerAlunos.setLayoutManager(new LinearLayoutManager(this));

        // Já carrega os dados assim que abrir o App
        carregarDados();
        // Dentro do onCreate da MainActivity
        Button btnNovo = findViewById(R.id.btnNovoAluno); // Supondo que você criou no XML

        btnNovo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CadastroActivity.class);
            startActivity(intent);
        });
    }

    private void carregarDados() {
        String urlBase = "https://teste.infinitydev.com.br/"; // Sua URL correta

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(urlBase)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService api = retrofit.create(ApiService.class);
        Call<List<Aluno>> call = api.listarAlunos();

        call.enqueue(new Callback<List<Aluno>>() {
            @Override
            public void onResponse(Call<List<Aluno>> call, Response<List<Aluno>> response) {
                if (response.isSuccessful()) {
                    listaAlunos = response.body();

                    // 2. Cria o Adapter e conecta na lista
                    adapter = new AlunoAdapter(MainActivity.this, listaAlunos);
                    recyclerAlunos.setAdapter(adapter);
                } else {
                    Toast.makeText(MainActivity.this, "Erro: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Aluno>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Falha: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}