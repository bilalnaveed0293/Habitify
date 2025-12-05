package com.example.habitify

data class PredefinedHabit(
    val id: Int,
    val title: String,
    val description: String?,
    val category: String,
    val iconName: String,
    val colorCode: String,
    val frequency: String,
    val suggestedCount: Int,
    val isCustom: Boolean = false, // NEW: Flag for custom habits
    val customHabitId: Int? = null // NEW: Original ID for custom habits
) {
    fun getIconResource(): Int {
        return when (iconName.toLowerCase()) {
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
    }
}