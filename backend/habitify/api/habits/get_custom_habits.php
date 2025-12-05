<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../../config/database.php';
require_once '../../includes/functions.php';
require_once '../../includes/response.php';

// Create response object
$response = new Response();

// Get database connection
$database = new Database();
$db = $database->getConnection();

try {
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        // Get POST data
        $data = json_decode(file_get_contents("php://input"));
        
        if (empty($data->user_id)) {
            $response->error('User ID is required.');
            $response->send();
            exit;
        }
        
        $user_id = intval($data->user_id);
        
        $query = "SELECT * FROM custom_habits WHERE user_id = :user_id AND is_active = 1 ORDER BY created_at DESC";
        $stmt = $db->prepare($query);
        $stmt->bindParam(':user_id', $user_id);
        
    } else if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        // Optional: Get all custom habits (for admin or browsing)
        $query = "SELECT * FROM custom_habits WHERE is_active = 1 ORDER BY created_at DESC";
        $stmt = $db->prepare($query);
    } else {
        $response->error('Invalid request method.', 405);
        $response->send();
        exit;
    }
    
    $stmt->execute();
    
    $custom_habits = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $custom_habits[] = [
            'id' => $row['id'],
            'user_id' => $row['user_id'],
            'title' => $row['title'],
            'description' => $row['description'],
            'category' => $row['category'],
            'icon_name' => $row['icon_name'],
            'color_code' => $row['color_code'],
            'frequency' => $row['frequency'],
            'reminder_time' => $row['reminder_time'],
            'reminder_enabled' => $row['reminder_enabled'],
            'created_at' => $row['created_at'],
            'is_active' => $row['is_active']
        ];
    }
    
    $response->success('Custom habits fetched successfully.', ['custom_habits' => $custom_habits]);
    
} catch (Exception $e) {
    $response->error('Database error: ' . $e->getMessage());
}

$response->send();
?>