<?php
// habits/reset_daily.php - IMPROVED with better reset logic
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
    
    // 1. Mark yesterday's incomplete habits as 'failed' (only if still todo at end of day)
    $sql1 = "UPDATE habit_logs hl
             JOIN habits h ON hl.habit_id = h.id
             SET hl.status = 'failed',
                 h.updated_at = NOW()
             WHERE hl.log_date = ?
             AND hl.status = 'todo'
             AND h.frequency = 'daily'
             AND h.status != 'archived'";
    
    $stmt1 = $conn->prepare($sql1);
    $stmt1->execute([$yesterday]);
    $failed_count = $stmt1->rowCount();
    
    // 2. Reset streak for habits that were failed yesterday
    $sql2 = "UPDATE habits h
             JOIN habit_logs hl ON h.id = hl.habit_id
             SET h.current_streak = 0,
                 h.status = 'todo',
                 h.updated_at = NOW()
             WHERE hl.log_date = ?
             AND hl.status = 'failed'
             AND h.frequency = 'daily'
             AND h.status != 'archived'";
    
    $stmt2 = $conn->prepare($sql2);
    $stmt2->execute([$yesterday]);
    $reset_streak_count = $stmt2->rowCount();
    
    // 3. Reset completed habits to todo for the new day
    $sql3 = "UPDATE habits 
             SET status = 'todo',
                 updated_at = NOW()
             WHERE status = 'completed'
             AND frequency = 'daily'
             AND status != 'archived'";
    
    $stmt3 = $conn->prepare($sql3);
    $stmt3->execute();
    $reset_completed_count = $stmt3->rowCount();
    
    // 4. Create new 'todo' entries for today
    $sql4 = "INSERT INTO habit_logs (habit_id, user_id, log_date, status, created_at, updated_at)
             SELECT h.id, h.user_id, ?, 'todo', NOW(), NOW()
             FROM habits h
             WHERE h.frequency = 'daily'
             AND h.status != 'archived'
             AND NOT EXISTS (
                 SELECT 1 FROM habit_logs hl2 
                 WHERE hl2.habit_id = h.id 
                 AND hl2.log_date = ?
             )";
    
    $stmt4 = $conn->prepare($sql4);
    $stmt4->execute([$today, $today]);
    $new_todo_count = $stmt4->rowCount();
    
    // 5. Preserve streaks for habits completed yesterday
    $sql5 = "UPDATE habits h
             JOIN habit_logs hl ON h.id = hl.habit_id
             SET h.status = 'todo',  -- Reset status but keep streak
                 h.updated_at = NOW()
             WHERE hl.log_date = ?
             AND hl.status = 'completed'
             AND h.frequency = 'daily'
             AND h.status != 'archived'";
    
    $stmt5 = $conn->prepare($sql5);
    $stmt5->execute([$yesterday]);
    $preserved_streak_count = $stmt5->rowCount();
    
    $conn->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Daily reset completed successfully',
        'data' => [
            'yesterday_failed' => $failed_count,
            'streaks_reset' => $reset_streak_count,
            'completed_reset' => $reset_completed_count,
            'new_todos_created' => $new_todo_count,
            'streaks_preserved' => $preserved_streak_count,
            'reset_date' => $today,
            'previous_date' => $yesterday
        ]
    ]);
    
} catch(PDOException $e) {
    if (isset($conn)) {
        $conn->rollBack();
    }
    error_log("Daily Reset Error: " . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>