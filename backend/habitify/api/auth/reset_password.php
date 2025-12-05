<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../../config/database.php';
require_once '../../includes/functions.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        "status" => "error",
        "message" => "Method not allowed"
    ]);
    exit();
}

$data = json_decode(file_get_contents("php://input"), true);

if (!$data || !isset($data['email']) || !isset($data['reset_code']) || !isset($data['new_password'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Email, reset code, and new password are required"
    ]);
    exit();
}

$email = sanitizeInput($data['email']);
$resetCode = sanitizeInput($data['reset_code']);
$newPassword = $data['new_password'];

if (!validateEmail($email)) {
    echo json_encode([
        "status" => "error", 
        "message" => "Invalid email format"
    ]);
    exit();
}

if (strlen($newPassword) < 6) {
    echo json_encode([
        "status" => "error",
        "message" => "Password must be at least 6 characters"
    ]);
    exit();
}

$database = new Database();
$db = $database->getConnection();

// Get user ID
$userQuery = "SELECT id FROM users WHERE email = :email AND is_active = 1";
$userStmt = $db->prepare($userQuery);
$userStmt->bindParam(":email", $email);
$userStmt->execute();

if ($userStmt->rowCount() === 0) {
    echo json_encode([
        "status" => "error",
        "message" => "User not found"
    ]);
    exit();
}

$user = $userStmt->fetch(PDO::FETCH_ASSOC);
$userId = $user['id'];

// Verify reset code
$resetQuery = "SELECT id FROM password_resets 
               WHERE user_id = :user_id 
               AND reset_code = :reset_code 
               AND expires_at > NOW()";
$resetStmt = $db->prepare($resetQuery);
$resetStmt->bindParam(":user_id", $userId);
$resetStmt->bindParam(":reset_code", $resetCode);
$resetStmt->execute();

if ($resetStmt->rowCount() === 0) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid or expired reset code"
    ]);
    exit();
}

// Hash new password
$passwordHash = password_hash($newPassword, PASSWORD_BCRYPT);

// Update user password
$updateQuery = "UPDATE users SET password_hash = :password_hash WHERE id = :user_id";
$updateStmt = $db->prepare($updateQuery);
$updateStmt->bindParam(":password_hash", $passwordHash);
$updateStmt->bindParam(":user_id", $userId);

if ($updateStmt->execute()) {
    // Delete used reset code
    $deleteQuery = "DELETE FROM password_resets WHERE user_id = :user_id";
    $deleteStmt = $db->prepare($deleteQuery);
    $deleteStmt->bindParam(":user_id", $userId);
    $deleteStmt->execute();
    
    echo json_encode([
        "status" => "success",
        "message" => "Password reset successful"
    ]);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "Failed to reset password"
    ]);
}
?>