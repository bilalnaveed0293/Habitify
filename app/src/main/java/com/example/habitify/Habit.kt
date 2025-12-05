package com.example.habitify

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Habit(
    val id: Int,
    val title: String,
    val description: String?,
    val frequency: String,
    val colorCode: String,
    val iconName: String,  // This should store the icon name
    val currentStreak: Int,
    val longestStreak: Int,
    val todayStatus: String,
    val category: String, // "todo", "completed", "failed"
    val createdAt: String,
    val createdAtFormatted: String,
    val todayCompletedAt: String?
) {
    // Get icon resource ID based on icon name
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
            "default" -> R.drawable.ic_logo
            else -> R.drawable.ic_logo
        }
    }

    fun getStatusIcon(): Int {
        return when (todayStatus.toLowerCase()) {
            "done", "completed" -> R.drawable.ic_check_circle
            "failed" -> R.drawable.ic_cancel
            "skipped" -> R.drawable.ic_pause_circle
            else -> R.drawable.ic_radio_button_unchecked
        }
    }

    fun getStatusColor(): Int {
        return when (category) {
            "completed" -> R.color.habit_completed
            "failed" -> R.color.habit_failed
            else -> R.color.habit_todo
        }
    }

    fun getFormattedDate(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val date = inputFormat.parse(createdAt)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            createdAtFormatted
        }
    }
}