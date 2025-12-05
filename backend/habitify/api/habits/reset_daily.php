<?php
// habits/reset_daily.php - Fixed version
header('Content-Type: application/json');

$host = "localhost";
$dbname = "habitify_db";
$username = "root";
$password = "";

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    $today = date('Y-m-d');
    $yesterday = date('Y-m-d', strtotime('-1 day'));
    
    $conn->beginTransaction();
    
    // 1. Mark yesterday's incomplete habits as 'failed'
    $sql1 = "UPDATE habit_logs hl
             JOIN habits h ON hl.habit_id = h.id
             SET hl.status = 'failed',
                 h.current_streak = 0,
                 h.status = 'failed',
                 h.updated_at = NOW()
             WHERE hl.log_date = ?
             AND hl.status = 'todo'
             AND h.status = 'todo'
             AND h.frequency = 'daily'";
    
    $stmt1 = $conn->prepare($sql1);
    $stmt1->execute([$yesterday]);
    $failed_count = $stmt1->rowCount();
    
    // 2. Reset streaks for failed habits
    $sql2 = "UPDATE habits 
             SET current_streak = 0 
             WHERE status = 'failed'";
    $stmt2 = $conn->prepare($sql2);
    $stmt2->execute();
    
    // 3. Create new 'todo' entries for today
    $sql3 = "INSERT INTO habit_logs (habit_id, user_id, log_date, status, created_at)
             SELECT h.id, h.user_id, ?, 'todo', NOW()
             FROM habits h
             WHERE h.status IN ('todo', 'completed')
             AND h.frequency = 'daily'
             AND h.status != 'archived'  -- Changed from is_active
             AND NOT EXISTS (
                 SELECT 1 FROM habit_logs hl2 
                 WHERE hl2.habit_id = h.id 
                 AND hl2.log_date = ?
             )";
    
    $stmt3 = $conn->prepare($sql3);
    $stmt3->execute([$today, $today]);
    $todo_count = $stmt3->rowCount();
    
    // 4. Reset habit status to 'todo' for the new day
    $sql4 = "UPDATE habits 
             SET status = 'todo',
                 updated_at = NOW()
             WHERE status = 'completed'
             AND frequency = 'daily'
             AND status != 'archived'";
    
    $stmt4 = $conn->prepare($sql4);
    $stmt4->execute();
    
    $conn->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Daily reset completed',
        'data' => [
            'failed_yesterday' => $failed_count,
            'created_today' => $todo_count,
            'reset_date' => $today
        ]
    ]);
    
} catch(PDOException $e) {
    if (isset($conn)) {
        $conn->rollBack();
    }
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>