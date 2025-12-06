<?php
// habits/manual_reset.php - SIMPLIFIED VERSION
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$host = "localhost";
$dbname = "habitify_db";
$username = "root";
$password = "";

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    $conn->beginTransaction();
    
    echo "=== MANUAL RESET - SIMPLIFIED ===<br>";
    echo "Time: " . date('Y-m-d H:i:s') . "<br><br>";
    
    // STEP 1: Reset streaks to 0 for ALL habits that have status = 'todo'
    $sql_reset_todo_streaks = "UPDATE habits 
                               SET current_streak = 0,
                                   updated_at = NOW()
                               WHERE status = 'todo'
                               AND status != 'archived'";
    
    $stmt_reset = $conn->prepare($sql_reset_todo_streaks);
    $stmt_reset->execute();
    $streaks_reset = $stmt_reset->rowCount();
    
    echo "1. Reset streaks to 0 for $streaks_reset habits with status='todo'<br>";
    
    // STEP 2: Set ALL habits status to 'todo' (except archived)
    $sql_all_todo = "UPDATE habits 
                     SET status = 'todo',
                         updated_at = NOW()
                     WHERE status != 'archived'";
    
    $stmt_all_todo = $conn->prepare($sql_all_todo);
    $stmt_all_todo->execute();
    $all_todo_count = $stmt_all_todo->rowCount();
    
    echo "2. Set ALL $all_todo_count habits to status='todo'<br>";
    
    // Optional: Also create today's log entries if needed
    $today = date('Y-m-d');
    $sql_create_logs = "INSERT INTO habit_logs (habit_id, user_id, log_date, status, created_at)
                        SELECT h.id, h.user_id, ?, 'todo', NOW()
                        FROM habits h
                        WHERE h.status != 'archived'
                        AND NOT EXISTS (
                            SELECT 1 FROM habit_logs hl2 
                            WHERE hl2.habit_id = h.id 
                            AND hl2.log_date = ?
                        )";
    
    $stmt_logs = $conn->prepare($sql_create_logs);
    $stmt_logs->execute([$today, $today]);
    $logs_created = $stmt_logs->rowCount();
    
    echo "3. Created $logs_created new log entries for today ($today)<br>";
    
    $conn->commit();
    
    // Verification
    $sql_verify = "SELECT 
                    COUNT(*) as total_habits,
                    SUM(CASE WHEN status = 'todo' THEN 1 ELSE 0 END) as todo_count,
                    SUM(CASE WHEN current_streak = 0 THEN 1 ELSE 0 END) as zero_streak_count
                   FROM habits 
                   WHERE status != 'archived'";
    
    $stmt_verify = $conn->prepare($sql_verify);
    $stmt_verify->execute();
    $verification = $stmt_verify->fetch(PDO::FETCH_ASSOC);
    
    echo "<br>=== VERIFICATION ===<br>";
    echo "Total habits: " . $verification['total_habits'] . "<br>";
    echo "Habits with status='todo': " . $verification['todo_count'] . "<br>";
    echo "Habits with streak=0: " . $verification['zero_streak_count'] . "<br>";
    
    echo json_encode([
        'success' => true,
        'message' => 'Manual reset completed successfully!',
        'data' => [
            'streaks_reset_to_zero' => $streaks_reset,
            'all_habits_set_to_todo' => $all_todo_count,
            'new_logs_created' => $logs_created,
            'verification' => $verification
        ]
    ]);
    
} catch(PDOException $e) {
    if (isset($conn)) {
        $conn->rollBack();
    }
    echo json_encode([
        'success' => false, 
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}
?>