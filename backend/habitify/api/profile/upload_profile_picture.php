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

// Check if file was uploaded
if (!isset($_FILES['profile_picture']) || $_FILES['profile_picture']['error'] !== UPLOAD_ERR_OK) {
    echo json_encode([
        "success" => false,
        "message" => "No file uploaded or upload error",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

// Get user_id from POST
if (!isset($_POST['user_id'])) {
    echo json_encode([
        "success" => false,
        "message" => "User ID is required",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

$user_id = intval($_POST['user_id']);

// Include database connection
require_once '../../config/database.php';
require_once '../../includes/functions.php';

// File upload configuration
$uploadDir = '../../uploads/profile_pictures/';
$allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
$maxFileSize = 5 * 1024 * 1024; // 5MB

// Create upload directory if it doesn't exist
if (!file_exists($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

try {
    $database = new Database();
    $db = $database->getConnection();

    // Check if user exists
    $checkUserQuery = "SELECT id FROM users WHERE id = :user_id AND is_active = 1";
    $checkUserStmt = $db->prepare($checkUserQuery);
    $checkUserStmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
    $checkUserStmt->execute();

    if ($checkUserStmt->rowCount() === 0) {
        echo json_encode([
            "success" => false,
            "message" => "User not found or inactive",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
        exit();
    }

    $file = $_FILES['profile_picture'];
    
    // Validate file
    if (!in_array($file['type'], $allowedTypes)) {
        echo json_encode([
            "success" => false,
            "message" => "Invalid file type. Only JPG, PNG, GIF, and WEBP are allowed",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
        exit();
    }

    if ($file['size'] > $maxFileSize) {
        echo json_encode([
            "success" => false,
            "message" => "File size exceeds 5MB limit",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
        exit();
    }

    // Get current profile picture to delete later
    $getCurrentPictureQuery = "SELECT profile_picture FROM users WHERE id = :user_id";
    $getCurrentPictureStmt = $db->prepare($getCurrentPictureQuery);
    $getCurrentPictureStmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
    $getCurrentPictureStmt->execute();
    $currentPicture = $getCurrentPictureStmt->fetchColumn();

    // Delete old profile picture if exists
    if ($currentPicture && file_exists($uploadDir . basename($currentPicture))) {
        unlink($uploadDir . basename($currentPicture));
    }

    // Generate unique filename
    $fileExtension = pathinfo($file['name'], PATHINFO_EXTENSION);
    $fileName = 'profile_' . $user_id . '_' . time() . '_' . bin2hex(random_bytes(8)) . '.' . $fileExtension;
    $filePath = $uploadDir . $fileName;
    $relativePath = 'uploads/profile_pictures/' . $fileName;

    // Move uploaded file
    if (move_uploaded_file($file['tmp_name'], $filePath)) {
        // Update database with new profile picture path
        $updateQuery = "UPDATE users SET profile_picture = :profile_picture, last_sync = NOW() WHERE id = :user_id";
        $updateStmt = $db->prepare($updateQuery);
        $updateStmt->bindParam(":profile_picture", $relativePath);
        $updateStmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);

        if ($updateStmt->execute()) {
            // Get full URL for the image
            $baseUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://$_SERVER[HTTP_HOST]";
            $projectPath = dirname(dirname(dirname($_SERVER['SCRIPT_NAME'])));
            $fullImageUrl = $baseUrl . $projectPath . '/' . $relativePath;
            
            echo json_encode([
                "success" => true,
                "message" => "Profile picture updated successfully",
                "data" => [
                    "profile_picture" => $relativePath,
                    "profile_picture_url" => $fullImageUrl,
                    "user_id" => $user_id,
                    "timestamp" => date('Y-m-d H:i:s')
                ]
            ]);
        } else {
            // Delete uploaded file if database update failed
            unlink($filePath);
            echo json_encode([
                "success" => false,
                "message" => "Failed to update database",
                "timestamp" => date('Y-m-d H:i:s')
            ]);
        }
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Failed to upload file",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
    }

} catch (PDOException $e) {
    error_log("Database error in upload_profile_picture.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "Database error occurred",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
} catch (Exception $e) {
    error_log("Error in upload_profile_picture.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "An error occurred",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
}
?>