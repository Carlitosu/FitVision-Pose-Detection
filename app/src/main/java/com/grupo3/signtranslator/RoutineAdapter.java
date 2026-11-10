package com.grupo3.signtranslator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder> {

    public interface OnRoutineActionListener {
        void onDelete(Routine routine, int position);
        void onEdit(Routine routine, int position);
    }

    private final List<Routine> routineList;
    private final OnRoutineActionListener actionListener;

    public RoutineAdapter(List<Routine> routineList, OnRoutineActionListener actionListener) {
        this.routineList = routineList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public RoutineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_routine, parent, false);
        return new RoutineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutineViewHolder holder, int position) {
        Routine routine = routineList.get(position);
        holder.tvTitle.setText(routine.getTitle());
        holder.tvTime.setText(routine.getFormattedTime());
        holder.tvDays.setText(routine.getFormattedDays());

        holder.btnEdit.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEdit(routine, holder.getAdapterPosition());
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(routine, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return routineList.size();
    }

    public static class RoutineViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvDays;
        View btnEdit, btnDelete;

        public RoutineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvRoutineTitle);
            tvTime = itemView.findViewById(R.id.tvRoutineTime);
            tvDays = itemView.findViewById(R.id.tvRoutineDays);
            btnEdit = itemView.findViewById(R.id.btnEditRoutine);
            btnDelete = itemView.findViewById(R.id.btnDeleteRoutine);
        }
    }
}