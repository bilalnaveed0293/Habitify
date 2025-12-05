<?php
// habits/save_custom.php - UPDATED to add to user habits
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

$host = "localhost";
$dbname = "habitify_db";
$username = "root";
$password = "";

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    
    if (!$data || !isset($data['user_id']) || !isset($data['title'])) {
        echo json_encode(['success' => false, 'message' => 'Missing required fields']);
        exit();
    }
    
    $user_id = (int)$data['user_id'];
    $title = trim($data['title']);
    $current_date = date('Y-m-d');
    
    if (empty($title)) {
        echo json_encode(['success' => false, 'message' => 'Habit title is required']);
        exit();
    }
    
    $conn->beginTransaction();
    
    // Check if this is an update or delete
    if (isset($data['custom_habit_id']) && $data['custom_habit_id'] > 0) {
        $custom_habit_id = (int)$data['custom_habit_id'];
        
        // Check if user owns this custom habit
        $check_sql = "SELECT id FROM custom_habits WHERE id = ? AND user_id = ?";
        $check_stmt = $conn->prepare($check_sql);
        $check_stmt->execute([$custom_habit_id, $user_id]);
        
        if ($check_stmt->rowCount() === 0) {
            $conn->rollBack();
            echo json_encode(['success' => false, 'message' => 'Custom habit not found']);
            exit();
        }
        
        // Check if this is a delete (soft delete)
        if (isset($data['is_active']) && $data['is_active'] == 0) {
            $delete_sql = "UPDATE custom_habits SET is_active = 0 WHERE id = ? AND user_id = ?";
            $delete_stmt = $conn->prepare($delete_sql);
            $delete_stmt->execute([$custom_habit_id, $user_id]);
            
            $conn->commit();
            echo json_encode([
                'success' => true,
                'message' => 'Custom habit deleted successfully',
                'data' => ['custom_habit_id' => $custom_habit_id]
            ]);
            exit();
        }
        
        // Update existing custom habit
        $update_sql = "UPDATE custom_habits SET 
                      title = ?,
                      description = ?,
                      category = ?,
                      icon_name = ?,
                      color_code = ?,
                      frequency = ?,
                      reminder_time = ?,
                      reminder_enabled = ?,
                      created_at = NOW()
                      WHERE id = ? AND user_id = ?";
        
        $update_stmt = $conn->prepare($update_sql);
        $update_stmt->execute([
            $title,
            $data['description'] ?? '',
            $data['category'] ?? 'custom',
            $data['icon_name'] ?? 'default',
            $data['color_code'] ?? '#4CAF50',
            $data['frequency'] ?? 'daily',
            $data['reminder_time'] ?? '09:00:00',
            $data['reminder_enabled'] ?? 1,
            $custom_habit_id,
            $user_id
        ]);
        
        $conn->commit();
        echo json_encode([
            'success' => true,
            'message' => 'Custom habit updated successfully',
            'data' => ['custom_habit_id' => $custom_habit_id]
        ]);
        
    } else {
        // CREATE NEW CUSTOM HABIT AND ADD TO USER HABITS
        
        // 1. Save to custom_habits table
        $insert_custom_sql = "INSERT INTO custom_habits 
                      (user_id, title, description, category, icon_name, color_code, 
                       frequency, reminder_time, reminder_enabled, created_at, is_active)
                      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 1)";
        
        $insert_custom_stmt = $conn->prepare($insert_custom_sql);
        $insert_custom_stmt->execute([
            $user_id,
            $title,
            $data['description'] ?? '',
            $data['category'] ?? 'custom',
            $data['icon_name'] ?? 'default',
            $data['color_code'] ?? '#4CAF50',
            $data['frequency'] ?? 'daily',
            $data['reminder_time'] ?? '09:00:00',
            $data['reminder_enabled'] ?? 1
        ]);
        
        $custom_habit_id = $conn->lastInsertId();
        
        // 2. ALSO ADD TO USER'S ACTIVE HABITS (habits table)
        $insert_habit_sql = "INSERT INTO habits 
                      (user_id, title, description, frequency, start_date, 
                       color_code, icon_name, current_streak, longest_streak, status, 
                       reminder_time, reminder_enabled, created_at, updated_at) 
                     VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 'todo', ?, ?, NOW(), NOW())";
        
        $insert_habit_stmt = $conn->prepare($insert_habit_sql);
        $insert_habit_stmt->execute([
            $user_id,
            $title,
            $data['description'] ?? '',
            $data['frequency'] ?? 'daily',
            $current_date,
            $data['color_code'] ?? '#4CAF50',
            $data['icon_name'] ?? 'default',
            $data['reminder_time'] ?? '09:00:00',
            $data['reminder_enabled'] ?? 1
        ]);
        
        $habit_id = $conn->lastInsertId();
        
        // 3. Create first habit_log entry
        $log_sql = "INSERT INTO habit_logs (habit_id, user_id, log_date, status, created_at) 
                    VALUES (?, ?, ?, 'todo', NOW())";
        
        $log_stmt = $conn->prepare($log_sql);
        $log_stmt->execute([$habit_id, $user_id, $current_date]);
        
        $conn->commit();
        
        echo json_encode([
            'success' => true,
            'message' => 'Custom habit created and added to your habits!',
            'data' => [
                'custom_habit_id' => $custom_habit_id,
                'habit_id' => $habit_id
            ]
        ]);
    }
    
} catch(PDOException $e) {
    if (isset($conn)) {
        $conn->rollBack();
    }
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>