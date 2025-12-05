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

// Include database connection and functions
require_once '../../config/database.php';
require_once '../../includes/functions.php';

// Get raw POST data
$data = json_decode(file_get_contents("php://input"), true);

// Check if data is valid
if (!$data || !isset($data['user_id'])) {
    echo json_encode([
        "success" => false,
        "message" => "User ID is required",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    exit();
}

// Get input values
$user_id = intval($data['user_id']);
$name = isset($data['name']) ? sanitizeInput($data['name']) : null;
$email = isset($data['email']) ? sanitizeInput($data['email']) : null;
$phone = isset($data['phone']) ? sanitizeInput($data['phone']) : null;

// Connect to database
try {
    $database = new Database();
    $db = $database->getConnection();

    // Start transaction
    $db->beginTransaction();

    // Check if user exists
    $checkUserQuery = "SELECT id, email FROM users WHERE id = :user_id AND is_active = 1";
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

    $user = $checkUserStmt->fetch(PDO::FETCH_ASSOC);
    $current_email = $user['email'];

    // Build update query dynamically based on what's being updated
    $updateFields = [];
    $params = [':user_id' => $user_id];

    // Validate and add name
    if ($name !== null && $name !== '') {
        if (strlen($name) < 2) {
            echo json_encode([
                "success" => false,
                "message" => "Name must be at least 2 characters",
                "timestamp" => date('Y-m-d H:i:s')
            ]);
            exit();
        }
        $updateFields[] = "name = :name";
        $params[':name'] = $name;
    }

    // Validate and add email
    if ($email !== null && $email !== '') {
        if (!validateEmail($email)) {
            echo json_encode([
                "success" => false,
                "message" => "Invalid email format",
                "timestamp" => date('Y-m-d H:i:s')
            ]);
            exit();
        }
        
        // Check if email is already taken by another user
        if ($email !== $current_email) {
            $checkEmailQuery = "SELECT id FROM users WHERE email = :email AND id != :user_id";
            $checkEmailStmt = $db->prepare($checkEmailQuery);
            $checkEmailStmt->bindParam(":email", $email);
            $checkEmailStmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
            $checkEmailStmt->execute();
            
            if ($checkEmailStmt->rowCount() > 0) {
                echo json_encode([
                    "success" => false,
                    "message" => "Email is already taken by another user",
                    "timestamp" => date('Y-m-d H:i:s')
                ]);
                exit();
            }
        }
        
        $updateFields[] = "email = :email";
        $params[':email'] = $email;
    }

    // Add phone
    if ($phone !== null) {
        // Optional phone validation
        if ($phone !== '' && !preg_match('/^[0-9+\-\s()]{10,20}$/', $phone)) {
            echo json_encode([
                "success" => false,
                "message" => "Invalid phone number format",
                "timestamp" => date('Y-m-d H:i:s')
            ]);
            exit();
        }
        $updateFields[] = "phone = :phone";
        $params[':phone'] = $phone;
    }

    // If nothing to update
    if (empty($updateFields)) {
        echo json_encode([
            "success" => false,
            "message" => "No changes to update",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
        exit();
    }

    // Add last_sync to update fields
    $updateFields[] = "last_sync = NOW()";

    // Build and execute update query
    $updateQuery = "UPDATE users SET " . implode(", ", $updateFields) . " WHERE id = :user_id";
    $updateStmt = $db->prepare($updateQuery);
    
    // Bind all parameters
    foreach ($params as $key => $value) {
        $updateStmt->bindValue($key, $value);
    }

    if ($updateStmt->execute()) {
        // Commit transaction
        $db->commit();
        
        // Get updated user data
            // After successful update in update_profile.php
        $getUpdatedQuery = "SELECT id, name, email, phone, profile_picture FROM users WHERE id = :user_id";
        $getUpdatedStmt = $db->prepare($getUpdatedQuery);
        $getUpdatedStmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
        $getUpdatedStmt->execute();
        $updatedUser = $getUpdatedStmt->fetch(PDO::FETCH_ASSOC);

        // Generate full URL for profile picture
        $baseUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://$_SERVER[HTTP_HOST]";
        $projectPath = dirname(dirname(dirname($_SERVER['SCRIPT_NAME'])));
        $profilePictureUrl = null;
        if ($updatedUser['profile_picture']) {
            $profilePictureUrl = $baseUrl . $projectPath . '/' . $updatedUser['profile_picture'];
        }

        echo json_encode([
            "success" => true,
            "message" => "Profile updated successfully",
            "data" => [
                "user" => [
                    "id" => $updatedUser['id'],
                    "name" => $updatedUser['name'],
                    "email" => $updatedUser['email'],
                    "phone" => $updatedUser['phone'],
                    "profile_picture" => $updatedUser['profile_picture'],
                    "profile_picture_url" => $profilePictureUrl
                ],
                "timestamp" => date('Y-m-d H:i:s')
            ]
        ]);
    } else {
        $db->rollBack();
        echo json_encode([
            "success" => false,
            "message" => "Failed to update profile",
            "timestamp" => date('Y-m-d H:i:s')
        ]);
    }

} catch (PDOException $e) {
    if (isset($db)) {
        $db->rollBack();
    }
    error_log("Database error in update_profile.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "Database error occurred",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
} catch (Exception $e) {
    if (isset($db)) {
        $db->rollBack();
    }
    error_log("Error in update_profile.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "An error occurred",
        "timestamp" => date('Y-m-d H:i:s')
    ]);
}
?>