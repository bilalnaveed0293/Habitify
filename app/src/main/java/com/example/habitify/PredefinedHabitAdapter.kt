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
    private val onHabitClick: (PredefinedHabit) -> Unit = {},
    private val onEditClick: (PredefinedHabit) -> Unit = {} // NEW: Edit callback
) : RecyclerView.Adapter<PredefinedHabitAdapter.HabitViewHolder>() {

    private val TAG = "PredefinedHabitAdapter"

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardHabit: CardView = itemView.findViewById(R.id.card_predefined_habit)
        private val ivHabitIcon: ImageView = itemView.findViewById(R.id.iv_habit_icon)
        private val tvHabitTitle: TextView = itemView.findViewById(R.id.tv_habit_title)
        private val tvHabitDescription: TextView = itemView.findViewById(R.id.tv_habit_description)
        private val tvHabitCategory: TextView = itemView.findViewById(R.id.tv_habit_category)
        private val tvSuggestedCount: TextView = itemView.findViewById(R.id.tv_suggested_count)
        private val btnAddHabit: ImageView = itemView.findViewById(R.id.btn_add_habit)
        private val tvCustomBadge: TextView = itemView.findViewById(R.id.tv_custom_badge)
        private val tvFrequency: TextView = itemView.findViewById(R.id.tv_frequency)

        fun bind(habit: PredefinedHabit) {
            Log.d(TAG, "Binding habit: ${habit.title} (Custom: ${habit.isCustom})")

            // Set habit title and description
            tvHabitTitle.text = habit.title
            tvHabitDescription.text = habit.description ?: "No description"
            tvHabitDescription.visibility = if (habit.description.isNullOrEmpty()) View.GONE else View.VISIBLE

            // Set category with emoji
            val (categoryEmoji, categoryText) = when (habit.category) {
                "mindfulness" -> Pair("🧘", "Mindfulness")
                "health" -> Pair("💊", "Health")
                "fitness" -> Pair("💪", "Fitness")
                "learning" -> Pair("📚", "Learning")
                "custom" -> Pair("✨", "Custom")
                else -> Pair("", habit.category.replaceFirstChar { it.uppercase() })
            }
            tvHabitCategory.text = "$categoryEmoji $categoryText"

            // Set frequency badge
            val frequencyText = when (habit.frequency) {
                "daily" -> "DAILY"
                "weekly" -> "WEEKLY"
                else -> habit.frequency.uppercase()
            }
            tvFrequency.text = frequencyText

            // For custom habits
            if (habit.isCustom) {
                tvSuggestedCount.text = "Your Template"
                tvSuggestedCount.setTextColor(ContextCompat.getColor(itemView.context, R.color.tertiary_text))

                // Show custom badge
                tvCustomBadge.visibility = View.VISIBLE

                // Show edit icon for custom habits
                btnAddHabit.setImageResource(R.drawable.ic_chevron_right)
                btnAddHabit.contentDescription = "Edit custom habit"
                btnAddHabit.setOnClickListener {
                    onEditClick(habit)
                }
            } else {
                // For predefined habits
                tvSuggestedCount.text = "${habit.suggestedCount} users"
                tvSuggestedCount.setTextColor(ContextCompat.getColor(itemView.context, R.color.secondary_text))

                // Hide custom badge
                tvCustomBadge.visibility = View.GONE

                // Show add icon for predefined habits
                btnAddHabit.setImageResource(R.drawable.ic_add_circle)
                btnAddHabit.contentDescription = "Add habit"
                btnAddHabit.setOnClickListener {
                    onHabitClick(habit)
                }
            }

            // Set habit icon and color
            // In your PredefinedHabitAdapter, update the bind function:
            val iconResource = when (habit.iconName.toLowerCase()) {
                "meditation" -> R.drawable.ic_meditation
                "water" -> R.drawable.ic_water
                "exercise" -> R.drawable.ic_exercise
                "read" -> R.drawable.ic_book
                "journal" -> R.drawable.ic_journal
                "learn" -> R.drawable.ic_learn
                "nosugar" -> R.drawable.ic_no_sugar
                "sleep" -> R.drawable.ic_sleep
                "gratitude" -> R.drawable.ic_gratitude
                "walk" -> R.drawable.ic_walk
                else -> R.drawable.ic_logo
            }

            ivHabitIcon.setImageResource(iconResource)

            try {
                val habitColor = Color.parseColor(habit.colorCode)
                ivHabitIcon.setColorFilter(habitColor)
                // Also tint the frequency badge
                tvFrequency.background?.setTint(habitColor)
            } catch (e: Exception) {
                val defaultColor = ContextCompat.getColor(itemView.context, R.color.primary_color)
                ivHabitIcon.setColorFilter(defaultColor)
                tvFrequency.background?.setTint(defaultColor)
            }

            // Whole card click
            cardHabit.setOnClickListener {
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