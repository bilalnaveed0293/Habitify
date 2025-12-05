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

if (!$data || !isset($data['email'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Email is required"
    ]);
    exit();
}

$email = sanitizeInput($data['email']);

if (!validateEmail($email)) {
    echo json_encode([
        "status" => "error", 
        "message" => "Invalid email format"
    ]);
    exit();
}

$database = new Database();
$db = $database->getConnection();

// Check if user exists
$query = "SELECT id, name, email FROM users WHERE email = :email AND is_active = 1";
$stmt = $db->prepare($query);
$stmt->bindParam(":email", $email);
$stmt->execute();

if ($stmt->rowCount() === 0) {
    echo json_encode([
        "status" => "error",
        "message" => "No account found with this email"
    ]);
    exit();
}

$user = $stmt->fetch(PDO::FETCH_ASSOC);

echo json_encode([
    "status" => "success",
    "message" => "Email verified successfully",
    "data" => [
        "user_id" => $user['id'],
        "name" => $user['name'],
        "email" => $user['email']
    ]
]);
?>