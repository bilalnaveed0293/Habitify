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
if (!$data || !isset($data['user_id'])) {
    echo json_encode([
        "status" => "error",
        "message" => "User ID required"
    ]);
    exit();
}

$user_id = intval($data['user_id']);

// Connect to database
$database = new Database();
$db = $database->getConnection();

// Fetch user profile
$query = "SELECT id, name, email, phone, profile_picture, theme, created_at FROM users WHERE id = :id AND is_active = 1";
$stmt = $db->prepare($query);
$stmt->bindParam(":id", $user_id);
$stmt->execute();

if ($stmt->rowCount() === 0) {
    echo json_encode([
        "status" => "error",
        "message" => "User not found"
    ]);
    exit();
}

$user = $stmt->fetch(PDO::FETCH_ASSOC);

// Create profile picture URL if exists
$profilePictureUrl = null;
if (!empty($user['profile_picture'])) {
    // Construct full URL to the profile picture
    $profilePictureUrl = "http://" . $_SERVER['HTTP_HOST'] . "/habitify/" . $user['profile_picture'];
}

// Return success response
echo json_encode([
    "status" => "success",
    "message" => "Profile fetched successfully",
    "data" => [
        "user" => [
            "id" => $user['id'],
            "name" => $user['name'],
            "email" => $user['email'],
            "phone" => $user['phone'] ?? "",
            "theme" => $user['theme'] ?? "system",
            "profile_picture" => $user['profile_picture'],
            "profile_picture_url" => $profilePictureUrl,
            "created_at" => $user['created_at']
        ]
    ]
]);
?>