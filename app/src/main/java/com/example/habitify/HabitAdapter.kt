package com.example.habitify

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(
    private var habits: List<Habit> = emptyList(),
    private val onHabitClick: (Habit) -> Unit = {},
    private val onHabitLongClick: (Habit) -> Unit = {},
    private val onCompleteClick: (Habit) -> Unit = {}  // NEW: Add completion callback
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

            val statusIcon = when (habit.category.toLowerCase()) {  // Change from todayStatus to category
                "completed" -> R.drawable.ic_check_circle
                "failed" -> R.drawable.ic_cancel
                else -> R.drawable.ic_radio_button_unchecked  // Default for todo
            }

            ivStatusIcon.setImageResource(statusIcon)

            val statusText = when (habit.category) {  // Already using category, this is correct
                "completed" -> "Completed"
                "failed" -> "Failed"
                else -> "To Do"
            }

            tvStatusText.text = statusText

            // Set status color - also update this to use category
            val statusColor = when (habit.category) {
                "completed" -> ContextCompat.getColor(itemView.context, R.color.habit_completed)
                "failed" -> ContextCompat.getColor(itemView.context, R.color.habit_failed)
                else -> ContextCompat.getColor(itemView.context, R.color.habit_todo)
            }
            tvStatusText.setTextColor(statusColor)

            // Set habit icon - UPDATED TO USE iconName
            ivHabitIcon.setImageResource(habit.getIconResource())

            // Set habit color
            try {
                val habitColor = Color.parseColor(habit.colorCode)
                ivHabitIcon.setColorFilter(habitColor)
            } catch (e: Exception) {
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

            // NEW: Add click listener to status icon for completion
            ivStatusIcon.setOnClickListener {
                // Only allow toggling for "To Do" habits
                if (habit.category == "todo") {
                    onCompleteClick(habit)
                }
            }
            ivStatusIcon.setOnClickListener {
                // Only allow toggling for "todo" habits (not completed or failed)
                if (habit.category == "todo") {
                    onCompleteClick(habit)
                } else {
                    // If already completed or failed, show message

                }
            }

            // Also make the whole status area clickable
            tvStatusText.setOnClickListener {
                if (habit.category == "todo") {
                    onCompleteClick(habit)
                }
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
