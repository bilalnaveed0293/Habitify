<?php
// habits/get_habits.php - FIXED SYNTAX
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type');

$host = "localhost";
$dbname = "habitify_db";
$username = "root";
$password = "";

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    $user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
    $today = date('Y-m-d');
    
    if ($user_id <= 0) {
        echo json_encode(['success' => false, 'message' => 'Invalid user ID']);
        exit();
    }
    
    // 1. Get habits - CRITICAL CHANGE: Use habits.status as primary category
    $sql = "SELECT 
                h.*,
                COALESCE(hl.status, 'todo') as today_status,
                hl.completed_at as today_completed_at,
                DATE_FORMAT(h.created_at, '%b %d, %Y') as created_at_formatted,
                h.status as display_category
            FROM habits h
            LEFT JOIN habit_logs hl ON h.id = hl.habit_id 
                AND hl.user_id = h.user_id 
                AND hl.log_date = ?
            WHERE h.user_id = ?
            AND h.status != 'archived'
            ORDER BY 
                CASE h.status
                    WHEN 'todo' THEN 1
                    WHEN 'completed' THEN 2
                    WHEN 'failed' THEN 3
                END,
                h.created_at DESC";
    
    $stmt = $conn->prepare($sql);
    $stmt->execute([$today, $user_id]);
    $habits = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // 2. Get statistics for the last 7 days
    $sevenDaysAgo = date('Y-m-d', strtotime('-7 days'));
    
    $statsSql = "SELECT 
                    DATE(hl.log_date) as log_date,
                    COUNT(CASE WHEN hl.status = 'completed' THEN 1 END) as completed_count,
                    COUNT(CASE WHEN hl.status = 'failed' THEN 1 END) as failed_count,
                    COUNT(CASE WHEN hl.status = 'todo' THEN 1 END) as todo_count
                 FROM habit_logs hl
                 JOIN habits h ON hl.habit_id = h.id
                 WHERE h.user_id = ?
                 AND hl.log_date BETWEEN ? AND ?
                 GROUP BY DATE(hl.log_date)
                 ORDER BY hl.log_date DESC
                 LIMIT 7";
    
    $statsStmt = $conn->prepare($statsSql);
    $statsStmt->execute([$user_id, $sevenDaysAgo, $today]);
    $dailyStats = $statsStmt->fetchAll(PDO::FETCH_ASSOC);
    
    // 3. Get overall statistics - UPDATED to use habits.status
    $overallSql = "SELECT 
                    COUNT(DISTINCT h.id) as total_habits,
                    COUNT(CASE WHEN h.status = 'todo' THEN 1 END) as todo_count,
                    COUNT(CASE WHEN h.status = 'completed' THEN 1 END) as completed_count,
                    COUNT(CASE WHEN h.status = 'failed' THEN 1 END) as failed_count,
                    COALESCE(AVG(h.current_streak), 0) as avg_streak,
                    MAX(h.longest_streak) as best_streak,
                    COUNT(DISTINCT CASE WHEN hl.status = 'completed' THEN hl.log_date END) as active_days
                   FROM habits h
                   LEFT JOIN habit_logs hl ON h.id = hl.habit_id AND hl.user_id = h.user_id
                   WHERE h.user_id = ? AND h.status != 'archived'";
    
    $overallStmt = $conn->prepare($overallSql);
    $overallStmt->execute([$user_id]);
    $overallStats = $overallStmt->fetch(PDO::FETCH_ASSOC);
    
    // 4. Organize habits by category - NOW BASED ON habits.status
    $organized_habits = [
        'todo' => [],
        'completed' => [],
        'failed' => []
    ];
    
    // 5. Use habits.status for category counts
    $category_counts = [
        'todo' => $overallStats['todo_count'] ?? 0,
        'completed' => $overallStats['completed_count'] ?? 0,
        'failed' => $overallStats['failed_count'] ?? 0
    ];
    
    foreach ($habits as $habit) {
        // Use habits.status for category
        $category = $habit['status'];
        
        // Only add to appropriate categories
        if ($category == 'todo' || $category == 'completed' || $category == 'failed') {
            $organized_habits[$category][] = $habit;
        }
    }
    
    echo json_encode([
        'success' => true,
        'data' => [
            'habits' => $organized_habits,
            'statistics' => [
                'category_counts' => $category_counts,
                'overall_stats' => $overallStats,
                'daily_stats' => $dailyStats
            ],
            'today' => $today,
            'user_id' => $user_id
        ]
    ]);
    
} catch(PDOException $e) {
    error_log("Get Habits Error: " . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>