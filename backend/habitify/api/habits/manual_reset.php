<?php
// habits/manual_reset.php - Fixed version
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$host = "localhost";
$dbname = "habitify_db";
$username = "root";
$password = "";

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Allow setting a specific date for testing
    $reset_date = isset($_GET['date']) ? $_GET['date'] : date('Y-m-d');
    $previous_date = date('Y-m-d', strtotime($reset_date . ' -1 day'));
    
    $conn->beginTransaction();
    
    echo "Starting reset for date: $reset_date<br>";
    echo "Checking habits from: $previous_date<br>";
    
    // 1. Mark previous day's incomplete habits as failed
    $sql1 = "UPDATE habit_logs hl
             JOIN habits h ON hl.habit_id = h.id
             SET hl.status = 'failed',
                 h.current_streak = 0,
                 h.status = 'failed',
                 h.updated_at = NOW()
             WHERE hl.log_date = ?
             AND hl.status = 'todo'
             AND h.frequency = 'daily'";
    
    $stmt1 = $conn->prepare($sql1);
    $stmt1->execute([$previous_date]);
    $failed_count = $stmt1->rowCount();
    echo "Marked $failed_count habits as failed from $previous_date<br>";
    
    // 2. Create todo entries for reset date
    // Remove the is_active check since it doesn't exist
    $sql2 = "INSERT INTO habit_logs (habit_id, user_id, log_date, status, created_at)
             SELECT h.id, h.user_id, ?, 'todo', NOW()
             FROM habits h
             WHERE h.frequency = 'daily'
             AND h.status != 'archived'  -- Use status instead of is_active
             AND NOT EXISTS (
                 SELECT 1 FROM habit_logs hl2 
                 WHERE hl2.habit_id = h.id 
                 AND hl2.log_date = ?
             )";
    
    $stmt2 = $conn->prepare($sql2);
    $stmt2->execute([$reset_date, $reset_date]);
    $todo_count = $stmt2->rowCount();
    echo "Created $todo_count new todo entries for $reset_date<br>";
    
    // 3. Reset completed habits to todo
    $sql3 = "UPDATE habits 
             SET status = 'todo',
                 updated_at = NOW()
             WHERE status = 'completed'
             AND frequency = 'daily'";
    
    $stmt3 = $conn->prepare($sql3);
    $stmt3->execute();
    $reset_count = $stmt3->rowCount();
    echo "Reset $reset_count completed habits to todo<br>";
    
    $conn->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Manual reset completed',
        'data' => [
            'previous_date' => $previous_date,
            'reset_date' => $reset_date,
            'failed_habits' => $failed_count,
            'new_todo_habits' => $todo_count,
            'reset_completed_habits' => $reset_count
        ]
    ]);
    
} catch(PDOException $e) {
    if (isset($conn)) {
        $conn->rollBack();
    }
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>