package br.ulbra.appapi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class AlunoAdapter extends RecyclerView.Adapter<AlunoAdapter.AlunoViewHolder> {

    private Context context;
    private List<Aluno> listaAlunos;

    public AlunoAdapter(Context context, List<Aluno> listaAlunos) {
        this.context = context;
        this.listaAlunos = listaAlunos;
    }

    @NonNull
    @Override
    public AlunoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Aqui ele "infla" (cria) o visual do item_aluno.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_aluno, parent, false);
        return new AlunoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlunoViewHolder holder, int position) {
        // Aqui ele pega os dados e joga na tela
        Aluno aluno = listaAlunos.get(position);

        holder.txtNome.setText(aluno.getNome());
        holder.txtEmail.setText(aluno.getEmail());

        // GLIDE: Carrega a foto redonda!
        if (aluno.getFotoUrl() != null && !aluno.getFotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(aluno.getFotoUrl())
                    .circleCrop() // Deixa redonda igual no CSS border-radius
                    .placeholder(android.R.drawable.ic_menu_camera) // Imagem enquanto carrega
                    .into(holder.imgPerfil);
        } else {
            // Se não tiver foto, coloca um bonequinho padrão
            holder.imgPerfil.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
    }

    @Override
    public int getItemCount() {
        return listaAlunos.size();
    }

    // Classe interna que mapeia os componentes do XML
    public class AlunoViewHolder extends RecyclerView.ViewHolder {
        TextView txtNome, txtEmail;
        ImageView imgPerfil;

        public AlunoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNome = itemView.findViewById(R.id.txtNome);
            txtEmail = itemView.findViewById(R.id.txtEmail);
            imgPerfil = itemView.findViewById(R.id.imgPerfil);
        }
    }
}