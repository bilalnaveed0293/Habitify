<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Only accept POST requests
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        "success" => false,
        "message" => "Method not allowed",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

// Get raw POST data
$data = json_decode(file_get_contents("php://input"), true);

// Check if data is valid
if (!$data || !isset($data['user_id']) || !isset($data['current_password']) || 
    !isset($data['new_password']) || !isset($data['confirm_password'])) {
    
    echo json_encode([
        "success" => false,
        "message" => "All fields are required: user_id, current_password, new_password, confirm_password",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

// Get input values
$user_id = intval($data['user_id']);
$current_password = $data['current_password'];
$new_password = $data['new_password'];
$confirm_password = $data['confirm_password'];

// Basic validation
if (empty($user_id) || empty($current_password) || empty($new_password) || empty($confirm_password)) {
    echo json_encode([
        "success" => false,
        "message" => "All fields are required",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

// Check if passwords match
if ($new_password !== $confirm_password) {
    echo json_encode([
        "success" => false,
        "message" => "New password and confirm password do not match",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

// Check if new password is different from current
if ($current_password === $new_password) {
    echo json_encode([
        "success" => false,
        "message" => "New password must be different from current password",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

// Password strength validation
if (strlen($new_password) < 6) {
    echo json_encode([
        "success" => false,
        "message" => "Password must be at least 6 characters",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

// Include database connection
require_once '../../config/database.php';
require_once '../../includes/functions.php';

try {
    // Create database connection
    $database = new Database();
    $db = $database->getConnection();

    // Get current user password from database
    $query = "SELECT id, name, email, password_hash FROM users WHERE id = :user_id AND is_active = 1";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
    $stmt->execute();

    if ($stmt->rowCount() === 0) {
        echo json_encode([
            "success" => false,
            "message" => "User not found or inactive",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
        exit();
    }

    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Verify current password
    if (!password_verify($current_password, $user['password_hash'])) {
        echo json_encode([
            "success" => false,
            "message" => "Current password is incorrect",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
        exit();
    }

    // Hash new password
    $new_hashed_password = password_hash($new_password, PASSWORD_DEFAULT);

    // Update password in database
    $updateQuery = "UPDATE users SET password_hash = :new_password, last_sync = NOW() WHERE id = :user_id";
    $updateStmt = $db->prepare($updateQuery);
    $updateStmt->bindParam(":new_password", $new_hashed_password);
    $updateStmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
    
    if ($updateStmt->execute()) {
        // Create password change logs table if not exists
        $createLogTable = "CREATE TABLE IF NOT EXISTS password_change_logs (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            change_date DATETIME NOT NULL,
            ip_address VARCHAR(45),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            INDEX idx_user_id (user_id),
            INDEX idx_change_date (change_date)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci";
        $db->exec($createLogTable);
        
        // Log password change activity
        $logQuery = "INSERT INTO password_change_logs (user_id, change_date, ip_address) 
                     VALUES (:user_id, NOW(), :ip_address)";
        $logStmt = $db->prepare($logQuery);
        $logStmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
        $logStmt->bindParam(":ip_address", $_SERVER['REMOTE_ADDR']);
        $logStmt->execute();
        
        // Return success response
        echo json_encode([
            "success" => true,
            "message" => "Password changed successfully",
            "data" => [
                "user" => [
                    "id" => $user['id'],
                    "name" => $user['name'],
                    "email" => $user['email']
                ],
                "timestamp" => date('Y-m-d H:i:s')
            ]
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Failed to update password in database",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
    }
    
} catch (PDOException $e) {
    error_log("Database error in change_password.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "Database error occurred. Please try again.",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
} catch (Exception $e) {
    error_log("Error in change_password.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "An error occurred. Please try again.",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
}
?>