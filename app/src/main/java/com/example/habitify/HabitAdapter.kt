package com.example.habitify

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(
    private var habits: List<Habit> = emptyList(),
    private val onHabitClick: (Habit) -> Unit = {},
    private val onHabitLongClick: (Habit) -> Unit = {}
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardHabit: CardView = itemView.findViewById(R.id.card_habit)
        private val ivHabitIcon: ImageView = itemView.findViewById(R.id.iv_habit_icon)
        private val tvHabitTitle: TextView = itemView.findViewById(R.id.tv_habit_title)
        private val tvHabitDescription: TextView = itemView.findViewById(R.id.tv_habit_description)
        private val tvStreakCount: TextView = itemView.findViewById(R.id.tv_streak_count)
        private val tvCreatedDate: TextView = itemView.findViewById(R.id.tv_created_date)
        private val ivStatusIcon: ImageView = itemView.findViewById(R.id.iv_status_icon)
        private val tvStatusText: TextView = itemView.findViewById(R.id.tv_status_text)

        fun bind(habit: Habit) {
            // Set habit title and description
            tvHabitTitle.text = habit.title
            tvHabitDescription.text = habit.description ?: "No description"

            // Set streak
            tvStreakCount.text = "🔥 ${habit.currentStreak} day streak"

            // Set creation date
            tvCreatedDate.text = "Started ${habit.getFormattedDate()}"

            // Set status icon and text
            ivStatusIcon.setImageResource(habit.getStatusIcon())
            tvStatusText.text = when (habit.category) {
                "completed" -> "Completed"
                "failed" -> "Failed"
                else -> "To Do"
            }

            // Set status color
            val statusColor = ContextCompat.getColor(itemView.context, habit.getStatusColor())
            tvStatusText.setTextColor(statusColor)

            // Set habit color (use color code from habit)
            try {
                val habitColor = Color.parseColor(habit.colorCode)
                ivHabitIcon.setColorFilter(habitColor)
            } catch (e: Exception) {
                // Use default color if parsing fails
                ivHabitIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.primary_color))
            }

            // Set click listeners
            itemView.setOnClickListener {
                onHabitClick(habit)
            }

            itemView.setOnLongClickListener {
                onHabitLongClick(habit)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(habits[position])
    }

    override fun getItemCount(): Int = habits.size

    fun updateHabits(newHabits: List<Habit>) {
        habits = newHabits
        notifyDataSetChanged()
    }

    fun getHabits(): List<Habit> = habits
}