<?php
// habits/delete.php - Simple standalone version
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Database configuration - EDIT THESE VALUES IF NEEDED
$host = "localhost";
$dbname = "habitify_db";
$username = "root";
$password = "";

try {
    // Connect to database
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get the JSON data from request body
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    
    // Check if JSON is valid
    if (!$data) {
        echo json_encode([
            'success' => false,
            'message' => 'Invalid JSON data'
        ]);
        exit();
    }
    
    // Check required fields
    if (!isset($data['user_id']) || !isset($data['habit_id'])) {
        echo json_encode([
            'success' => false,
            'message' => 'Missing user_id or habit_id'
        ]);
        exit();
    }
    
    $user_id = (int)$data['user_id'];
    $habit_id = (int)$data['habit_id'];
    
    // Start transaction
    $conn->beginTransaction();
    
    // 1. Delete from habit_logs
    $sql1 = "DELETE FROM habit_logs WHERE habit_id = ? AND user_id = ?";
    $stmt1 = $conn->prepare($sql1);
    $stmt1->execute([$habit_id, $user_id]);
    
    // 2. Delete from notifications
    $sql2 = "DELETE FROM notifications WHERE habit_id = ? AND user_id = ?";
    $stmt2 = $conn->prepare($sql2);
    $stmt2->execute([$habit_id, $user_id]);
    
    // 3. Delete from habits
    $sql3 = "DELETE FROM habits WHERE id = ? AND user_id = ?";
    $stmt3 = $conn->prepare($sql3);
    $stmt3->execute([$habit_id, $user_id]);
    
    // Commit transaction
    $conn->commit();
    
    // Return success response
    echo json_encode([
        'success' => true,
        'message' => 'Habit deleted successfully',
        'data' => [
            'habit_id' => $habit_id
        ]
    ]);
    
} catch(PDOException $e) {
    // Rollback if something went wrong
    if (isset($conn)) {
        $conn->rollBack();
    }
    
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
} catch(Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>