<?php
function validateEmail($email) {
    return filter_var($email, FILTER_VALIDATE_EMAIL);
}

function sanitizeInput($data) {
    $data = trim($data);
    $data = stripslashes($data);
    $data = htmlspecialchars($data);
    return $data;
}
function logAction($db, $user_id, $action_type, $description = '') {
    try {
        $query = "INSERT INTO sync_logs (user_id, table_name, record_id, operation, sync_timestamp) 
                  VALUES (:user_id, 'habits', :record_id, 'delete', NOW())";
        $stmt = $db->prepare($query);
        $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
        $stmt->bindParam(':record_id', $user_id, PDO::PARAM_INT);
        $stmt->execute();
        return true;
    } catch (Exception $e) {
        error_log("Failed to log action: " . $e->getMessage());
        return false;
    }
}

// Function to validate JSON input
function getJsonInput() {
    $data = json_decode(file_get_contents("php://input"), true);
    if (json_last_error() !== JSON_ERROR_NONE) {
        return null;
    }
    return $data;
}

// Function to validate required fields
function validateRequiredFields($data, $required_fields) {
    foreach ($required_fields as $field) {
        if (!isset($data[$field]) || empty($data[$field])) {
            return "Missing required field: $field";
        }
    }
    return null;
}

?>