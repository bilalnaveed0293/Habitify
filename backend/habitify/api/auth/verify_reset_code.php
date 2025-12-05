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

if (!$data || !isset($data['email']) || !isset($data['reset_code'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Email and reset code are required"
    ]);
    exit();
}

$email = sanitizeInput($data['email']);
$resetCode = sanitizeInput($data['reset_code']);

if (!validateEmail($email)) {
    echo json_encode([
        "status" => "error", 
        "message" => "Invalid email format"
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
$resetQuery = "SELECT id, expires_at FROM password_resets 
               WHERE user_id = :user_id 
               AND reset_code = :reset_code 
               AND expires_at > NOW()";
$resetStmt = $db->prepare($resetQuery);
$resetStmt->bindParam(":user_id", $userId);
$resetStmt->bindParam(":reset_code", $resetCode);
$resetStmt->execute();

if ($resetStmt->rowCount() > 0) {
    $resetRecord = $resetStmt->fetch(PDO::FETCH_ASSOC);
    
    // Calculate time left
    $expiresAt = new DateTime($resetRecord['expires_at']);
    $now = new DateTime();
    $interval = $now->diff($expiresAt);
    $minutesLeft = $interval->i;
    
    echo json_encode([
        "status" => "success",
        "message" => "Code verified successfully",
        "data" => [
            "valid" => true,
            "expires_in_minutes" => $minutesLeft,
            "code_id" => $resetRecord['id']
        ]
    ]);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid or expired reset code",
        "data" => [
            "valid" => false
        ]
    ]);
}
?>