<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Only accept POST requests
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        "success" => false,
        "message" => "Method not allowed",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

// Include database connection and functions
require_once '../../config/database.php';
require_once '../../includes/functions.php';

// Get raw POST data
$data = json_decode(file_get_contents("php://input"), true);

// Check if data is valid
if (!$data || !isset($data['habit_id']) || !isset($data['user_id'])) {
    echo json_encode([
        "success" => false,
        "message" => "Habit ID and User ID are required",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

// Get input values
$habit_id = intval($data['habit_id']);
$user_id = intval($data['user_id']);
$title = isset($data['title']) ? sanitizeInput($data['title']) : null;
$description = isset($data['description']) ? sanitizeInput($data['description']) : null;
$frequency = isset($data['frequency']) ? sanitizeInput($data['frequency']) : null;
$color_code = isset($data['color_code']) ? sanitizeInput($data['color_code']) : null;
$icon_name = isset($data['icon_name']) ? sanitizeInput($data['icon_name']) : null;
$reminder_time = isset($data['reminder_time']) ? sanitizeInput($data['reminder_time']) : null;
$reminder_enabled = isset($data['reminder_enabled']) ? intval($data['reminder_enabled']) : null;
$status = isset($data['status']) ? sanitizeInput($data['status']) : null;

// Validate frequency if provided
if ($frequency && !in_array($frequency, ['daily', 'weekly', 'custom'])) {
    $frequency = 'daily';
}

// Validate reminder time format if provided
if ($reminder_time && !preg_match('/^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$/', $reminder_time)) {
    $reminder_time = '09:00:00';
}

try {
    $database = new Database();
    $db = $database->getConnection();
    
    if (!$db) {
        throw new Exception("Failed to connect to database");
    }

    // Check if user owns this habit
    $checkOwnershipQuery = "SELECT id FROM habits WHERE id = :habit_id AND user_id = :user_id";
    $checkOwnershipStmt = $db->prepare($checkOwnershipQuery);
    $checkOwnershipStmt->bindParam(":habit_id", $habit_id, PDO::PARAM_INT);
    $checkOwnershipStmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
    $checkOwnershipStmt->execute();

    if ($checkOwnershipStmt->rowCount() === 0) {
        echo json_encode([
            "success" => false,
            "message" => "Habit not found or you don't have permission to edit it",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
        exit();
    }

    // Build update query dynamically based on what's being updated
    $updateFields = [];
    $params = [':habit_id' => $habit_id];
    
    if ($title !== null) {
        $updateFields[] = "title = :title";
        $params[':title'] = $title;
    }
    
    if ($description !== null) {
        $updateFields[] = "description = :description";
        $params[':description'] = $description;
    }
    
    if ($frequency !== null) {
        $updateFields[] = "frequency = :frequency";
        $params[':frequency'] = $frequency;
    }
    
    if ($color_code !== null) {
        $updateFields[] = "color_code = :color_code";
        $params[':color_code'] = $color_code;
    }
    
    if ($icon_name !== null) {
        $updateFields[] = "icon_name = :icon_name";
        $params[':icon_name'] = $icon_name;
    }
    
    if ($reminder_time !== null) {
        $updateFields[] = "reminder_time = :reminder_time";
        $params[':reminder_time'] = $reminder_time;
    }
    
    if ($reminder_enabled !== null) {
        $updateFields[] = "reminder_enabled = :reminder_enabled";
        $params[':reminder_enabled'] = $reminder_enabled;
    }
    
    if ($status !== null && in_array($status, ['todo', 'completed', 'failed', 'archived'])) {
        $updateFields[] = "status = :status";
        $params[':status'] = $status;
    }
    
    // Always update the updated_at timestamp
    $updateFields[] = "updated_at = NOW()";
    
    // If nothing to update
    if (count($updateFields) === 1) { // Only updated_at
        echo json_encode([
            "success" => false,
            "message" => "No changes to update",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
        exit();
    }
    
    // Build and execute update query
    $updateQuery = "UPDATE habits SET " . implode(", ", $updateFields) . " WHERE id = :habit_id";
    $updateStmt = $db->prepare($updateQuery);
    
    // Bind all parameters
    foreach ($params as $key => $value) {
        $updateStmt->bindValue($key, $value);
    }
    
    if ($updateStmt->execute()) {
        // Get updated habit
        $getHabitQuery = "
            SELECT h.*, COALESCE(hl.status, 'todo') as today_status
            FROM habits h
            LEFT JOIN habit_logs hl ON h.id = hl.habit_id AND hl.log_date = CURDATE()
            WHERE h.id = :habit_id
        ";
        
        $getHabitStmt = $db->prepare($getHabitQuery);
        $getHabitStmt->bindParam(":habit_id", $habit_id, PDO::PARAM_INT);
        $getHabitStmt->execute();
        $updatedHabit = $getHabitStmt->fetch(PDO::FETCH_ASSOC);
        if ($reminder_enabled) {
            // Schedule notification for today (if time hasn't passed) and tomorrow
            scheduleHabitNotifications($db, $user_id, $habit_id, $title, $reminder_time);
        }
        
        echo json_encode([
            "success" => true,
            "message" => "Habit updated successfully",
            "data" => [
                "habit" => $updatedHabit
            ],
            "timestamp" => date('Y-m-d H:i:s')
        ]);
        
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Failed to update habit",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
    }
    
} catch (PDOException $e) {
    error_log("Database error in update_habit.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "Database error occurred",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
} catch (Exception $e) {
    error_log("Error in update_habit.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "An error occurred",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
}
?>