<?php
// habits/get_predefined_habits.php - Ensure categories are included
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$host = "localhost";
$dbname = "habitify_db";
$username = "root";
$password = "";

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    $search = isset($_GET['search']) ? trim($_GET['search']) : '';
    $category = isset($_GET['category']) ? $_GET['category'] : 'all';
    $user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
    
    // Get predefined habits
    $predefined_sql = "SELECT * FROM predefined_habits WHERE is_active = 1";
    $params = [];
    
    if (!empty($search)) {
        $predefined_sql .= " AND (title LIKE ? OR description LIKE ?)";
        $search_term = "%$search%";
        $params[] = $search_term;
        $params[] = $search_term;
    }
    
    if ($category !== 'all' && $category !== 'custom') {
        $predefined_sql .= " AND category = ?";
        $params[] = $category;
    }
    
    $predefined_stmt = $conn->prepare($predefined_sql);
    $predefined_stmt->execute($params);
    $predefined_habits = $predefined_stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Get custom habits for this user
    $custom_habits = [];
    if ($user_id > 0) {
        $custom_sql = "SELECT * FROM custom_habits WHERE user_id = ? AND is_active = 1";
        $custom_params = [$user_id];
        
        if ($category === 'custom') {
            $custom_sql .= " AND category = ?";
            $custom_params[] = 'custom';
        } elseif ($category !== 'all') {
            $custom_sql .= " AND category = ?";
            $custom_params[] = $category;
        }
        
        if (!empty($search)) {
            $custom_sql .= " AND (title LIKE ? OR description LIKE ?)";
            $search_term = "%$search%";
            $custom_params[] = $search_term;
            $custom_params[] = $search_term;
        }
        
        $custom_stmt = $conn->prepare($custom_sql);
        $custom_stmt->execute($custom_params);
        $custom_habits = $custom_stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Combine habits
    $all_habits = [];
    
    foreach ($predefined_habits as $habit) {
        $all_habits[] = [
            'id' => $habit['id'],
            'title' => $habit['title'],
            'description' => $habit['description'],
            'category' => $habit['category'],
            'icon_name' => $habit['icon_name'],
            'color_code' => $habit['color_code'],
            'frequency' => $habit['frequency'],
            'suggested_count' => $habit['suggested_count'],
            'is_custom' => false,
            'custom_habit_id' => null
        ];
    }
    
    foreach ($custom_habits as $habit) {
        $all_habits[] = [
            'id' => $habit['id'] + 10000, // Offset to avoid conflicts
            'title' => $habit['title'],
            'description' => $habit['description'],
            'category' => $habit['category'],
            'icon_name' => $habit['icon_name'],
            'color_code' => $habit['color_code'],
            'frequency' => $habit['frequency'],
            'suggested_count' => 1,
            'is_custom' => true,
            'custom_habit_id' => $habit['id']
        ];
    }
    
    // Get unique categories from both tables
    $category_sql = "SELECT DISTINCT category FROM predefined_habits WHERE is_active = 1 
                     UNION 
                     SELECT DISTINCT category FROM custom_habits WHERE is_active = 1";
    $category_stmt = $conn->prepare($category_sql);
    $category_stmt->execute();
    $db_categories = $category_stmt->fetchAll(PDO::FETCH_COLUMN);
    
    $categories = array_unique(array_merge(['all'], $db_categories));
    
    echo json_encode([
        'success' => true,
        'data' => [
            'habits' => $all_habits,
            'categories' => $categories
        ]
    ]);
    
} catch(PDOException $e) {
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>