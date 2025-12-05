<?php
// habits/create.php - Updated to handle custom habits
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
    
    // Insert into habits table
    $sql = "INSERT INTO habits (user_id, title, description, frequency, start_date, 
              color_code, icon_name, current_streak, longest_streak, status, 
              reminder_time, reminder_enabled, created_at, updated_at) 
             VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 'todo', ?, ?, NOW(), NOW())";
    
    $stmt = $conn->prepare($sql);
    $stmt->execute([
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
    
    // Create first habit_log entry
    $log_sql = "INSERT INTO habit_logs (habit_id, user_id, log_date, status, created_at) 
                VALUES (?, ?, ?, 'todo', NOW())";
    
    $log_stmt = $conn->prepare($log_sql);
    $log_stmt->execute([$habit_id, $user_id, $current_date]);
    
    $conn->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Habit created successfully',
        'data' => [
            'habit_id' => $habit_id,
            'start_date' => $current_date,
            'status' => 'todo'
        ]
    ]);
    
} catch(PDOException $e) {
    if (isset($conn)) {
        $conn->rollBack();
    }
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>