<?php
// habits/update_status.php - COMPLETELY REWRITTEN for proper status handling
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

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
    
    if (!$data || !isset($data['user_id']) || !isset($data['habit_id']) || !isset($data['status'])) {
        echo json_encode(['success' => false, 'message' => 'Missing required fields']);
        exit();
    }
    
    $user_id = (int)$data['user_id'];
    $habit_id = (int)$data['habit_id'];
    $status = $data['status']; // 'completed' or 'failed'
    $today = date('Y-m-d');
    
    // Validate status
    if (!in_array($status, ['completed', 'failed'])) {
        echo json_encode(['success' => false, 'message' => 'Invalid status. Must be "completed" or "failed"']);
        exit();
    }
    
    $conn->beginTransaction();
    
    // 1. Check if habit belongs to user
    $checkSql = "SELECT id, current_streak, longest_streak FROM habits 
                WHERE id = ? AND user_id = ? AND status != 'archived'";
    $checkStmt = $conn->prepare($checkSql);
    $checkStmt->execute([$habit_id, $user_id]);
    
    if ($checkStmt->rowCount() === 0) {
        $conn->rollBack();
        echo json_encode(['success' => false, 'message' => 'Habit not found or archived']);
        exit();
    }
    
    $habit = $checkStmt->fetch(PDO::FETCH_ASSOC);
    $current_streak = (int)$habit['current_streak'];
    $longest_streak = (int)$habit['longest_streak'];
    
    // 2. Update or insert today's habit_log
    $logSql = "INSERT INTO habit_logs (habit_id, user_id, log_date, status, completed_at, created_at, updated_at)
              VALUES (?, ?, ?, ?, NOW(), NOW(), NOW())
              ON DUPLICATE KEY UPDATE 
              status = VALUES(status),
              completed_at = NOW(),
              updated_at = NOW()";
    
    $logStmt = $conn->prepare($logSql);
    $logStmt->execute([$habit_id, $user_id, $today, $status]);
    
    // 3. Update habit table with proper status and streak calculation
    if ($status === 'completed') {
        // Increment streak for completed habits
        $new_streak = $current_streak + 1;
        $new_longest_streak = max($longest_streak, $new_streak);
        
        $habitUpdateSql = "UPDATE habits 
                          SET status = 'completed',
                              current_streak = ?,
                              longest_streak = ?,
                              updated_at = NOW()
                          WHERE id = ? AND user_id = ?";
        
        $habitStmt = $conn->prepare($habitUpdateSql);
        $habitStmt->execute([$new_streak, $new_longest_streak, $habit_id, $user_id]);
        
    } else if ($status === 'failed') {
        // Reset streak for failed habits
        $habitUpdateSql = "UPDATE habits 
                          SET status = 'failed',
                              current_streak = 0,
                              updated_at = NOW()
                          WHERE id = ? AND user_id = ?";
        
        $habitStmt = $conn->prepare($habitUpdateSql);
        $habitStmt->execute([$habit_id, $user_id]);
    }
    
    // 4. Get updated habit with today's status
    $getUpdatedSql = "SELECT 
                        h.*,
                        hl.status as today_status,
                        hl.completed_at as today_completed_at,
                        DATE_FORMAT(h.created_at, '%b %d, %Y') as created_at_formatted
                     FROM habits h
                     LEFT JOIN habit_logs hl ON h.id = hl.habit_id 
                         AND hl.user_id = h.user_id 
                         AND hl.log_date = ?
                     WHERE h.id = ? AND h.user_id = ?";
    
    $getUpdatedStmt = $conn->prepare($getUpdatedSql);
    $getUpdatedStmt->execute([$today, $habit_id, $user_id]);
    $updated_habit = $getUpdatedStmt->fetch(PDO::FETCH_ASSOC);
    
    $conn->commit();
    
    echo json_encode([
        'success' => true,
        'message' => "Habit marked as $status",
        'data' => [
            'habit' => $updated_habit,
            'updated_status' => $status,
            'today' => $today
        ]
    ]);
    
} catch(PDOException $e) {
    if (isset($conn)) {
        $conn->rollBack();
    }
    error_log("Update Status Error: " . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>