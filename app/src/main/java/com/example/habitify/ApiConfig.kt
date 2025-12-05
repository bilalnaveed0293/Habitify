package com.example.habitify

object ApiConfig {

    // Change this to your laptop's IP address
    // For emulator: "http://10.0.2.2"
    // For real device on same WiFi: "http://[YOUR_LAPTOP_IP]"
    const val BASE_IP = "http://192.168.100.177"
    private const val PROJECT_FOLDER = "habitify"

    // API Endpoints
    private val BASE_URL = "$BASE_IP/$PROJECT_FOLDER/api/"

    // Authentication Endpoints
    val LOGIN_URL = "${BASE_URL}auth/login.php"
    val REGISTER_URL = "${BASE_URL}auth/register.php"
    val FORGOT_PASSWORD_URL = "${BASE_URL}auth/forgot_password.php"

    val CHECK_EMAIL_URL = "${BASE_URL}auth/check_email.php" // NEW
    val VERIFY_RESET_CODE_URL = "${BASE_URL}auth/verify_reset_code.php"
    val RESET_PASSWORD_URL = "${BASE_URL}auth/reset_password.php"

    // Habit Endpoints
     val CREATE_HABIT_URL = "${BASE_URL}habits/create.php"
     val UPDATE_HABIT_URL = "${BASE_URL}habits/update.php"
     val DELETE_HABIT_URL = "${BASE_URL}habits/delete.php"
    val GET_PREDEFINED_HABITS_URL = "${BASE_URL}habits/get_predefined_habits.php"

    val GET_USER_HABITS_URL = "${BASE_URL}habits/get_habits.php"


    // Profile Endpoints
     val GET_PROFILE_URL = "${BASE_URL}profile/get.php"
     val UPDATE_PROFILE_URL = "${BASE_URL}profile/update.php"
    val CHANGE_PASSWORD_URL = "${BASE_URL}profile/change_password.php"
    val UPLOAD_PROFILE_PICTURE_URL = "${BASE_URL}profile/upload_profile_picture.php"



    // Sync Endpoints
     val SYNC_PUSH_URL = "${BASE_URL}sync/push.php"
     val SYNC_PULL_URL = "${BASE_URL}sync/pull.php"

    // Helper method to get full URL for custom endpoints
    fun getFullUrl(endpoint: String): String {
        return "${BASE_URL}${endpoint}"
    }

    // Helper method to change IP dynamically (if needed later)
    fun changeBaseIp(newIp: String) {
        // Note: This is a simplified version. In production, you'd want to persist this
        // For now, you need to change the BASE_IP constant above
        println("Change BASE_IP constant to: $newIp")
    }
}