<?php
// habits/get_habits.php - Fixed version
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
    
    // Get habits with today's status from habit_logs
    $sql = "SELECT 
                h.*,
                hl.status as today_status,
                hl.completed_at as today_completed_at,
                -- Removed: hl.notes as today_notes (column doesn't exist)
                DATE_FORMAT(h.created_at, '%b %d, %Y') as created_at_formatted,
                CASE 
                    WHEN hl.status = 'completed' THEN 'completed'
                    WHEN hl.status = 'failed' THEN 'failed'
                    ELSE 'todo'
                END as display_category
            FROM habits h
            LEFT JOIN habit_logs hl ON h.id = hl.habit_id 
                AND hl.user_id = h.user_id 
                AND hl.log_date = ?
            WHERE h.user_id = ?
            AND h.status != 'archived'
            ORDER BY h.created_at DESC";
    
    $stmt = $conn->prepare($sql);
    $stmt->execute([$today, $user_id]);
    $habits = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Organize by category for frontend
    $organized_habits = [
        'todo' => [],
        'completed' => [],
        'failed' => []
    ];
    
    $stats = [
        'todo_count' => 0,
        'completed_count' => 0,
        'failed_count' => 0
    ];
    
    foreach ($habits as $habit) {
        $category = $habit['display_category'];
        $organized_habits[$category][] = $habit;
        
        // Update statistics
        switch ($category) {
            case 'todo':
                $stats['todo_count']++;
                break;
            case 'completed':
                $stats['completed_count']++;
                break;
            case 'failed':
                $stats['failed_count']++;
                break;
        }
    }
    
    echo json_encode([
        'success' => true,
        'data' => [
            'habits' => $organized_habits,
            'statistics' => $stats,
            'today' => $today
        ]
    ]);
    
} catch(PDOException $e) {
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>