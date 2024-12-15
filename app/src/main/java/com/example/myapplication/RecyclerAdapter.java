package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private final List<String> datos;

    public RecyclerAdapter(List<String> datos) {
        this.datos = datos;
    }

    public List<String> getDatos() {
        return datos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] splitData = datos.get(position).split(", ");
        if (splitData.length == 2) {
            holder.textFecha.setText(splitData[0].replace("Fecha: ", ""));
            holder.textValor.setText(splitData[1].replace("Código: ", ""));
        }
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textFecha;
        final TextView textValor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textFecha = itemView.findViewById(R.id.text_fecha);
            textValor = itemView.findViewById(R.id.text_valor);
        }
    }
}
