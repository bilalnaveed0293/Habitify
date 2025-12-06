<?php
// habits/add_custom_to_habits.php - SIMPLIFIED
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
    
    if (!$data || !isset($data['user_id']) || !isset($data['custom_habit_id'])) {
        echo json_encode(['success' => false, 'message' => 'Missing required fields']);
        exit();
    }
    
    $user_id = (int)$data['user_id'];
    $custom_habit_id = (int)$data['custom_habit_id'];
    $current_date = date('Y-m-d');
    
    $conn->beginTransaction();
    
    // 1. Get custom habit details
    $getCustomSql = "SELECT * FROM custom_habits WHERE id = ? AND user_id = ? AND is_active = 1";
    $getCustomStmt = $conn->prepare($getCustomSql);
    $getCustomStmt->execute([$custom_habit_id, $user_id]);
    
    if ($getCustomStmt->rowCount() === 0) {
        $conn->rollBack();
        echo json_encode(['success' => false, 'message' => 'Custom habit not found']);
        exit();
    }
    
    $customHabit = $getCustomStmt->fetch(PDO::FETCH_ASSOC);
    
    // 2. Insert into habits table
    $insertSql = "INSERT INTO habits (user_id, title, description, frequency, start_date, 
                  color_code, icon_name, current_streak, longest_streak, status, 
                  reminder_time, reminder_enabled, created_at, updated_at) 
                 VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 'todo', ?, ?, NOW(), NOW())";
    
    $insertStmt = $conn->prepare($insertSql);
    $insertStmt->execute([
        $user_id,
        $customHabit['title'],
        $customHabit['description'],
        $customHabit['frequency'],
        $current_date,
        $customHabit['color_code'],
        $customHabit['icon_name'],
        $customHabit['reminder_time'],
        $customHabit['reminder_enabled']
    ]);
    
    $habit_id = $conn->lastInsertId();
    
    // 3. Create first habit_log entry
    $logSql = "INSERT INTO habit_logs (habit_id, user_id, log_date, status, created_at) 
                VALUES (?, ?, ?, 'todo', NOW())";
    $logStmt = $conn->prepare($logSql);
    $logStmt->execute([$habit_id, $user_id, $current_date]);
    
    $conn->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Custom habit added to your habits!',
        'data' => ['habit_id' => $habit_id]
    ]);
    
} catch(PDOException $e) {
    if (isset($conn)) {
        $conn->rollBack();
    }
    error_log("Add Custom to Habits Error: " . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>