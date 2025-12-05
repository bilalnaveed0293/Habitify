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
if (!$data || !isset($data['email']) || !isset($data['password'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Email and password required"
    ]);
    exit();
}

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

// Connect to database
$database = new Database();
$db = $database->getConnection();

// Check if user exists - UPDATED QUERY TO INCLUDE PROFILE PICTURE
$query = "SELECT id, name, email, password_hash, profile_picture, phone, theme FROM users WHERE email = :email AND is_active = 1";
$stmt = $db->prepare($query);
$stmt->bindParam(":email", $email);
$stmt->execute();

if ($stmt->rowCount() === 0) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid email or password"
    ]);
    exit();
}

$user = $stmt->fetch(PDO::FETCH_ASSOC);

// Verify password
if (!password_verify($password, $user['password_hash'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid email or password"
    ]);
    exit();
}

// Update last login time
$updateQuery = "UPDATE users SET last_sync = NOW() WHERE id = :id";
$updateStmt = $db->prepare($updateQuery);
$updateStmt->bindParam(":id", $user['id']);
$updateStmt->execute();

// Generate a simple token (in production, use JWT)
$token = bin2hex(random_bytes(32));

// Create profile picture URL if exists
$profilePictureUrl = null;
if (!empty($user['profile_picture'])) {
    // Construct full URL to the profile picture
    $profilePictureUrl = "http://" . $_SERVER['HTTP_HOST'] . "/habitify/" . $user['profile_picture'];
}

// Return success response with profile picture - UPDATED RESPONSE
echo json_encode([
    "status" => "success",
    "message" => "Login successful",
    "data" => [
        "user" => [
            "id" => $user['id'],
            "name" => $user['name'],
            "email" => $user['email'],
            "phone" => $user['phone'] ?? "", // Include phone
            "theme" => $user['theme'] ?? "system", // Include theme
            "profile_picture" => $user['profile_picture'], // Include profile picture path
            "profile_picture_url" => $profilePictureUrl // Include full URL
        ],
        "token" => $token
    ]
]);
?>