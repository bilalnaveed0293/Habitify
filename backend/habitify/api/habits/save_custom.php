<?php
// habits/save_custom.php - FIXED DELETE OPERATION
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
    
    if (!$data || !isset($data['user_id'])) {
        echo json_encode(['success' => false, 'message' => 'Missing user_id']);
        exit();
    }
    
    $user_id = (int)$data['user_id'];
    
    // CHECK IF THIS IS A DELETE OPERATION
    if (isset($data['is_active']) && $data['is_active'] == 0 && isset($data['custom_habit_id'])) {
        // DELETE OPERATION
        $custom_habit_id = (int)$data['custom_habit_id'];
        
        $conn->beginTransaction();
        
        // Check if user owns this custom habit
        $checkSql = "SELECT id FROM custom_habits WHERE id = ? AND user_id = ?";
        $checkStmt = $conn->prepare($checkSql);
        $checkStmt->execute([$custom_habit_id, $user_id]);
        
        if ($checkStmt->rowCount() === 0) {
            $conn->rollBack();
            echo json_encode(['success' => false, 'message' => 'Custom habit not found']);
            exit();
        }
        
        // Soft delete
        $deleteSql = "UPDATE custom_habits SET is_active = 0 WHERE id = ? AND user_id = ?";
        $deleteStmt = $conn->prepare($deleteSql);
        $deleteStmt->execute([$custom_habit_id, $user_id]);
        
        $conn->commit();
        
        echo json_encode([
            'success' => true,
            'message' => 'Custom habit deleted successfully'
        ]);
        exit();
        
    } else if (isset($data['custom_habit_id']) && $data['custom_habit_id'] > 0) {
        // UPDATE OPERATION
        $custom_habit_id = (int)$data['custom_habit_id'];
        $title = isset($data['title']) ? trim($data['title']) : '';
        
        if (empty($title)) {
            echo json_encode(['success' => false, 'message' => 'Habit title is required for update']);
            exit();
        }
        
        $conn->beginTransaction();
        
        // Check if user owns this custom habit
        $checkSql = "SELECT id FROM custom_habits WHERE id = ? AND user_id = ?";
        $checkStmt = $conn->prepare($checkSql);
        $checkStmt->execute([$custom_habit_id, $user_id]);
        
        if ($checkStmt->rowCount() === 0) {
            $conn->rollBack();
            echo json_encode(['success' => false, 'message' => 'Custom habit not found']);
            exit();
        }
        
        // Update custom habit
        $updateSql = "UPDATE custom_habits SET 
                      title = ?,
                      description = ?,
                      category = ?,
                      icon_name = ?,
                      color_code = ?,
                      frequency = ?,
                      reminder_time = ?,
                      reminder_enabled = ?,
                      updated_at = NOW()
                      WHERE id = ? AND user_id = ?";
        
        $updateStmt = $conn->prepare($updateSql);
        $updateStmt->execute([
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
            'message' => 'Custom habit updated successfully'
        ]);
        
    } else {
        // CREATE OPERATION
        $title = isset($data['title']) ? trim($data['title']) : '';
        
        if (empty($title)) {
            echo json_encode(['success' => false, 'message' => 'Habit title is required']);
            exit();
        }
        
        $conn->beginTransaction();
        
        // Create new custom habit template
        $insertSql = "INSERT INTO custom_habits 
                      (user_id, title, description, category, icon_name, color_code, 
                       frequency, reminder_time, reminder_enabled, created_at, is_active)
                      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 1)";
        
        $insertStmt = $conn->prepare($insertSql);
        $insertStmt->execute([
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
        
        $conn->commit();
        
        echo json_encode([
            'success' => true,
            'message' => 'Custom habit template created successfully',
            'data' => ['custom_habit_id' => $custom_habit_id]
        ]);
    }
    
} catch(PDOException $e) {
    if (isset($conn)) {
        $conn->rollBack();
    }
    error_log("Save Custom Error: " . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>