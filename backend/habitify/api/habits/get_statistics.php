<?php
// habits/get_statistics.php - Detailed statistics endpoint
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

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
    
    // 1. Today's Statistics
    $todaySql = "SELECT 
                    COUNT(CASE WHEN hl.status = 'completed' THEN 1 END) as today_completed,
                    COUNT(CASE WHEN hl.status = 'failed' THEN 1 END) as today_failed,
                    COUNT(CASE WHEN hl.status = 'todo' THEN 1 END) as today_todo,
                    COUNT(DISTINCT h.id) as total_active_habits
                 FROM habits h
                 LEFT JOIN habit_logs hl ON h.id = hl.habit_id 
                    AND hl.user_id = h.user_id 
                    AND hl.log_date = ?
                 WHERE h.user_id = ?
                 AND h.status != 'archived'";
    
    $todayStmt = $conn->prepare($todaySql);
    $todayStmt->execute([$today, $user_id]);
    $todayStats = $todayStmt->fetch(PDO::FETCH_ASSOC);
    
    // 2. Weekly Statistics (Last 7 days)
    $sevenDaysAgo = date('Y-m-d', strtotime('-7 days'));
    
    $weeklySql = "SELECT 
                    DATE(hl.log_date) as date,
                    COUNT(CASE WHEN hl.status = 'completed' THEN 1 END) as completed,
                    COUNT(CASE WHEN hl.status = 'failed' THEN 1 END) as failed,
                    COUNT(DISTINCT hl.habit_id) as total_habits
                  FROM habit_logs hl
                  JOIN habits h ON hl.habit_id = h.id
                  WHERE h.user_id = ?
                  AND hl.log_date BETWEEN ? AND ?
                  GROUP BY DATE(hl.log_date)
                  ORDER BY hl.log_date DESC";
    
    $weeklyStmt = $conn->prepare($weeklySql);
    $weeklyStmt->execute([$user_id, $sevenDaysAgo, $today]);
    $weeklyStats = $weeklyStmt->fetchAll(PDO::FETCH_ASSOC);
    
    // 3. Streak Statistics
    $streakSql = "SELECT 
                    COUNT(DISTINCT h.id) as total_habits,
                    AVG(h.current_streak) as avg_current_streak,
                    MAX(h.current_streak) as max_current_streak,
                    MAX(h.longest_streak) as max_longest_streak,
                    COUNT(CASE WHEN h.current_streak >= 7 THEN 1 END) as week_plus_streaks,
                    COUNT(CASE WHEN h.current_streak >= 30 THEN 1 END) as month_plus_streaks
                  FROM habits h
                  WHERE h.user_id = ?
                  AND h.status != 'archived'";
    
    $streakStmt = $conn->prepare($streakSql);
    $streakStmt->execute([$user_id]);
    $streakStats = $streakStmt->fetch(PDO::FETCH_ASSOC);
    
    // 4. Habit Completion Rate
    $completionSql = "SELECT 
                        h.title,
                        h.current_streak,
                        h.longest_streak,
                        COUNT(CASE WHEN hl.status = 'completed' THEN 1 END) as total_completed,
                        COUNT(DISTINCT hl.log_date) as total_days_tracked,
                        ROUND((COUNT(CASE WHEN hl.status = 'completed' THEN 1 END) * 100.0 / 
                              GREATEST(COUNT(DISTINCT hl.log_date), 1)), 2) as completion_rate
                      FROM habits h
                      LEFT JOIN habit_logs hl ON h.id = hl.habit_id
                      WHERE h.user_id = ?
                      AND h.status != 'archived'
                      GROUP BY h.id, h.title, h.current_streak, h.longest_streak
                      ORDER BY completion_rate DESC";
    
    $completionStmt = $conn->prepare($completionSql);
    $completionStmt->execute([$user_id]);
    $completionStats = $completionStmt->fetchAll(PDO::FETCH_ASSOC);
    
    // 5. Category-wise Statistics
    $categorySql = "SELECT 
                        h.category,
                        COUNT(DISTINCT h.id) as habit_count,
                        COUNT(CASE WHEN hl.status = 'completed' AND hl.log_date = ? THEN 1 END) as today_completed,
                        AVG(h.current_streak) as avg_streak
                    FROM habits h
                    LEFT JOIN habit_logs hl ON h.id = hl.habit_id
                    WHERE h.user_id = ?
                    AND h.status != 'archived'
                    GROUP BY h.category";
    
    $categoryStmt = $conn->prepare($categorySql);
    $categoryStmt->execute([$today, $user_id]);
    $categoryStats = $categoryStmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'data' => [
            'today_stats' => $todayStats,
            'weekly_stats' => $weeklyStats,
            'streak_stats' => $streakStats,
            'completion_stats' => $completionStats,
            'category_stats' => $categoryStats,
            'summary' => [
                'total_habits' => $streakStats['total_habits'] ?? 0,
                'avg_streak' => round($streakStats['avg_current_streak'] ?? 0, 1),
                'best_streak' => $streakStats['max_longest_streak'] ?? 0,
                'completion_rate' => count($completionStats) > 0 ? 
                    round(array_sum(array_column($completionStats, 'completion_rate')) / count($completionStats), 1) : 0
            ]
        ],
        'timestamp' => date('Y-m-d H:i:s')
    ]);
    
} catch(PDOException $e) {
    error_log("Statistics Error: " . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>