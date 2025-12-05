<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../../config/database.php';
require_once '../../includes/functions.php';

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Only accept POST requests
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        "status" => "error",
        "message" => "Method not allowed"
    ]);
    exit();
}

// Get raw POST data
$data = json_decode(file_get_contents("php://input"), true);

// Check if data is valid
if (!$data) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid request data"
    ]);
    exit();
}

// Validate required fields
$required_fields = ['name', 'email', 'password'];
foreach ($required_fields as $field) {
    if (!isset($data[$field]) || empty(trim($data[$field]))) {
        echo json_encode([
            "status" => "error",
            "message" => ucfirst($field) . " is required"
        ]);
        exit();
    }
}

$name = sanitizeInput($data['name']);
$email = sanitizeInput($data['email']);
$password = $data['password'];

// Validate email
if (!validateEmail($email)) {
    echo json_encode([
        "status" => "error", 
        "message" => "Invalid email format"
    ]);
    exit();
}

// Validate password strength
if (strlen($password) < 6) {
    echo json_encode([
        "status" => "error",
        "message" => "Password must be at least 6 characters"
    ]);
    exit();
}

// Connect to database
$database = new Database();
$db = $database->getConnection();

// Check if email already exists
$checkQuery = "SELECT id FROM users WHERE email = :email";
$checkStmt = $db->prepare($checkQuery);
$checkStmt->bindParam(":email", $email);
$checkStmt->execute();

if ($checkStmt->rowCount() > 0) {
    echo json_encode([
        "status" => "error",
        "message" => "Email already registered"
    ]);
    exit();
}

// Hash password
$password_hash = password_hash($password, PASSWORD_BCRYPT);

// Insert new user
$query = "INSERT INTO users (name, email, password_hash, created_at) 
          VALUES (:name, :email, :password_hash, NOW())";
$stmt = $db->prepare($query);

$stmt->bindParam(":name", $name);
$stmt->bindParam(":email", $email);
$stmt->bindParam(":password_hash", $password_hash);

if ($stmt->execute()) {
    $user_id = $db->lastInsertId();
    
    // Create default settings for user
    $settingsQuery = "INSERT INTO user_settings (user_id) VALUES (:user_id)";
    $settingsStmt = $db->prepare($settingsQuery);
    $settingsStmt->bindParam(":user_id", $user_id);
    $settingsStmt->execute();
    
    // Return success response
    echo json_encode([
        "status" => "success",
        "message" => "Registration successful",
        "data" => [
            "user_id" => $user_id,
            "name" => $name,
            "email" => $email
        ]
    ]);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "Registration failed. Please try again."
    ]);
}
?>