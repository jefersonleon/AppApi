package br.ulbra.appapi;

// --- IMPORTS QUE ESTAVAM FALTANDO ---
import java.util.List;          // Para o Java entender o List<>

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;          // Para o Retrofit entender o Call<>
import retrofit2.http.GET;      // Para o Retrofit entender o @GET
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

// Define como o Android vai chamar o seu PHP API
public interface ApiService {

    // Note que este método aponta para o seu arquivo PHP que retorna JSON
    @GET("api/get_alunos.php")
    Call<List<Aluno>> listarAlunos();

    // NOVO MÉTODO DE CADASTRO
    @Multipart
    @POST("api/insert_aluno.php")
    Call<ResponseBody> cadastrarAluno(
            @Part("nome") RequestBody nome,
            @Part("email") RequestBody email,
            @Part MultipartBody.Part foto
    );

}