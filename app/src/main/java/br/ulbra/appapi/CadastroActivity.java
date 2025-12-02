package br.ulbra.appapi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CadastroActivity extends AppCompatActivity {

    private ImageView imgPreview;
    private EditText edtNome, edtEmail;
    private Button btnSelecionarFoto, btnSalvar;

    // Variáveis novas para controle da Câmera
    private String currentPhotoPath; // Guarda o caminho onde a foto FULL HD foi salva
    private File arquivoFotoFinal = null; // Guarda o arquivo JÁ REDIMENSIONADO
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PERMISSION_CAMERA = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        // --- RECUPERA O CAMINHO DA FOTO SE O APP TIVER REINICIADO ---
        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString("caminho_foto_recuperado");
        }
        // -------------------------------------------------------------

        imgPreview = findViewById(R.id.imgPreview);
        edtNome = findViewById(R.id.edtNome);
        edtEmail = findViewById(R.id.edtEmail);
        btnSelecionarFoto = findViewById(R.id.btnSelecionarFoto);
        btnSalvar = findViewById(R.id.btnSalvar);

        // Muda o texto do botão
        btnSelecionarFoto.setText("Tirar Foto");

        // AÇÃO 1: Botão agora chama a verificação de permissão
        btnSelecionarFoto.setOnClickListener(v -> verificarPermissaoCamera());

        // AÇÃO 2: Botão Salvar
        btnSalvar.setOnClickListener(v -> enviarCadastro());
    }

    // --- PARTE 1: PERMISSÕES ---
    private void verificarPermissaoCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Se não tem permissão, pede
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
        } else {
            // Se já tem, abre a câmera
            abrirCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamera();
            } else {
                Toast.makeText(this, "Precisamos da câmera para tirar a foto!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- PARTE 2: ABRIR CÂMERA E CRIAR ARQUIVO ---
    private void abrirCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = null;
        try {
            photoFile = criarArquivoImagem();
        } catch (IOException ex) {
            Toast.makeText(this, "Erro ao criar arquivo de foto", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

            // --- A MUDANÇA ESTÁ AQUI ---
            // Removemos o "if resolveActivity" e colocamos um try/catch direto
            try {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (Exception e) {
                // Só entra aqui se o celular não tiver NENHUM app de câmera (raríssimo)
                Toast.makeText(this, "Erro: Não foi possível abrir a câmera.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private File criarArquivoImagem() throws IOException {
        // Cria um nome único para o arquivo
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // Usa a pasta de cache para não poluir a galeria do usuário
        File storageDir = getExternalCacheDir();
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        // Salva o caminho completo para usarmos depois
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // SALVA O CAMINHO DA FOTO ANTES DO APP SER DESTRUÍDO PELA CÂMERA
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentPhotoPath != null) {
            outState.putString("caminho_foto_recuperado", currentPhotoPath);
        }
    }
    // --- PARTE 3: VOLTA DA CÂMERA E REDIMENSIONAMENTO (A MÁGICA) ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // A câmera não retorna os dados no "data". Ela salvou no currentPhotoPath.
            // Agora precisamos pegar aquele arquivo GIGANTE e diminuir.
            processarImagemDaCamera();
        }
    }

    private void processarImagemDaCamera() {
        try {
            // DEFINA AQUI O TAMANHO MÁXIMO (ex: 1024px de largura ou altura)
            // Isso garante que a foto nunca será maior que isso.
            int maxDimension = 1024;

            // 1. Descobre o tamanho real da imagem sem carregar ela na memória (para não travar)
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // 2. Calcula o fator de escala (quantas vezes precisa diminuir)
            int scaleFactor = Math.min(photoW / maxDimension, photoH / maxDimension);
            if (scaleFactor < 1) scaleFactor = 1;

            // 3. Agora carrega a imagem já diminuída na memória
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor; // Define o fator de redução
            Bitmap bitmapReduzido = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

            // 4. Corrige a rotação (Câmeras Samsung/Xiaomi às vezes salvam a foto deitada)
            bitmapReduzido = corrigirRotacao(bitmapReduzido, currentPhotoPath);

            // Mostra na tela
            imgPreview.setImageBitmap(bitmapReduzido);

            // 5. Salva o Bitmap reduzido em um novo arquivo para enviar
            arquivoFotoFinal = new File(getCacheDir(), "foto_reduzida_final.jpg");
            FileOutputStream fos = new FileOutputStream(arquivoFotoFinal);
            // Comprime para JPEG com qualidade 70% (ótimo balanço tamanho/qualidade)
            bitmapReduzido.compress(Bitmap.CompressFormat.JPEG, 70, fos);
            fos.flush();
            fos.close();

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao processar imagem: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("CAMERA_ERRO", e.getMessage());
        }
    }

    // Função auxiliar para desvirar fotos deitadas
    private Bitmap corrigirRotacao(Bitmap bitmap, String path) throws IOException {
        ExifInterface ei = new ExifInterface(path);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap = null;
        switch(orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: rotatedBitmap = rotateImage(bitmap, 90); break;
            case ExifInterface.ORIENTATION_ROTATE_180: rotatedBitmap = rotateImage(bitmap, 180); break;
            case ExifInterface.ORIENTATION_ROTATE_270: rotatedBitmap = rotateImage(bitmap, 270); break;
            case ExifInterface.ORIENTATION_NORMAL: default: rotatedBitmap = bitmap;
        }
        return rotatedBitmap;
    }

    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    // --- PARTE 4: ENVIO PARA API (BEM MAIS SIMPLES AGORA) ---
    private void enviarCadastro() {
        String nome = edtNome.getText().toString();
        String email = edtEmail.getText().toString();

        if (nome.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Preencha nome e email!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Configura Retrofit com timeout maior
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .protocols(java.util.Collections.singletonList(okhttp3.Protocol.HTTP_1_1))
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        String urlBase = "https://teste.infinitydev.com.br/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(urlBase)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService api = retrofit.create(ApiService.class);

        RequestBody reqNome = RequestBody.create(MediaType.parse("text/plain"), nome);
        RequestBody reqEmail = RequestBody.create(MediaType.parse("text/plain"), email);

        MultipartBody.Part bodyFoto = null;

        // SE TIVER FOTO PROCESSADA: Usa o arquivoFotoFinal que já está pronto e pequeno
        if (arquivoFotoFinal != null && arquivoFotoFinal.exists()) {
            RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), arquivoFotoFinal);
            bodyFoto = MultipartBody.Part.createFormData("foto", arquivoFotoFinal.getName(), reqFile);
        }

        Toast.makeText(this, "Enviando...", Toast.LENGTH_SHORT).show();

        Call<ResponseBody> call = api.cadastrarAluno(reqNome, reqEmail, bodyFoto);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CadastroActivity.this, "Sucesso! Aluno salvo.", Toast.LENGTH_LONG).show();
                    // Tenta apagar as fotos temporárias para não encher o celular
                    if (arquivoFotoFinal != null) arquivoFotoFinal.delete();
                    new File(currentPhotoPath).delete();
                    finish();
                } else {
                    Toast.makeText(CadastroActivity.this, "Erro no servidor: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(CadastroActivity.this, "Falha na conexão: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}