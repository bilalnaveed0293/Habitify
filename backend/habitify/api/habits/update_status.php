<?php
// habits/update_status.php - Updated with streak calculation
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
    
    if (!$data || !isset($data['user_id']) || !isset($data['habit_id']) || !isset($data['status'])) {
        echo json_encode(['success' => false, 'message' => 'Missing required fields']);
        exit();
    }
    
    $user_id = (int)$data['user_id'];
    $habit_id = (int)$data['habit_id'];
    $status = $data['status']; // 'completed' or 'failed'
    $today = date('Y-m-d');
    
    $conn->beginTransaction();
    
    // 1. Update today's habit_log
    $sql1 = "UPDATE habit_logs 
             SET status = ?, completed_at = NOW()
             WHERE habit_id = ? 
             AND user_id = ? 
             AND log_date = ?";
    
    $stmt1 = $conn->prepare($sql1);
    $stmt1->execute([$status, $habit_id, $user_id, $today]);
    
    // 2. Update habit status and calculate streaks
    if ($status === 'completed') {
        // Increase streak for completed habits
        $sql2 = "UPDATE habits 
                 SET status = ?,
                     current_streak = current_streak + 1,
                     longest_streak = GREATEST(longest_streak, current_streak + 1),
                     updated_at = NOW()
                 WHERE id = ? AND user_id = ?";
    } else {
        // Reset streak for failed habits
        $sql2 = "UPDATE habits 
                 SET status = ?,
                     current_streak = 0,
                     updated_at = NOW()
                 WHERE id = ? AND user_id = ?";
    }
    
    $stmt2 = $conn->prepare($sql2);
    $stmt2->execute([$status, $habit_id, $user_id]);
    
    $conn->commit();
    
    // Get updated habit info
    $sql3 = "SELECT current_streak, longest_streak FROM habits WHERE id = ?";
    $stmt3 = $conn->prepare($sql3);
    $stmt3->execute([$habit_id]);
    $habit_info = $stmt3->fetch(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'message' => "Habit marked as $status",
        'data' => [
            'habit_id' => $habit_id,
            'status' => $status,
            'current_streak' => $habit_info['current_streak'],
            'longest_streak' => $habit_info['longest_streak'],
            'updated_at' => $today
        ]
    ]);
    
} catch(PDOException $e) {
    if (isset($conn)) {
        $conn->rollBack();
    }
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>