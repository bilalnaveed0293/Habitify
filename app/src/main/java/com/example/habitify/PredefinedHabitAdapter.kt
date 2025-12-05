package com.example.habitify

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class PredefinedHabitAdapter(
    private var habits: List<PredefinedHabit> = emptyList(),
    private val onHabitClick: (PredefinedHabit) -> Unit = {}
) : RecyclerView.Adapter<PredefinedHabitAdapter.HabitViewHolder>() {

    private val TAG = "PredefinedHabitAdapter"

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardHabit: CardView = itemView.findViewById(R.id.card_predefined_habit)
        private val ivHabitIcon: ImageView = itemView.findViewById(R.id.iv_habit_icon)
        private val tvHabitTitle: TextView = itemView.findViewById(R.id.tv_habit_title)
        private val tvHabitDescription: TextView = itemView.findViewById(R.id.tv_habit_description)
        private val tvHabitCategory: TextView = itemView.findViewById(R.id.tv_habit_category)
        private val tvSuggestedCount: TextView = itemView.findViewById(R.id.tv_suggested_count)
        private val btnAddHabit: View = itemView.findViewById(R.id.btn_add_habit)

        fun bind(habit: PredefinedHabit) {
            // Debug: Log binding
            Log.d(TAG, "Binding habit: ${habit.title}")

            // Set habit title and description
            tvHabitTitle.text = habit.title
            tvHabitDescription.text = habit.description ?: "No description"

            // Set category
            tvHabitCategory.text = when (habit.category) {
                "mindfulness" -> "🧘 Mindfulness"
                "health" -> "💊 Health"
                "fitness" -> "💪 Fitness"
                "learning" -> "📚 Learning"
                else -> habit.category.replaceFirstChar { it.uppercase() }
            }

            // Set suggested count
            tvSuggestedCount.text = "Added by ${habit.suggestedCount} users"

            // Debug: Check if icon is being set
            Log.d(TAG, "Setting icon: ${habit.iconName}")

            // Set habit icon and color
            ivHabitIcon.setImageResource(habit.getIconResource())
            try {
                val habitColor = Color.parseColor(habit.colorCode)
                ivHabitIcon.setColorFilter(habitColor)
            } catch (e: Exception) {
                // Use default color if parsing fails
                ivHabitIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.primary_color))
            }

            // Set click listener
            btnAddHabit.setOnClickListener {
                Log.d(TAG, "Add button clicked for: ${habit.title}")
                onHabitClick(habit)
            }

            // Whole card click
            cardHabit.setOnClickListener {
                Log.d(TAG, "Card clicked for: ${habit.title}")
                onHabitClick(habit)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        Log.d(TAG, "Creating view holder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_predefined_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        Log.d(TAG, "Binding view holder at position $position")
        holder.bind(habits[position])
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "Item count: ${habits.size}")
        return habits.size
    }

    fun updateHabits(newHabits: List<PredefinedHabit>) {
        Log.d(TAG, "Updating habits from ${habits.size} to ${newHabits.size}")
        habits = newHabits
        notifyDataSetChanged()
        Log.d(TAG, "Notified dataset changed")
    }
}